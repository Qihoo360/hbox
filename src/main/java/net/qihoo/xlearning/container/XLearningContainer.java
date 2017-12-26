package net.qihoo.xlearning.container;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.gson.Gson;
import net.qihoo.xlearning.api.ApplicationContainerProtocol;
import net.qihoo.xlearning.api.XLearningConstants;
import net.qihoo.xlearning.common.InputInfo;
import net.qihoo.xlearning.common.OutputInfo;
import net.qihoo.xlearning.common.XLearningContainerStatus;
import net.qihoo.xlearning.common.TextMultiOutputFormat;
import net.qihoo.xlearning.conf.XLearningConfiguration;
import net.qihoo.xlearning.domain.Cluster;
import net.qihoo.xlearning.domain.ClusterDef;
import net.qihoo.xlearning.domain.TFConfig;
import net.qihoo.xlearning.domain.Task;
import net.qihoo.xlearning.util.Utilities;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IOUtils;
import org.apache.hadoop.ipc.RPC;
import org.apache.hadoop.yarn.api.ApplicationConstants;
import org.apache.hadoop.yarn.util.ConverterUtils;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.*;
import org.apache.hadoop.util.ReflectionUtils;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.*;
import java.text.SimpleDateFormat;

public class XLearningContainer {

  private static final Log LOG = LogFactory.getLog(XLearningContainer.class);

  private XLearningConfiguration conf;

  private ApplicationContainerProtocol amClient;

  private String clusterDef;

  private String inputFileList;

  private XLearningContainerId containerId;

  private Map<String, String> envs;

  private Boolean single;

  private Boolean singleMx;

  private final int downloadRetry;

  private final Socket reservedSocket;

  private String role;

  private final int index;

  private final String xlearningAppType;

  private Heartbeat heartbeatThread;

  private int heartbeatInterval;

  private XLearningContainer() {
    this.conf = new XLearningConfiguration();
    conf.addResource(new Path(XLearningConstants.XLEARNING_JOB_CONFIGURATION));
    LOG.info("user is " + conf.get("hadoop.job.ugi"));
    this.containerId = new XLearningContainerId(ConverterUtils.toContainerId(System
        .getenv(ApplicationConstants.Environment.CONTAINER_ID.name())));
    this.downloadRetry = conf.getInt(XLearningConfiguration.XLEARNING_DOWNLOAD_FILE_RETRY, XLearningConfiguration.DEFAULT_XLEARNING_DOWNLOAD_FILE_RETRY);
    this.envs = System.getenv();
    this.xlearningAppType = envs.get(XLearningConstants.Environment.XLEARNING_APP_TYPE.toString()).toUpperCase();
    this.role = envs.get(XLearningConstants.Environment.XLEARNING_TF_ROLE.toString());
    if ("TENSORFLOW".equals(xlearningAppType)) {
      LOG.info("TensorFlow role is:" + this.role);
    }
    if (xlearningAppType.equals("MXNET")) {
      if (this.role.equals("ps")) {
        this.role = "server";
      }
      LOG.info("MXNet role is:" + this.role);
    }
    this.index = Integer.valueOf(envs.get(XLearningConstants.Environment.XLEARNING_TF_INDEX.toString()));
    if ("TENSORFLOW".equals(xlearningAppType)) {
      LOG.info("TensorFlow index is:" + this.index);
    }
    if (xlearningAppType.equals("MXNET")) {
      LOG.info("MXNet index is:" + this.index);
    }
    this.single = conf.getBoolean(XLearningConfiguration.XLEARNING_TF_MODE_SINGLE, XLearningConfiguration.DEFAULT_XLEARNING_TF_MODE_SINGLE);
    this.singleMx = conf.getBoolean(XLearningConfiguration.XLEARNING_MXNET_MODE_SINGLE, XLearningConfiguration.DEFAULT_XLEARNING_MXNET_MODE_SINGLE);
    heartbeatInterval = this.conf.getInt(XLearningConfiguration.XLEARNING_CONTAINER_HEARTBEAT_INTERVAL, XLearningConfiguration.DEFAULT_XLEARNING_CONTAINER_HEARTBEAT_INTERVAL);
    reservedSocket = new Socket();
  }

  private void init() {
    LOG.info("XLearningContainer initializing");
    String appMasterHost = System.getenv(XLearningConstants.Environment.APPMASTER_HOST.toString());
    int appMasterPort = Integer.valueOf(System.getenv(XLearningConstants.Environment.APPMASTER_PORT.toString()));
    InetSocketAddress addr = new InetSocketAddress(appMasterHost, appMasterPort);
    try {
      this.amClient = RPC.getProxy(ApplicationContainerProtocol.class,
          ApplicationContainerProtocol.versionID, addr, conf);
    } catch (IOException e) {
      LOG.error("Connecting to ApplicationMaster " + appMasterHost + ":" + appMasterPort + " failed!");
      LOG.error("Container will suicide!");
      System.exit(1);
    }

    heartbeatThread = new Heartbeat(amClient, conf, containerId);
    heartbeatThread.setDaemon(true);
    heartbeatThread.start();
    heartbeatThread.setContainerStatus(XLearningContainerStatus.INITIALIZING);

    if ("TENSORFLOW".equals(xlearningAppType) && !single) {
      try {
        reservedSocket.bind(new InetSocketAddress("127.0.0.1", 0));
      } catch (IOException e) {
        LOG.error("Can not get available port");
        reportFailedAndExit();
      }
    }
  }

  public Configuration getConf() {
    return this.conf;
  }

  public ApplicationContainerProtocol getAmClient() {
    return this.amClient;
  }

  private XLearningContainerId getContainerId() {
    return this.containerId;
  }

  private class DownLoadTask implements Runnable {

    private final Path downloadSrc;

    private final String downloadDst;

    DownLoadTask(Path downloadSrc, String downloadDst) throws IOException {
      this.downloadSrc = downloadSrc;
      this.downloadDst = downloadDst;
    }

    @Override
    public void run() {
      LOG.info("Downloading input file from " + this.downloadSrc + " to " + this.downloadDst);
      int retry = 0;
      while (true) {
        InputStream in = null;
        OutputStream out = null;
        try {
          File exist = new File(downloadDst);
          if (exist.exists()) {
            exist.delete();
          }
          FileSystem fs = downloadSrc.getFileSystem(conf);
          in = fs.open(downloadSrc);
          out = new FileOutputStream(downloadDst);
          IOUtils.copyBytes(in, out, conf, true);
          LOG.info("Download input file " + this.downloadSrc + " successful.");
          break;
        } catch (Exception e) {
          if (retry < downloadRetry) {
            LOG.warn("Download input file " + this.downloadSrc + " failed, retry in " + (++retry), e);
          } else {
            LOG.error("Download input file " + this.downloadSrc + " failed after " + downloadRetry + " retry times!", e);
            reportFailedAndExit();
          }
        } finally {
          if (in != null) {
            try {
              in.close();
            } catch (IOException e) {
            }
          }
          if (out != null) {
            try {
              out.close();
            } catch (IOException e) {
            }
          }
        }
      }
    }
  }


  @SuppressWarnings("deprecation")
  private void prepareInputFiles() throws IOException, InterruptedException,
      ExecutionException {
    if (conf.get(XLearningConfiguration.XLEARNING_INPUT_STRATEGY, XLearningConfiguration.DEFAULT_XLEARNING_INPUT_STRATEGY).equals("STREAM")) {
      LOG.info("XLEARNING_INPUT_STRATEGY is STREAM, use the stream way to read data from hdfs.");
    } else if (conf.get(XLearningConfiguration.XLEARNING_INPUT_STRATEGY, XLearningConfiguration.DEFAULT_XLEARNING_INPUT_STRATEGY).equals("PLACEHOLDER")) {
      List<InputInfo> inputs = Arrays.asList(amClient.getInputSplit(containerId));
      if (inputs.size() == 0) {
        LOG.info("Current container has no input.");
        return;
      }
      Map<String, List<String>> phInputInfo = new HashMap<>();
      for (InputInfo inputInfo : inputs) {
        List<String> stringPaths = new ArrayList<>();
        for (Path path : inputInfo.getPaths()) {
          stringPaths.add(path.toString());
        }
        phInputInfo.put(inputInfo.getAliasName(), stringPaths);
      }
      this.inputFileList = new Gson().toJson(phInputInfo);
      LOG.info("Input path is:" + this.inputFileList);
    } else {
      List<InputInfo> inputs = Arrays.asList(amClient.getInputSplit(containerId));
      if (inputs.size() == 0) {
        LOG.info("Current container has no input.");
        return;
      }
      for (InputInfo inputInfo : inputs) {
        LOG.info("Input path: " + inputInfo.getAliasName() + "@" + inputInfo.getPaths().toString());
      }

      ExecutorService executor = Executors.newFixedThreadPool(
          conf.getInt(XLearningConfiguration.XLEARNING_DOWNLOAD_FILE_THREAD_NUMS, XLearningConfiguration.DEFAULT_XLEARNING_DOWNLOAD_FILE_THREAD_NUMS),
          new ThreadFactoryBuilder()
              .setDaemon(true)
              .setNameFormat("Download-File-Thread #%d")
              .build()
      );

      for (InputInfo inputInfo : inputs) {
        String downloadDir = inputInfo.getAliasName();
        Utilities.mkdirs(downloadDir.toString());
        int index = 0;
        for (Path path : inputInfo.getPaths()) {
          String downloadDst;
          if (conf.getBoolean(XLearningConfiguration.XLEARNING_INPUTFILE_RENAME, XLearningConfiguration.DEFAULT_XLEARNING_INPUTFILE_RENAME)) {
            downloadDst = downloadDir + File.separator + System.currentTimeMillis() + "_" + index++;
          } else {
            String[] fileName = StringUtils.split(path.toString(), '/');
            downloadDst = downloadDir + File.separator + fileName[fileName.length - 1];
          }
          DownLoadTask downloadTask = new DownLoadTask(path, downloadDst);
          executor.submit(downloadTask);
        }
      }

      boolean allDownloadTaskFinished = false;
      executor.shutdown();
      do {
        try {
          executor.awaitTermination(Integer.MAX_VALUE, TimeUnit.SECONDS);
          allDownloadTaskFinished = true;
        } catch (InterruptedException e) {
        }
      } while (!allDownloadTaskFinished);
      LOG.info("All input files download finished.");
    }
  }

  private void createLocalOutputDir() {
    if (this.conf.get(XLearningConfiguration.XLEARNING_OUTPUT_STRATEGY, XLearningConfiguration.DEFAULT_XLEARNING_OUTPUT_STRATEGY).equals("STREAM")) {
      LOG.info("XLEARNING_OUTPUT_STRATEGY is STREAM, do not need to create local output dir.");
    } else {
      List<OutputInfo> outputs = Arrays.asList(amClient.getOutputLocation());
      for (OutputInfo outputInfo : outputs) {
        Utilities.mkdirs(outputInfo.getLocalLocation());
        LOG.info("Created output dir " + outputInfo.getLocalLocation());
      }
    }

    if ("TENSORFLOW".equals(xlearningAppType)) {
      int boardIndex = this.conf.getInt(XLearningConfiguration.XLEARNING_TF_BOARD_WORKER_INDEX, XLearningConfiguration.DEFAULT_XLEARNING_TF_BOARD_WORKER_INDEX);
      Boolean boardEnable = this.conf.getBoolean(XLearningConfiguration.XLEARNING_TF_BOARD_ENABLE, XLearningConfiguration.DEFAULT_XLEARNING_TF_BOARD_ENABLE);
      if (boardEnable && this.role.equals(XLearningConstants.WORKER) && boardIndex == this.index) {
        if (this.conf.get(XLearningConfiguration.XLEARNING_TF_BOARD_LOG_DIR, XLearningConfiguration.DEFAULT_XLEARNING_TF_BOARD_LOG_DIR).indexOf("hdfs://") == -1) {
          Utilities.mkdirs(this.conf.get(XLearningConfiguration.XLEARNING_TF_BOARD_LOG_DIR, XLearningConfiguration.DEFAULT_XLEARNING_TF_BOARD_LOG_DIR));
          LOG.info("Created tensorboard log dir " + this.conf.get(XLearningConfiguration.XLEARNING_TF_BOARD_LOG_DIR, XLearningConfiguration.DEFAULT_XLEARNING_TF_BOARD_LOG_DIR));
        } else {
          LOG.info("User appoint the tensorboard log dir : " + this.conf.get(XLearningConfiguration.XLEARNING_TF_BOARD_LOG_DIR));
        }
      }
    }
  }

  @SuppressWarnings("deprecation")
  private void uploadOutputFiles() throws IOException {
    if (this.conf.get(XLearningConfiguration.XLEARNING_OUTPUT_STRATEGY, XLearningConfiguration.DEFAULT_XLEARNING_OUTPUT_STRATEGY).equals("STREAM")) {
      LOG.info("XLEARNING_OUTPUT_STRATEGY is STREAM, do not need to upload local output files.");
    } else {
      List<OutputInfo> outputs = Arrays.asList(amClient.getOutputLocation());
      for (OutputInfo s : outputs) {
        LOG.info("Output path: " + s.getLocalLocation() + "#" + s.getDfsLocation());
      }
      if (outputs.size() > 0) {
        for (OutputInfo outputInfo : outputs) {
          FileSystem localFs = FileSystem.getLocal(conf);
          Path localPath = new Path(outputInfo.getLocalLocation());
          Path remotePath = new Path(outputInfo.getDfsLocation() + "/_temporary/" + containerId.toString());
          FileSystem dfs = remotePath.getFileSystem(conf);
          if (dfs.exists(remotePath)) {
            LOG.info("Container remote output path " + remotePath + "exists, so we has to delete is first.");
            dfs.delete(remotePath);
          }
          if (localFs.exists(localPath)) {
            dfs.copyFromLocalFile(false, false, localPath, remotePath);
            LOG.info("Upload output " + localPath + " to remote path " + remotePath + " finished.");
          }
          localFs.close();
        }
      }
    }

    if ("TENSORFLOW".equals(xlearningAppType)) {
      if (this.conf.get(XLearningConfiguration.XLEARNING_TF_BOARD_LOG_DIR, XLearningConfiguration.DEFAULT_XLEARNING_TF_BOARD_LOG_DIR).indexOf("hdfs://") == -1) {
        int boardIndex = conf.getInt(XLearningConfiguration.XLEARNING_TF_BOARD_WORKER_INDEX, XLearningConfiguration.DEFAULT_XLEARNING_TF_BOARD_WORKER_INDEX);
        Boolean boardEnable = conf.getBoolean(XLearningConfiguration.XLEARNING_TF_BOARD_ENABLE, XLearningConfiguration.DEFAULT_XLEARNING_TF_BOARD_ENABLE);
        String boardLogDir = conf.get(XLearningConfiguration.XLEARNING_TF_BOARD_LOG_DIR, XLearningConfiguration.DEFAULT_XLEARNING_TF_BOARD_LOG_DIR);
        String boardHistoryDir = conf.get(XLearningConfiguration.XLEARNING_TF_BOARD_HISTORY_DIR,
            XLearningConfiguration.DEFAULT_XLEARNING_TF_BOARD_HISTORY_DIR) + "/" + this.envs.get("APP_ID");
        Path localLogPath = new Path(boardLogDir);
        Path remoteLogPath = new Path(boardHistoryDir);
        FileSystem boardLocalFs = FileSystem.getLocal(conf);
        if (boardLocalFs.exists(localLogPath) && boardEnable && boardIndex == this.index) {
          FileSystem boardDfs = remoteLogPath.getFileSystem(conf);
          if (boardDfs.exists(remoteLogPath)) {
            LOG.info("Container remote tensorboard log output path " + remoteLogPath + "exists, so we has to delete is first.");
            boardDfs.delete(remoteLogPath);
          }
          boardDfs.copyFromLocalFile(false, false, localLogPath, remoteLogPath);
          LOG.info("Upload tensorboard  log dir " + localLogPath + " to remote path " + remoteLogPath + " finished.");
        }
        boardLocalFs.close();
      } else {
        LOG.info("User appoint the tensorboard log dir : " + this.conf.get(XLearningConfiguration.XLEARNING_TF_BOARD_LOG_DIR));
      }
    }
  }

  private Boolean run() throws IOException {
    try {
      if (this.role.equals(XLearningConstants.WORKER)) {
        prepareInputFiles();
      }
      if (this.conf.getBoolean(XLearningConfiguration.XLEARNING_CONTAINER_AUTO_CREATE_OUTPUT_DIR, XLearningConfiguration.DEFAULT_XLEARNING_CONTAINER_AUTO_CREATE_OUTPUT_DIR)) {
        createLocalOutputDir();
      }
    } catch (InterruptedException e) {
      LOG.error("Container prepare inputs failed!", e);
      this.reportFailedAndExit();
    } catch (ExecutionException e) {
      LOG.error("Container prepare inputs failed!", e);
      this.reportFailedAndExit();
    }

    if ("TENSORFLOW".equals(xlearningAppType) && !single) {
      LOG.info("Reserved available port: " + reservedSocket.getLocalPort());
      amClient.reportReservedPort(envs.get(ApplicationConstants.Environment.NM_HOST.toString()),
          reservedSocket.getLocalPort(), this.role, this.index);

      while (true) {
        //TODO may be need encode use Base64 while used in Env
        this.clusterDef = amClient.getClusterDef();
        if (this.clusterDef != null) {
          LOG.info("Cluster def is: " + this.clusterDef);
          break;
        }
        Utilities.sleep(this.conf.getInt(XLearningConfiguration.XLEARNING_CONTAINER_UPDATE_APP_STATUS_INTERVAL, XLearningConfiguration.DEFAULT_XLEARNING_CONTAINER_UPDATE_APP_STATUS_INTERVAL));
      }
    }

    String[] env = null;
    if ("TENSORFLOW".equals(xlearningAppType)) {
      if (single) {
        env = new String[]{
            "PATH=" + System.getenv("PATH"),
            "JAVA_HOME=" + System.getenv("JAVA_HOME"),
            "HADOOP_HOME=" + System.getenv("HADOOP_HOME"),
            "HADOOP_HDFS_HOME=" + System.getenv("HADOOP_HDFS_HOME"),
            "LD_LIBRARY_PATH=" + "./:" + System.getenv("LD_LIBRARY_PATH") + ":" + System.getenv("JAVA_HOME") +
                "/jre/lib/amd64/server:" + System.getenv("HADOOP_HOME") + "/lib/native",
            "CLASSPATH=" + "./:" + System.getenv("CLASSPATH") + ":" + System.getProperty("java.class.path"),
            "PYTHONUNBUFFERED=1",
            XLearningConstants.Environment.XLEARNING_TF_INDEX.toString() + "=" + this.index,
            XLearningConstants.Environment.XLEARNING_TF_ROLE.toString() + "=" + this.role,
            XLearningConstants.Environment.XLEARNING_INPUT_FILE_LIST.toString() + "=" + this.inputFileList
        };
      } else {
        /**
         * set TF_CLUSTER_DEF in env
         * python script can load cluster def use "json.loads(os.environ["CLUSTER_DEF"])"
         * set TF_CONFIG in env for new api in distributed tensorflow(Estimator API)
         */
        env = new String[]{
            "PATH=" + System.getenv("PATH"),
            "JAVA_HOME=" + System.getenv("JAVA_HOME"),
            "HADOOP_HOME=" + System.getenv("HADOOP_HOME"),
            "HADOOP_HDFS_HOME=" + System.getenv("HADOOP_HDFS_HOME"),
            "LD_LIBRARY_PATH=" + "./:" + System.getenv("LD_LIBRARY_PATH") + ":" + System.getenv("JAVA_HOME") +
                "/jre/lib/amd64/server:" + System.getenv("HADOOP_HOME") + "/lib/native",
            "CLASSPATH=" + "./:" + System.getenv("CLASSPATH") + ":" + System.getProperty("java.class.path"),
            "PYTHONUNBUFFERED=1",
            XLearningConstants.Environment.XLEARNING_TF_CLUSTER_DEF.toString() + "=" + this.clusterDef,
            XLearningConstants.Environment.XLEARNING_TF_INDEX.toString() + "=" + this.index,
            XLearningConstants.Environment.XLEARNING_TF_ROLE.toString() + "=" + this.role,
            XLearningConstants.Environment.XLEARNING_INPUT_FILE_LIST.toString() + "=" + this.inputFileList,
            "TF_CONFIG=" + getTFConfig()
        };
      }
    } else if (xlearningAppType.equals("MXNET")) {
      if (singleMx) {
        env = new String[]{
            "PATH=" + System.getenv("PATH"),
            "JAVA_HOME=" + System.getenv("JAVA_HOME"),
            "HADOOP_HOME=" + System.getenv("HADOOP_HOME"),
            "HADOOP_HDFS_HOME=" + System.getenv("HADOOP_HDFS_HOME"),
            "LD_LIBRARY_PATH=" + "./:" + System.getenv("LD_LIBRARY_PATH") + ":" + System.getenv("JAVA_HOME") +
                "/jre/lib/amd64/server:" + System.getenv("HADOOP_HOME") + "/lib/native",
            "CLASSPATH=" + "./:" + System.getenv("CLASSPATH") + ":" + System.getProperty("java.class.path"),
            "PYTHONUNBUFFERED=1",
            XLearningConstants.Environment.XLEARNING_INPUT_FILE_LIST.toString() + "=" + this.inputFileList
        };
      } else {
        String dmlcID;
        if (this.role.equals("worker")) {
          dmlcID = "DMLC_WORKER_ID";
        } else {
          dmlcID = "DMLC_SERVER_ID";
        }
        env = new String[]{
            "PATH=" + System.getenv("PATH"),
            "JAVA_HOME=" + System.getenv("JAVA_HOME"),
            "HADOOP_HOME=" + System.getenv("HADOOP_HOME"),
            "HADOOP_HDFS_HOME=" + System.getenv("HADOOP_HDFS_HOME"),
            "LD_LIBRARY_PATH=" + "./:" + System.getenv("LD_LIBRARY_PATH") + ":" + System.getenv("JAVA_HOME") +
                "/jre/lib/amd64/server:" + System.getenv("HADOOP_HOME") + "/lib/native",
            "CLASSPATH=" + "./:" + System.getenv("CLASSPATH") + ":" + System.getProperty("java.class.path"),
            "DMLC_PS_ROOT_URI=" + System.getenv("DMLC_PS_ROOT_URI"),
            "DMLC_PS_ROOT_PORT=" + System.getenv("DMLC_PS_ROOT_PORT"),
            "DMLC_NUM_WORKER=" + System.getenv("DMLC_NUM_WORKER"),
            "DMLC_NUM_SERVER=" + System.getenv("DMLC_NUM_SERVER"),
            "PYTHONUNBUFFERED=1",
            dmlcID + "=" + this.index,
            "DMLC_ROLE=" + this.role,
            XLearningConstants.Environment.XLEARNING_INPUT_FILE_LIST.toString() + "=" + this.inputFileList
        };
      }
    } else {
      env = new String[]{
          "PATH=" + System.getenv("PATH"),
          "JAVA_HOME=" + System.getenv("JAVA_HOME"),
          "HADOOP_HOME=" + System.getenv("HADOOP_HOME"),
          "HADOOP_HDFS_HOME=" + System.getenv("HADOOP_HDFS_HOME"),
          "LD_LIBRARY_PATH=" + "./:" + System.getenv("LD_LIBRARY_PATH") + ":" + System.getenv("JAVA_HOME") +
              "/jre/lib/amd64/server:" + System.getenv("HADOOP_HOME") + "/lib/native",
          "CLASSPATH=" + "./:" + System.getenv("CLASSPATH") + ":" + System.getProperty("java.class.path"),
          "PYTHONUNBUFFERED=1",
          XLearningConstants.Environment.XLEARNING_INPUT_FILE_LIST.toString() + "=" + this.inputFileList
      };
    }

    String command = envs.get(XLearningConstants.Environment.XLEARNING_EXEC_CMD.toString());
    LOG.info("Executing command:" + command);
    Runtime rt = Runtime.getRuntime();

    //close reserved socket as tf will bind this port later
    this.reservedSocket.close();
    final Process xlearningProcess = rt.exec(command, env);
    Date now = new Date();
    heartbeatThread.setContainersStartTime(now.toString());

    if (conf.get(XLearningConfiguration.XLEARNING_INPUT_STRATEGY, XLearningConfiguration.DEFAULT_XLEARNING_INPUT_STRATEGY).equals("STREAM")) {
      LOG.info("Starting thread to redirect stdin of xlearning process");
      Thread stdinRedirectThread = new Thread(new Runnable() {
        @Override
        public void run() {
          try {
            OutputStreamWriter osw = new OutputStreamWriter(xlearningProcess.getOutputStream());
            List<InputSplit> inputs = Arrays.asList(amClient.getStreamInputSplit(containerId));
            JobConf jobConf = new JobConf(conf);
            RecordReader reader;
            InputFormat inputFormat = ReflectionUtils.newInstance(conf.getClass(XLearningConfiguration.XLEARNING_INPUTF0RMAT_CLASS, XLearningConfiguration.DEFAULT_XLEARNING_INPUTF0RMAT_CLASS, InputFormat.class),
                jobConf);
            for (int j = 0; j < conf.getInt(XLearningConfiguration.XLEARNING_STREAM_EPOCH, XLearningConfiguration.DEFAULT_XLEARNING_STREAM_EPOCH); j++) {
              LOG.info("Epoch " + (j + 1) + " starting...");
              for (int i = 0, len = inputs.size(); i < len; i++) {
                LOG.info("split " + (i + 1) + " is handling...");
                reader = inputFormat.getRecordReader(inputs.get(i), jobConf, Reporter.NULL);
                Object key = reader.createKey();
                Object value = reader.createValue();
                Boolean finished = false;
                while (!finished) {
                  try {
                    finished = !reader.next(key, value);
                    if (finished) {
                      break;
                    }
                    osw.write(value.toString());
                    osw.write("\n");
                  } catch (EOFException e) {
                    finished = true;
                  }
                }
                reader.close();
                LOG.info("split " + (i + 1) + " is finished.");
              }
              LOG.info("Epoch " + (j + 1) + " finished.");
            }
            osw.close();
          } catch (Exception e) {
            LOG.warn("Exception in thread stdinRedirectThread");
            e.printStackTrace();
          }
        }
      });
      stdinRedirectThread.start();
    }

    List<OutputInfo> outputs = Arrays.asList(amClient.getOutputLocation());
    if ((this.conf.get(XLearningConfiguration.XLEARNING_OUTPUT_STRATEGY, XLearningConfiguration.DEFAULT_XLEARNING_OUTPUT_STRATEGY).equals("STREAM")) && outputs.size() > 0) {
      LOG.info("Starting thread to redirect stream stdout of xlearning process");
      final Thread stdoutRedirectThread = new Thread(new Runnable() {
        @Override
        public void run() {
          try {
            BufferedReader reader;
            reader = new BufferedReader(new InputStreamReader(xlearningProcess.getInputStream()));
            List<OutputInfo> outputs = Arrays.asList(amClient.getOutputLocation());
            JobConf jobConf = new JobConf(conf);
            jobConf.setOutputKeyClass(Text.class);
            jobConf.setOutputValueClass(Text.class);
            jobConf.setBoolean("mapred.output.compress", true);
            jobConf.set("mapred.output.compression.codec", "org.apache.hadoop.io.compress.GzipCodec");
            jobConf.setOutputFormat(TextMultiOutputFormat.class);

            Path remotePath = new Path(outputs.get(0).getDfsLocation() + "/_temporary/" + containerId.toString());
            FileSystem dfs = remotePath.getFileSystem(jobConf);
            jobConf.set(XLearningConstants.STREAM_OUTPUT_DIR, remotePath.makeQualified(dfs).toString());
            OutputFormat outputFormat = ReflectionUtils.newInstance(conf.getClass(XLearningConfiguration.XLEARNING_OUTPUTFORMAT_CLASS, XLearningConfiguration.DEFAULT_XLEARNING_OUTPUTF0RMAT_CLASS, OutputFormat.class),
                jobConf);
            outputFormat.checkOutputSpecs(dfs, jobConf);
            JobID jobID = new JobID(new SimpleDateFormat("yyyyMMddHHmm").format(new Date()), 0);
            TaskAttemptID taId = new TaskAttemptID(new TaskID(jobID, true, 0), 0);
            jobConf.set("mapred.tip.id", taId.getTaskID().toString());
            jobConf.set("mapred.task.id", taId.toString());
            jobConf.set("mapred.job.id", jobID.toString());
            amClient.reportMapedTaskID(containerId, taId.toString());
            RecordWriter writer = outputFormat.getRecordWriter(dfs, jobConf, "part-r", Reporter.NULL);
            String xlearningStreamResultLine;
            while ((xlearningStreamResultLine = reader.readLine()) != null) {
              writer.write(null, xlearningStreamResultLine);
            }
            writer.close(Reporter.NULL);
            reader.close();
            dfs.close();
          } catch (Exception e) {
            LOG.warn("Exception in thread stdoutRedirectThread");
            e.printStackTrace();
          }
        }
      });
      stdoutRedirectThread.start();
    } else {
      LOG.info("Starting thread to redirect stdout of xlearning process");
      Thread stdoutRedirectThread = new Thread(new Runnable() {
        @Override
        public void run() {
          try {
            BufferedReader reader;
            reader = new BufferedReader(new InputStreamReader(xlearningProcess.getInputStream()));
            String xlearningStdoutLog;
            while ((xlearningStdoutLog = reader.readLine()) != null) {
              LOG.info(xlearningStdoutLog);
            }
          } catch (Exception e) {
            LOG.warn("Exception in thread stdoutRedirectThread");
            e.printStackTrace();
          }
        }
      });
      stdoutRedirectThread.start();
    }

    LOG.info("Starting thread to redirect stderr of xlearning process");
    Thread stderrRedirectThread = new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          BufferedReader reader;
          reader = new BufferedReader(new InputStreamReader(xlearningProcess.getErrorStream()));
          String xlearningStderrLog;
          while ((xlearningStderrLog = reader.readLine()) != null) {
            if (xlearningStderrLog.contains("reporter progress")) {
              heartbeatThread.setProgressLog(xlearningStderrLog);
            } else {
              LOG.info(xlearningStderrLog);
            }
          }
        } catch (Exception e) {
          LOG.warn("Error in thread stderrRedirectThread");
          e.printStackTrace();
        }
      }
    });
    stderrRedirectThread.start();

    heartbeatThread.setContainerStatus(XLearningContainerStatus.RUNNING);

    //Start tensorboard process
    if ("TENSORFLOW".equals(xlearningAppType)) {
      int boardIndex = this.conf.getInt(XLearningConfiguration.XLEARNING_TF_BOARD_WORKER_INDEX, XLearningConfiguration.DEFAULT_XLEARNING_TF_BOARD_WORKER_INDEX);
      Boolean boardEnable = this.conf.getBoolean(XLearningConfiguration.XLEARNING_TF_BOARD_ENABLE, XLearningConfiguration.DEFAULT_XLEARNING_TF_BOARD_ENABLE);
      if (boardEnable && this.role.equals(XLearningConstants.WORKER) && boardIndex == this.index) {
        Socket boardReservedSocket = new Socket();
        try {
          boardReservedSocket.bind(new InetSocketAddress("127.0.0.1", 0));
        } catch (IOException e) {
          LOG.error("Can not get available port");
          reportFailedAndExit();
        }
        String boardHost = envs.get(ApplicationConstants.Environment.NM_HOST.toString());
        int boardReloadInterval = this.conf.getInt(XLearningConfiguration.XLEARNING_TF_BOARD_RELOAD_INTERVAL, XLearningConfiguration.DEFAULT_XLEARNING_TF_BOARD_RELOAD_INTERVAL);
        String boardLogDir = this.conf.get(XLearningConfiguration.XLEARNING_TF_BOARD_LOG_DIR, XLearningConfiguration.DEFAULT_XLEARNING_TF_BOARD_LOG_DIR);
        int boardPort = boardReservedSocket.getLocalPort();
        String boardCommand = "tensorboard --host=" + boardHost + " --port=" + boardPort + " --reload_interval=" + boardReloadInterval + " --logdir=" + boardLogDir;
        String boardUrl = "http://" + boardHost + ":" + boardPort;
        LOG.info("Executing tensorborad command:" + boardCommand);
        boardReservedSocket.close();
        final Process boardProcess = rt.exec(boardCommand, env);
        LOG.info("Starting thread to redirect stdout of tensorboard process");
        Thread boardStdoutRedirectThread = new Thread(new Runnable() {
          @Override
          public void run() {
            try {
              BufferedReader reader;
              reader = new BufferedReader(new InputStreamReader(boardProcess.getInputStream()));
              String boardStdoutLog;
              while ((boardStdoutLog = reader.readLine()) != null) {
                LOG.debug(boardStdoutLog);
              }
            } catch (Exception e) {
              LOG.warn("Exception in thread boardStdoutRedirectThread");
              e.printStackTrace();
            }
          }
        });
        boardStdoutRedirectThread.start();

        LOG.info("Starting thread to redirect stderr of tensorboard process");
        Thread boardStderrRedirectThread = new Thread(new Runnable() {
          @Override
          public void run() {
            try {
              BufferedReader reader;
              reader = new BufferedReader(new InputStreamReader(boardProcess.getErrorStream()));
              String boardStderrLog;
              while ((boardStderrLog = reader.readLine()) != null) {
                LOG.debug(boardStderrLog);
              }
            } catch (Exception e) {
              LOG.warn("Error in thread boardStderrRedirectThread");
              e.printStackTrace();
            }
          }
        });
        boardStderrRedirectThread.start();
        amClient.reportTensorBoardURL(boardUrl);
        LOG.info("Container index is " + index + ", report tensorboard url:" + boardUrl);
      }
    }

    int updateAppStatusInterval = this.conf.getInt(XLearningConfiguration.XLEARNING_CONTAINER_UPDATE_APP_STATUS_INTERVAL, XLearningConfiguration.DEFAULT_XLEARNING_CONTAINER_UPDATE_APP_STATUS_INTERVAL);

    int code = -1;
    while (code == -1 && !heartbeatThread.isXLearningTrainCompleted()) {
      Utilities.sleep(updateAppStatusInterval);
      try {
        code = xlearningProcess.exitValue();
      } catch (IllegalThreadStateException e) {
        LOG.debug("XLearning Process is running");
      }
    }

    if (this.role.equals(XLearningConstants.PS)) {
      if (code == -1) {
        xlearningProcess.destroy();
        return true;
      } else if (code == 0) {
        return true;
      }
      return false;
    }

    if (this.role.equals("server")) {
      if (code == -1) {
        xlearningProcess.destroy();
        return true;
      } else if (code == 0) {
        return true;
      }
      return false;
    }
    //As role is worker
    if (code == 0) {
      this.uploadOutputFiles();
    } else {
      return false;
    }
    return true;
  }

  private void reportFailedAndExit() {
    Date now = new Date();
    heartbeatThread.setContainersFinishTime(now.toString());
    heartbeatThread.setContainerStatus(XLearningContainerStatus.FAILED);
    Utilities.sleep(heartbeatInterval);
    System.exit(-1);
  }

  private void reportSucceededAndExit() {
    Date now = new Date();
    heartbeatThread.setContainersFinishTime(now.toString());
    heartbeatThread.setContainerStatus(XLearningContainerStatus.SUCCEEDED);
    Utilities.sleep(heartbeatInterval);
    System.exit(-1);
  }

  /**
   TF_CONFIG='{
   "cluster": {
   "chief": ["host0:2222"],
   "worker": ["host1:2222", "host2:2222", "host3:2222"],
   "ps": ["host4:2222", "host5:2222"]
   },
   "task": {"type": "ps", "index": 0}
   }'
   */
  // get env value: TF_CONFIG
  private String getTFConfig()
  {
    // get old ClusterDef
    ClusterDef clusterDefOld = new Gson().fromJson(this.clusterDef, ClusterDef.class);
    List<String> psListOld = clusterDefOld.getPs();
    List<String> workerListOld = clusterDefOld.getWorker();
    int workerLen = workerListOld.size();

    // create TFConfig Json Object
    TFConfig tfConfig = new TFConfig();
    Cluster cluster = new Cluster();
    Task task = new Task();
    List<String> chiefListNew = new ArrayList<>();
    List<String> workerListNew = new ArrayList<>();
    chiefListNew.add(workerListOld.get(0));
    if (workerLen == 1)
    {
      workerListNew.add(null);
    }
    else if (workerLen > 1){
      for (int i = 1; i < workerLen; i++)
      {
        workerListNew.add(workerListOld.get(i));
      }
    }
    else {
      LOG.error("number of workers is error!");
    }
    // set cluster in TFConfig
    cluster.setChief(chiefListNew);
    cluster.setWorker(workerListNew);
    cluster.setPs(psListOld);
    tfConfig.setCluster(cluster);

    // set task in TFConfig
    if (this.role.equals(XLearningConstants.PS)){
      task.setType("ps");
      task.setIndex(this.index);
    }
    else if(this.role.equals(XLearningConstants.WORKER)){
      if (this.index == 0){
        task.setType("chief");
        task.setIndex(this.index);
      }
      else if (this.index > 0)
      {
        task.setType("worker");
        task.setIndex(this.index - 1);
      }
    }
    else {
      LOG.error("Role " + this.role + "Not Found!");
    }
    tfConfig.setTask(task);

    String tfConfigStr = new Gson().toJson(tfConfig);
    LOG.info("TF_CONF: " + tfConfigStr);
    return tfConfigStr;
  }

  public static void main(String[] args) {
    XLearningContainer container = new XLearningContainer();
    try {
      container.init();
      if (container.run()) {
        LOG.info("XLearningContainer " + container.getContainerId().toString() + " finish successfully");
        container.reportSucceededAndExit();
      } else {
        LOG.error("XLearningContainer run failed!");
        container.reportFailedAndExit();
      }
    } catch (Exception e) {
      LOG.error("Some errors has occurred during container running!", e);
      container.reportFailedAndExit();
    }
  }
}
