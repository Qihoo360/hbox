package net.qihoo.hbox.container;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.qihoo.hbox.api.ApplicationContainerProtocol;
import net.qihoo.hbox.api.HboxConstants;
import net.qihoo.hbox.common.InputInfo;
import net.qihoo.hbox.common.OutputInfo;
import net.qihoo.hbox.common.HboxContainerStatus;
import net.qihoo.hbox.common.UploadTask;
import net.qihoo.hbox.conf.HboxConfiguration;
import net.qihoo.hbox.util.Utilities;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.LocatedFileStatus;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IOUtils;
import org.apache.hadoop.ipc.RPC;
import org.apache.hadoop.yarn.api.ApplicationConstants;
import org.apache.hadoop.yarn.util.ConverterUtils;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.*;
import org.apache.hadoop.mapred.lib.TextMultiOutputFormat;
import org.apache.hadoop.util.ReflectionUtils;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;
import java.text.SimpleDateFormat;
import java.util.zip.GZIPOutputStream;

public class HboxContainer {

  private static final Log LOG = LogFactory.getLog(HboxContainer.class);

  private HboxConfiguration conf;

  private ApplicationContainerProtocol amClient;

  private String clusterDef;

  private String tfConfig;

  private String localAddress;

  private String inputFileList;

  private HboxContainerId containerId;

  private Map<String, String> envs;

  private Boolean single;

  private Boolean singleMx;

  private int downloadRetry;

  private Socket reservedSocket;

  private String role;

  private int index;

  private String hboxAppType;

  private Heartbeat heartbeatThread;

  private ContainerReporter containerReporter;

  private int heartbeatInterval;

  private String hboxCmdProcessId;

  private int lightGBMLocalPort;

  private int lightLDALocalPort;

  private int torchRank0Port;

  private String torchRank0IP;

  private String lightLDAEndpoint;

  private String mpiAppDir;

  private int signalID;

  private int reservePortBegin = 0;

  private int reservePortEnd = 0;

  private String localHost;

  private HboxContainer() {
    this.conf = new HboxConfiguration();
    conf.addResource(new Path(HboxConstants.HBOX_JOB_CONFIGURATION));
    LOG.info("user is " + conf.get("hadoop.job.ugi"));
    this.containerId = new HboxContainerId(ConverterUtils.toContainerId(System
        .getenv(ApplicationConstants.Environment.CONTAINER_ID.name())));
    this.downloadRetry = conf.getInt(HboxConfiguration.HBOX_DOWNLOAD_FILE_RETRY, HboxConfiguration.DEFAULT_HBOX_DOWNLOAD_FILE_RETRY);
    this.envs = System.getenv();

    if (envs.containsKey(ApplicationConstants.Environment.NM_HOST.toString())) {
      localHost = envs.get(ApplicationConstants.Environment.NM_HOST.toString());
    } else {
      localHost = "127.0.0.1";
    }
    reservePortBegin = this.conf.getInt(HboxConfiguration.HBOX_RESERVE_PORT_BEGIN, HboxConfiguration.DEFAULT_HBOX_RESERVE_PORT_BEGIN);
    reservePortEnd = this.conf.getInt(HboxConfiguration.HBOX_RESERVE_PORT_END, HboxConfiguration.DEFAULT_HBOX_RESERVE_PORT_END);

    this.hboxAppType = envs.get(HboxConstants.Environment.HBOX_APP_TYPE.toString()).toUpperCase();
    this.role = envs.get(HboxConstants.Environment.HBOX_TF_ROLE.toString());
    this.index = Integer.valueOf(envs.get(HboxConstants.Environment.HBOX_TF_INDEX.toString()));
    this.hboxCmdProcessId = "";
    if ("TENSORFLOW".equals(hboxAppType)) {
      LOG.info("TensorFlow role is:" + this.role);
    }
    if (hboxAppType.equals("MXNET")) {
      if (this.role.equals("ps")) {
        this.role = "server";
      }
      LOG.info("mxnet role is:" + this.role);
    }
    if (hboxAppType.equals("DISTXGBOOST")) {
      LOG.info("Dist Xgboost role is:" + this.role);
    }
    if (hboxAppType.equals("DISTLIGHTGBM")) {
      LOG.info("Dist lightGBM role is:" + this.role);
    }
    if (hboxAppType.equals("DISTLIGHTLDA")) {
      LOG.info("Dist lightLDA role is:" + this.role);
    }
    if (hboxAppType.equals("XFLOW")) {
      if (this.role.equals("ps")) {
        this.role = "server";
      }
      LOG.info("xflow role is:" + this.role);
    }

    if ("TENSORFLOW".equals(hboxAppType)) {
      LOG.info("TensorFlow index is:" + this.index);
    }
    if (hboxAppType.equals("MXNET")) {
      LOG.info("mxnet index is:" + this.index);
    }
    if (hboxAppType.equals("DISTXGBOOST")) {
      LOG.info("Dist Xgboost index is:" + this.index);
    }
    if (hboxAppType.equals("DISTLIGHTGBM")) {
      LOG.info("Dist lightGBM index is:" + this.index);
    }
    if (hboxAppType.equals("DISTLIGHTLDA")) {
      LOG.info("Dist lightLDA index is:" + this.index);
    }
    if (hboxAppType.equals("XFLOW")) {
      LOG.info("xflow index is:" + this.index);
    }
    if (hboxAppType.equals("MPI") || hboxAppType.equals("HOROVOD")) {
      if (this.envs.containsKey(HboxConstants.Environment.MPI_EXEC_DIR.toString())) {
        this.mpiAppDir = envs.get(HboxConstants.Environment.MPI_EXEC_DIR.toString());
      } else {
        this.mpiAppDir = envs.get(ApplicationConstants.Environment.PWD.name());
      }
      LOG.info(hboxAppType.toLowerCase() + " app dir is:" + this.mpiAppDir);
      LOG.info(hboxAppType.toLowerCase() + " container index is: " + this.index);
    }
    this.single = conf.getBoolean(HboxConfiguration.HBOX_TF_MODE_SINGLE, HboxConfiguration.DEFAULT_HBOX_TF_MODE_SINGLE);
    this.singleMx = conf.getBoolean(HboxConfiguration.HBOX_MXNET_MODE_SINGLE, HboxConfiguration.DEFAULT_HBOX_MXNET_MODE_SINGLE);
    heartbeatInterval = this.conf.getInt(HboxConfiguration.HBOX_CONTAINER_HEARTBEAT_INTERVAL, HboxConfiguration.DEFAULT_HBOX_CONTAINER_HEARTBEAT_INTERVAL);
    reservedSocket = new Socket();

    this.signalID = -1;
  }

  private void init() {
    LOG.info("HboxContainer initializing");
    String appMasterHost = System.getenv(HboxConstants.Environment.APPMASTER_HOST.toString());
    int appMasterPort = Integer.valueOf(System.getenv(HboxConstants.Environment.APPMASTER_PORT.toString()));
    InetSocketAddress addr = new InetSocketAddress(appMasterHost, appMasterPort);
    try {
      this.amClient = RPC.getProxy(ApplicationContainerProtocol.class,
          ApplicationContainerProtocol.versionID, addr, conf);
    } catch (IOException e) {
      LOG.error("Connecting to ApplicationMaster " + appMasterHost + ":" + appMasterPort + " failed!");
      LOG.error("Container will suicide!");
      System.exit(1);
    }
    //this.amClient.reportStatus(containerId, HboxContainerStatus.INITIALIZING);
    heartbeatThread = new Heartbeat(amClient, conf, containerId);
    heartbeatThread.setDaemon(true);
    heartbeatThread.start();
    heartbeatThread.setContainerStatus(HboxContainerStatus.INITIALIZING);
    containerReporter = null;

    if (("TENSORFLOW".equals(hboxAppType) && !single) || hboxAppType.equals("DIGITS") || hboxAppType.equals("DISTLIGHTGBM") || hboxAppType.equals("DISTLIGHTLDA") || (hboxAppType.equals("DISTTORCH") && this.index == 0)) {
      try {
        Utilities.getReservePort(reservedSocket, InetAddress.getByName(localHost).getHostAddress(), reservePortBegin, reservePortEnd);
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

  private HboxContainerId getContainerId() {
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
    if (this.conf.getBoolean(HboxConfiguration.HBOX_INPUT_STREAM, HboxConfiguration.DEFAULT_HBOX_INPUT_STREAM) ||
        conf.get(HboxConfiguration.HBOX_INPUT_STRATEGY, HboxConfiguration.DEFAULT_HBOX_INPUT_STRATEGY).equals("STREAM")) {
      LOG.info("HBOX_INPUT_STRATEGY is STREAM, use the stream way to read data from hdfs.");
    } else if (conf.get(HboxConfiguration.HBOX_INPUT_STRATEGY, HboxConfiguration.DEFAULT_HBOX_INPUT_STRATEGY).equals("PLACEHOLDER")) {
      if(conf.getBoolean(HboxConfiguration.HBOX_PLACEHOLDER_WHOLE_ENABLE, HboxConfiguration.DEFAULT_HBOX_PLACEHOLDER_WHOLE_ENABLE)) {
        InputInfo[] wholeFiles = amClient.getInputWholeSplit();
        if (wholeFiles.length == 0) {
          LOG.info("Current container has no input.");
          return;
        }
        Map<String, List<String>> phInputInfo = new HashMap<>();
        for(InputInfo inputInfo: wholeFiles) {
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
      }
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
          conf.getInt(HboxConfiguration.HBOX_DOWNLOAD_FILE_THREAD_NUMS, HboxConfiguration.DEFAULT_HBOX_DOWNLOAD_FILE_THREAD_NUMS),
          new ThreadFactoryBuilder()
              .setDaemon(true)
              .setNameFormat("Download-File-Thread #%d")
              .build()
      );

      for (InputInfo inputInfo : inputs) {
        String downloadDir;
        if (hboxAppType.equals("MPI") || hboxAppType.equals("HOROVOD")) {
          downloadDir = this.mpiAppDir + File.separator + inputInfo.getAliasName();
          Utilities.mkdirs(downloadDir, true);
        } else {
          downloadDir = inputInfo.getAliasName();
          Utilities.mkdirs(downloadDir);
        }
        int index = 0;
        for (Path path : inputInfo.getPaths()) {
          String downloadDst;
          if (conf.getBoolean(HboxConfiguration.HBOX_INPUTFILE_RENAME, HboxConfiguration.DEFAULT_HBOX_INPUTFILE_RENAME)) {
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

  private void createLocalInputDir() {
    if (this.envs.containsKey(HboxConstants.Environment.HBOX_INPUT_PATH.toString())) {
      String[] inputPath = this.envs.get(HboxConstants.Environment.HBOX_INPUT_PATH.toString()).split(",");
      for (String path : inputPath) {
        Utilities.mkdirs(path);
      }
    }
  }

  private void createLocalOutputDir() {
    if (this.conf.getBoolean(HboxConfiguration.HBOX_OUTPUT_STREAM, HboxConfiguration.DEFAULT_HBOX_OUTPUT_STREAM)
        || this.conf.get(HboxConfiguration.HBOX_OUTPUT_STRATEGY, HboxConfiguration.DEFAULT_HBOX_OUTPUT_STRATEGY).equals("STREAM")) {
      LOG.info("HBOX_OUTPUT_STRATEGY is STREAM, do not need to create local output dir.");
    } else {
      List<OutputInfo> outputs = Arrays.asList(amClient.getOutputLocation());
      for (OutputInfo outputInfo : outputs) {
        if (hboxAppType.equals("MPI") || hboxAppType.equals("HOROVOD")) {
          Utilities.mkdirs(outputInfo.getLocalLocation(), true);
        } else {
          Utilities.mkdirs(outputInfo.getLocalLocation());
        }
        LOG.info("Created output dir " + outputInfo.getLocalLocation());
      }
    }

    if (!(hboxAppType.equals("VPC") || hboxAppType.equals("DIGITS"))) {
      int boardIndex = this.conf.getInt(HboxConfiguration.HBOX_TF_BOARD_WORKER_INDEX, HboxConfiguration.DEFAULT_HBOX_TF_BOARD_WORKER_INDEX);
      Boolean boardEnable = this.conf.getBoolean(HboxConfiguration.HBOX_TF_BOARD_ENABLE, HboxConfiguration.DEFAULT_HBOX_TF_BOARD_ENABLE);
      Boolean boardPsEnable = this.conf.getBoolean(HboxConfiguration.HBOX_TF_BOARD_PS_ENABLE, HboxConfiguration.DEFAULT_HBOX_TF_BOARD_PS_ENABLE);
      if(boardEnable) {
        if(boardPsEnable) {
          if (this.role.equals(HboxConstants.PS) && boardIndex == this.index) {
            if (this.conf.get(HboxConfiguration.HBOX_TF_BOARD_LOG_DIR, HboxConfiguration.DEFAULT_HBOX_TF_BOARD_LOG_DIR).indexOf("hdfs://") == -1) {
              Utilities.mkdirs(this.conf.get(HboxConfiguration.HBOX_TF_BOARD_LOG_DIR, HboxConfiguration.DEFAULT_HBOX_TF_BOARD_LOG_DIR));
              LOG.info("Created board log dir " + this.conf.get(HboxConfiguration.HBOX_TF_BOARD_LOG_DIR, HboxConfiguration.DEFAULT_HBOX_TF_BOARD_LOG_DIR));
            } else {
              LOG.info("User appoint the board log dir : " + this.conf.get(HboxConfiguration.HBOX_TF_BOARD_LOG_DIR));
            }
          }
        } else {
          if (this.role.equals(HboxConstants.WORKER) && boardIndex == this.index) {
            if (this.conf.get(HboxConfiguration.HBOX_TF_BOARD_LOG_DIR, HboxConfiguration.DEFAULT_HBOX_TF_BOARD_LOG_DIR).indexOf("hdfs://") == -1) {
              Utilities.mkdirs(this.conf.get(HboxConfiguration.HBOX_TF_BOARD_LOG_DIR, HboxConfiguration.DEFAULT_HBOX_TF_BOARD_LOG_DIR));
              LOG.info("Created board log dir " + this.conf.get(HboxConfiguration.HBOX_TF_BOARD_LOG_DIR, HboxConfiguration.DEFAULT_HBOX_TF_BOARD_LOG_DIR));
            } else {
              LOG.info("User appoint the board log dir : " + this.conf.get(HboxConfiguration.HBOX_TF_BOARD_LOG_DIR));
            }
          }
        }
      }
    }
  }

  @SuppressWarnings("deprecation")
  private void uploadOutputFiles() throws IOException {
    Boolean boardUpload = this.conf.getBoolean(HboxConfiguration.HBOX_TF_BOARD_UPLOAD, true);
    if (this.conf.getBoolean(HboxConfiguration.HBOX_OUTPUT_STREAM, HboxConfiguration.DEFAULT_HBOX_OUTPUT_STREAM)
        || this.conf.get(HboxConfiguration.HBOX_OUTPUT_STRATEGY, HboxConfiguration.DEFAULT_HBOX_OUTPUT_STRATEGY).equals("STREAM")) {
      LOG.info("HBOX_OUTPUT_STRATEGY is STREAM, do not need to upload local output files.");
    } else {
      List<OutputInfo> outputs = Arrays.asList(amClient.getOutputLocation());
      for (OutputInfo s : outputs) {
        LOG.info("Output path: " + s.getLocalLocation() + "#" + s.getDfsLocation());
      }
      if (outputs.size() > 0) {
        int workerNum = conf.getInt(HboxConfiguration.HBOX_WORKER_NUM, HboxConfiguration.DEFAULT_HBOX_WORKER_NUM);
        ExecutorService executor = Executors.newFixedThreadPool(
            conf.getInt(HboxConfiguration.HBOX_UPLOAD_OUTPUT_THREAD_NUMS, HboxConfiguration.DEFAULT_HBOX_DOWNLOAD_FILE_THREAD_NUMS),
            new ThreadFactoryBuilder()
                .setDaemon(true)
                .setNameFormat("Upload-Output-Thread #%d")
                .build()
        );
        for (OutputInfo outputInfo : outputs) {
          FileSystem localFs = FileSystem.getLocal(conf);
          Path localPath = new Path(outputInfo.getLocalLocation());
          Path remotePath = new Path(outputInfo.getDfsLocation() + "/_temporary/" + containerId.toString());
          if (workerNum == 1 && !conf.getBoolean(HboxConfiguration.HBOX_CREATE_CONTAINERID_DIR, HboxConfiguration.DEFAULT_HBOX_CREATE_CONTAINERID_DIR)) {
            remotePath = new Path(outputInfo.getDfsLocation() + "/_temporary/" + localPath.toString());
          }
          FileSystem dfs = remotePath.getFileSystem(conf);
          if (dfs.exists(remotePath)) {
            LOG.info("Container remote output path " + remotePath + "exists, so we has to delete is first.");
            dfs.delete(remotePath);
          }
          dfs.close();
          if (localFs.exists(localPath)) {
            String splitDir = localPath.toString();
            if (!localPath.toString().endsWith("/")) {
              splitDir = localPath.toString() + "/";
            } else if (!localPath.toString().startsWith("/")) {
              splitDir = "/" + localPath.toString();
            }
            FileStatus[] uploadFiles = localFs.listStatus(localPath);
            for (FileStatus uploadFile : uploadFiles) {
              Path uploadPath = uploadFile.getPath();
              LOG.debug("upload:" + uploadPath + " \tfrom\tlocalPath:" + localPath);
              String[] fileName = StringUtils.splitByWholeSeparator(uploadPath.toString() + "/", splitDir, 2);
              if (fileName.length == 2) {
                Path uploadDstPath = new Path(remotePath.toString() + "/" + fileName[1]);
                UploadTask uploadTask = new UploadTask(conf, uploadDstPath, uploadPath);
                LOG.debug("upload from " + uploadPath + " to " + uploadDstPath);
                executor.submit(uploadTask);
              } else {
                LOG.error("Get the local path error");
              }
            }
          }
          localFs.close();
        }
        boolean allUploadTaskFinished = false;
        executor.shutdown();
        do {
          try {
            executor.awaitTermination(Integer.MAX_VALUE, TimeUnit.SECONDS);
            allUploadTaskFinished = true;
          } catch (InterruptedException e) {
            reportFailedAndExit();
            break;
          }
        } while (!allUploadTaskFinished);
        LOG.info("All output files upload finished.");
      }
    }

    if (!(hboxAppType.equals("VPC") || hboxAppType.equals("DIGITS"))) {
      if (this.conf.get(HboxConfiguration.HBOX_TF_BOARD_LOG_DIR, HboxConfiguration.DEFAULT_HBOX_TF_BOARD_LOG_DIR).indexOf("hdfs://") == -1) {
        HboxConfiguration tfConf = new HboxConfiguration();
        tfConf.setBoolean("fs.hdfs.impl.disable.cache", true);
        tfConf.setBoolean("fs.hdfsold.impl.disable.cache", true);
        int boardIndex = this.conf.getInt(HboxConfiguration.HBOX_TF_BOARD_WORKER_INDEX, HboxConfiguration.DEFAULT_HBOX_TF_BOARD_WORKER_INDEX);
        Boolean boardEnable = this.conf.getBoolean(HboxConfiguration.HBOX_TF_BOARD_ENABLE, HboxConfiguration.DEFAULT_HBOX_TF_BOARD_ENABLE);
        String boardLogDir = this.conf.get(HboxConfiguration.HBOX_TF_BOARD_LOG_DIR, HboxConfiguration.DEFAULT_HBOX_TF_BOARD_LOG_DIR);
        Path localLogPath = new Path(boardLogDir);
        FileSystem boardLocalFs = FileSystem.getLocal(conf);
        Path boardHistoryDir;
        Path remoteLogPath;
        FileSystem boardDfs;
        if (conf.get(HboxConfiguration.HBOX_TF_BOARD_HISTORY_DIR, HboxConfiguration.DEFAULT_HBOX_TF_BOARD_HISTORY_DIR).equals(tfConf.get(HboxConfiguration.HBOX_TF_BOARD_HISTORY_DIR, HboxConfiguration.DEFAULT_HBOX_TF_BOARD_HISTORY_DIR))) {
          boardHistoryDir = new Path(conf.get(HboxConfiguration.HBOX_TF_BOARD_HISTORY_DIR,
              HboxConfiguration.DEFAULT_HBOX_TF_BOARD_HISTORY_DIR) + "/" + this.envs.get("APP_ID"));
          remoteLogPath = new Path(tfConf.get("fs.defaultFS"), boardHistoryDir);
          boardDfs = remoteLogPath.getFileSystem(tfConf);
        } else {
          boardHistoryDir = new Path(conf.get(HboxConfiguration.HBOX_TF_BOARD_HISTORY_DIR,
              HboxConfiguration.DEFAULT_HBOX_TF_BOARD_HISTORY_DIR));
          remoteLogPath = new Path(conf.get("fs.defaultFS"), boardHistoryDir);
          boardDfs = remoteLogPath.getFileSystem(conf);
        }

        if (boardUpload && boardLocalFs.exists(localLogPath) && boardEnable && boardIndex == this.index && !this.role.equals(HboxConstants.EVALUATOR)) {
          if (boardDfs.exists(remoteLogPath)) {
            LOG.info("Container remote board log output path " + remoteLogPath + "exists, so we has to delete is first.");
            boardDfs.delete(remoteLogPath);
          }
          boardDfs.copyFromLocalFile(false, false, localLogPath, remoteLogPath);
          LOG.info("Upload board  log dir " + localLogPath + " to remote path " + remoteLogPath + " finished.");
        }
        boardLocalFs.close();
        boardDfs.close();
      } else {
        LOG.info("User appoint the board log dir : " + this.conf.get(HboxConfiguration.HBOX_TF_BOARD_LOG_DIR));
        if (!(hboxAppType.equals("TENSORFLOW"))) {
          LOG.error("Note that VisualDL not support the hdfs path of logdir.");
        }
      }
    }
  }

  /**
   * build reLinksFiles for cacheFiles and cacheArchives for mpi app
   */
  public void reLinksFiles() {
    if (this.mpiAppDir.equals(envs.get("PWD"))) {
      LOG.info("PWD and mpi app dir are the same, so skip create symlinks");
      return;
    }
    if (envs.containsKey(HboxConstants.Environment.MPI_FILES_LINKS.toString())) {
      String linkFileStr = envs.get(HboxConstants.Environment.MPI_FILES_LINKS.toString());
      String[] linkFiles = StringUtils.split(linkFileStr, ",");
      try {
        for (String file : linkFiles) {
          java.nio.file.Path targetPath = Paths.get(envs.get("PWD"), file);
          java.nio.file.Path linkPath = Paths.get(this.mpiAppDir, file);
          File linkfile = new File(linkPath.toString());
          if (linkfile.exists()) {
            linkfile.delete();
          }
          Files.createSymbolicLink(linkPath, targetPath);
          LOG.info("create Symlink " + linkPath + " -> " + targetPath);
        }
      } catch (IOException e) {
        LOG.error("Create symlinks failed!", e);
      }
    }
  }

  private static synchronized String getPidOfProcess(Process p) {
    long pid = -1;
    try {
      if (p.getClass().getName().equals("java.lang.UNIXProcess")) {
        Field f = p.getClass().getDeclaredField("pid");
        f.setAccessible(true);
        pid = f.getLong(p);
        f.setAccessible(false);
      }
    } catch (Exception e) {
      pid = -1;
    }
    return Long.toString(pid);
  }

  private Boolean run() throws IOException {
    try {
      if(conf.getBoolean(HboxConfiguration.HBOX_TF_INPUT_PS_ENABLE, HboxConfiguration.DEFAULT_HBOX_TF_INPUT_PS_ENABLE)) {
        prepareInputFiles();
      } else if(conf.get(HboxConfiguration.HBOX_INPUT_STRATEGY, HboxConfiguration.DEFAULT_HBOX_INPUT_STRATEGY).equals("PLACEHOLDER")
              && conf.getBoolean(HboxConfiguration.HBOX_PLACEHOLDER_WHOLE_ENABLE, HboxConfiguration.DEFAULT_HBOX_PLACEHOLDER_WHOLE_ENABLE)) {
        prepareInputFiles();
      } else {
        if (this.role.equals(HboxConstants.WORKER)) {
          prepareInputFiles();
        } else if (conf.getBoolean(HboxConfiguration.HBOX_TF_EVALUATOR, HboxConfiguration.DEFAULT_HBOX_TF_EVALUATOR)){
          createLocalInputDir();
        }
      }
      if (this.conf.getBoolean(HboxConfiguration.HBOX_CONTAINER_AUTO_CREATE_OUTPUT_DIR, HboxConfiguration.DEFAULT_HBOX_CONTAINER_AUTO_CREATE_OUTPUT_DIR)) {
        createLocalOutputDir();
      }
    } catch (InterruptedException e) {
      LOG.error("Container prepare inputs failed!", e);
      this.reportFailedAndExit();
    } catch (ExecutionException e) {
      LOG.error("Container prepare inputs failed!", e);
      this.reportFailedAndExit();
    }

    if (hboxAppType.equals("MPI") || hboxAppType.equals("HOROVOD")) {
      reLinksFiles();
    }

    if ("TENSORFLOW".equals(hboxAppType) && !single) {
      LOG.info("Reserved available port: " + reservedSocket.getLocalPort());
      if (!this.role.equals(HboxConstants.EVALUATOR)) {
        amClient.reportReservedPort(envs.get(ApplicationConstants.Environment.NM_HOST.toString()),
            reservedSocket.getLocalPort(), this.role, this.index);
      }
      this.localAddress = envs.get(ApplicationConstants.Environment.NM_HOST.toString()) + ":" + reservedSocket.getLocalPort();
      LOG.info("Current localAddress: " + this.localAddress);

      while (!heartbeatThread.isHboxTrainCompleted()) {
        //TODO may be need encode use Base64 while used in Env
        this.clusterDef = amClient.getClusterDef();
        if (this.clusterDef != null) {
          LOG.info("Cluster def is: " + this.clusterDef);
          break;
        }
        Utilities.sleep(this.conf.getInt(HboxConfiguration.HBOX_CONTAINER_UPDATE_APP_STATUS_INTERVAL, HboxConfiguration.DEFAULT_HBOX_CONTAINER_UPDATE_APP_STATUS_INTERVAL));
      }
      if (heartbeatThread.isHboxTrainCompleted()) {
        return false;
      }

      Map<String, Object> taskInfo = new HashMap<>();
      if (this.role.equals("ps")) {
        taskInfo.put("type", this.role);
      } else {
        taskInfo.put("type", "master");
      }
      taskInfo.put("index", this.index);

      Gson gson = new Gson();
      Map<String, Object> clusterInfo = new HashMap<>();
      clusterInfo = (Map) gson.fromJson(this.clusterDef.replaceAll("worker", "master"), clusterInfo.getClass());

      Map<String, Object> tfConfigInfo = new HashMap<>();
      tfConfigInfo.put("cluster", clusterInfo);
      tfConfigInfo.put("task", taskInfo);
      tfConfigInfo.put("environment", "cloud");

      LOG.info("Tensor2Tensor distribute mode needs the TF_CONFIG: " + new Gson().toJson(tfConfigInfo));
      this.tfConfig = new Gson().toJson(tfConfigInfo);
    }

    if (hboxAppType.equals("DISTLIGHTGBM")) {
      LOG.info("Reserved available port: " + reservedSocket.getLocalPort());
      this.lightGBMLocalPort = reservedSocket.getLocalPort();
      InetAddress address = null;
      try {
        address = InetAddress.getByName(envs.get(ApplicationConstants.Environment.NM_HOST.toString()));
      } catch (UnknownHostException e) {
        LOG.info("acquire host ip failed " + e);
        reportFailedAndExit();
      }
      String ipPortStr = address.getHostAddress() + " " + reservedSocket.getLocalPort();
      LOG.info("lightGBM ip port string is: " + ipPortStr);
      amClient.reportLightGbmIpPort(containerId, ipPortStr);
      String lightGBMIpPortStr = null;
      while (!heartbeatThread.isHboxTrainCompleted()) {
        //TODO may be need encode use Base64 while used in Env
        lightGBMIpPortStr = amClient.getLightGbmIpPortStr();
        if (lightGBMIpPortStr != null) {
          LOG.info("lightGBM IP PORT list is: " + lightGBMIpPortStr);
          break;
        }
        Utilities.sleep(this.conf.getInt(HboxConfiguration.HBOX_CONTAINER_UPDATE_APP_STATUS_INTERVAL, HboxConfiguration.DEFAULT_HBOX_CONTAINER_UPDATE_APP_STATUS_INTERVAL));
      }
      if (heartbeatThread.isHboxTrainCompleted()) {
        return false;
      }
      Type type = new TypeToken<ConcurrentHashMap<String, String>>() {
      }.getType();
      ConcurrentHashMap<String, String> map = new Gson().fromJson(lightGBMIpPortStr, type);
      PrintWriter writer = new PrintWriter("lightGBMlist.txt", "UTF-8");
      for (String str : map.keySet()) {
        writer.println(map.get(str));
      }
      writer.close();

    }

    if (hboxAppType.equals("DISTLIGHTLDA")) {
      if (this.role.equals(HboxConstants.PS)) {
        LOG.info("Reserved available port: " + reservedSocket.getLocalPort());
        this.lightLDALocalPort = reservedSocket.getLocalPort();
        InetAddress address = null;
        try {
          address = InetAddress.getByName(envs.get(ApplicationConstants.Environment.NM_HOST.toString()));
        } catch (UnknownHostException e) {
          LOG.info("acquire host ip failed " + e);
          reportFailedAndExit();
        }
        String ipPortStr = this.index + " " + address.getHostAddress() + ":" + reservedSocket.getLocalPort();
        this.lightLDAEndpoint = address.getHostAddress() + ":" + reservedSocket.getLocalPort();
        LOG.info("lightLDA ip port string is: " + ipPortStr);
        amClient.reportLightLdaIpPort(containerId, ipPortStr);
      }
      if (this.role.equals(HboxConstants.WORKER)) {
        String lightLDAIpPortStr = null;
        while (!heartbeatThread.isHboxTrainCompleted()) {
          //TODO may be need encode use Base64 while used in Env
          lightLDAIpPortStr = amClient.getLightLdaIpPortStr();
          if (lightLDAIpPortStr != null) {
            LOG.info("lightLDA IP PORT list is: " + lightLDAIpPortStr);
            break;
          }
          Utilities.sleep(this.conf.getInt(HboxConfiguration.HBOX_CONTAINER_UPDATE_APP_STATUS_INTERVAL, HboxConfiguration.DEFAULT_HBOX_CONTAINER_UPDATE_APP_STATUS_INTERVAL));
        }
        if (heartbeatThread.isHboxTrainCompleted()) {
          return false;
        }
        Type type = new TypeToken<ConcurrentHashMap<String, String>>() {
        }.getType();
        ConcurrentHashMap<String, String> map = new Gson().fromJson(lightLDAIpPortStr, type);
        PrintWriter writer = new PrintWriter("lightLdaEndPoints.txt", "UTF-8");
        for (String str : map.keySet()) {
          writer.println(map.get(str));
        }
        writer.close();
      }
    }

    if (hboxAppType.equals("DISTTORCH")) {
      if(this.index == 0) {
        this.torchRank0Port = reservedSocket.getLocalPort();
        LOG.info("Reserved available port: " + torchRank0Port);
        try {
          InetAddress address = InetAddress.getByName(envs.get(ApplicationConstants.Environment.NM_HOST.toString()));
          this.torchRank0IP = address.getHostAddress() + ":" + torchRank0Port;
        } catch (UnknownHostException e) {
          LOG.info("acquire host ip failed " + e);
          reportFailedAndExit();
        }
        LOG.info("torch rank 0 ip port string is: " + torchRank0Port);
        amClient.reportTorchRank0IP(torchRank0IP);
      } else {
        while (!heartbeatThread.isHboxTrainCompleted()) {
          //TODO may be need encode use Base64 while used in Env
          this.torchRank0IP = amClient.getTorchRank0IP();
          if (torchRank0IP != null) {
            LOG.info("Torch Rank 0 IP:Port is: " + torchRank0IP);
            break;
          }
          Utilities.sleep(this.conf.getInt(HboxConfiguration.HBOX_CONTAINER_UPDATE_APP_STATUS_INTERVAL, HboxConfiguration.DEFAULT_HBOX_CONTAINER_UPDATE_APP_STATUS_INTERVAL));
        }
        if (heartbeatThread.isHboxTrainCompleted()) {
          return false;
        }
      }

    }

    /**
     * set TF_CLUSTER_DEF in env
     * python script can load cluster def use "json.loads(os.environ["CLUSTER_DEF"])"
     */
    List<String> envList = new ArrayList<>(20);
    String cudaEnv;
    int cudaNum = 0;
    if (!System.getenv().containsKey(HboxConstants.Environment.HBOX_CUDA_VISIBLE_DEVICES.toString())) {
      cudaEnv = "";
    } else {
      cudaEnv = System.getenv(HboxConstants.Environment.HBOX_CUDA_VISIBLE_DEVICES.toString());
      cudaNum = StringUtils.split(cudaEnv, ',').length;
    }
    this.amClient.reportGPUDevice(containerId, cudaEnv);

    LOG.debug("hadoop user name:" + System.getenv(HboxConstants.Environment.HADOOP_USER_NAME.toString()));
    String containerType = conf.get(HboxConfiguration.CONTAINER_EXECUTOR_TYPE, HboxConfiguration.DEFAULT_CONTAINER_EXECUTOR_TYPE).toUpperCase();
    String cudaVisibleDevicesEnv = "CUDA_VISIBLE_DEVICES=" + cudaEnv;

    if (conf.get(HboxConfiguration.HBOX_CONTAINER_ENV) != null) {
      String[] env = StringUtils.split(conf.get(HboxConfiguration.HBOX_CONTAINER_ENV), "|");
      for (String envPair : env) {
        if (StringUtils.split(envPair, "=").length != 2) {
          LOG.error(envPair + " is not correct");
        } else {
          envList.add(envPair);
        }
      }
    }

    envList.add("PATH=" + System.getenv("PATH"));
    envList.add("JAVA_HOME=" + System.getenv("JAVA_HOME"));
    envList.add("HADOOP_HOME=" + System.getenv("HADOOP_HOME"));
    envList.add("HADOOP_HDFS_HOME=" + System.getenv("HADOOP_HDFS_HOME"));
    envList.add("LD_LIBRARY_PATH=" + "./:" + System.getenv("LD_LIBRARY_PATH") + ":" + System.getenv("JAVA_HOME") +
            "/jre/lib/amd64/server:" + System.getenv("HADOOP_HOME") + "/lib/native");
    envList.add("CLASSPATH=" + "./:" + System.getenv("CLASSPATH") + ":" + System.getProperty("java.class.path"));
    envList.add("PYTHONUNBUFFERED=1");

    if (conf.get(HboxConfiguration.HBOX_INPUT_STRATEGY, HboxConfiguration.DEFAULT_HBOX_INPUT_STRATEGY).toUpperCase().equals("PLACEHOLDER")) {
      envList.add("HADOOP_VERSION=2.7.2");
      envList.add("HADOOP_CONF_DIR=./:" + System.getenv("HADOOP_CONF_DIR"));
    }

    if ("TENSORFLOW".equals(hboxAppType)) {
      if (containerType.equals("DOCKER")) {
        cudaVisibleDevicesEnv = "";
      }
      envList.add(cudaVisibleDevicesEnv);
      envList.add(HboxConstants.Environment.HBOX_TF_INDEX.toString() + "=" + this.index);
      envList.add(HboxConstants.Environment.HBOX_TF_ROLE.toString() + "=" + this.role);
      envList.add(HboxConstants.Environment.HBOX_CUDA_VISIBLE_DEVICES_NUM.toString() + "=" + cudaNum);
      if (!single) {
        envList.add(HboxConstants.Environment.HBOX_TF_CLUSTER_DEF.toString() + "=" + this.clusterDef);
        envList.add(HboxConstants.Environment.HBOX_TF_CONFIG.toString() + "=" + this.tfConfig);
        envList.add(HboxConstants.Environment.HBOX_LOCAL_ADDRESS.toString() + "=" + this.localAddress);
      }
    } else if (hboxAppType.equals("MXNET")) {
      if (containerType.equals("DOCKER")) {
        cudaVisibleDevicesEnv = "";
      }
      envList.add(cudaVisibleDevicesEnv);
      envList.add(HboxConstants.Environment.HBOX_CUDA_VISIBLE_DEVICES_NUM.toString() + "=" + cudaNum);
      if (!singleMx) {
        String dmlcID;
        if (this.role.equals("worker")) {
          dmlcID = "DMLC_WORKER_ID";
        } else {
          dmlcID = "DMLC_SERVER_ID";
        }
        envList.add("DMLC_PS_ROOT_URI=" + System.getenv("DMLC_PS_ROOT_URI"));
        envList.add("DMLC_PS_ROOT_PORT=" + System.getenv("DMLC_PS_ROOT_PORT"));
        envList.add("DMLC_NUM_WORKER=" + System.getenv("DMLC_NUM_WORKER"));
        envList.add("DMLC_NUM_SERVER=" + System.getenv("DMLC_NUM_SERVER"));
        envList.add(dmlcID + "=" + this.index);
        envList.add("DMLC_ROLE=" + this.role);
      }
    } else if (hboxAppType.equals("DISTXGBOOST")) {
      if (containerType.equals("DOCKER")) {
        cudaVisibleDevicesEnv = "";
      }
      envList.add(cudaVisibleDevicesEnv);
      envList.add(HboxConstants.Environment.HBOX_CUDA_VISIBLE_DEVICES_NUM.toString() + "=" + cudaNum);
      envList.add("DMLC_TRACKER_URI=" + System.getenv("DMLC_TRACKER_URI"));
      envList.add("DMLC_TRACKER_PORT=" + System.getenv("DMLC_TRACKER_PORT"));
      envList.add("DMLC_NUM_WORKER=" + System.getenv("DMLC_NUM_WORKER"));
      envList.add("DMLC_TASK_ID=" + this.index);
      envList.add("DMLC_ROLE=" + this.role);
    } else if (hboxAppType.equals("DISTLIGHTGBM")) {
      if (containerType.equals("DOCKER")) {
        cudaVisibleDevicesEnv = "";
      }
      envList.add(cudaVisibleDevicesEnv);
      envList.add(HboxConstants.Environment.HBOX_CUDA_VISIBLE_DEVICES_NUM.toString() + "=" + cudaNum);
      envList.add("LIGHTGBM_NUM_MACHINE=" + System.getenv(HboxConstants.Environment.HBOX_LIGHTGBM_WORKER_NUM.toString()));
      envList.add("LIGHTGBM_LOCAL_LISTEN_PORT=" + this.lightGBMLocalPort);
    } else if (hboxAppType.equals("DISTLIGHTLDA")) {
      if (containerType.equals("DOCKER")) {
        cudaVisibleDevicesEnv = "";
      }
      envList.add(cudaVisibleDevicesEnv);
      envList.add(HboxConstants.Environment.HBOX_CUDA_VISIBLE_DEVICES_NUM.toString() + "=" + cudaNum);
      envList.add("LIGHTLDA_WORKER_NUM=" + System.getenv(HboxConstants.Environment.HBOX_LIGHTLDA_WORKER_NUM.toString()));
      envList.add("LIGHTLDA_SERVER_NUM=" + System.getenv(HboxConstants.Environment.HBOX_LIGHTLDA_PS_NUM.toString()));
      envList.add("LIGHTLDA_RANK=" + this.index);
      envList.add("LIGHTLDA_SERVER_ENDPOINT=" + this.lightLDAEndpoint);
      envList.add("LIGHTLDA_ROLE=" + this.role);
    } else if (hboxAppType.equals("VPC")) {
      envList.add("PYTHONPATH=" + System.getenv("PYTHONPATH"));
    } else if (hboxAppType.equals("DIGITS")) {
      envList.add("PYTHONPATH=" + System.getenv("PYTHONPATH"));
    } else if (hboxAppType.equals("MPI") || hboxAppType.equals("HOROVOD")) {
      StringBuilder ldLibraryPath = new StringBuilder();
      String mpiExtraLdLibraryPath = conf.get(HboxConfiguration.HBOX_MPI_EXTRA_LD_LIBRARY_PATH);
      if (mpiExtraLdLibraryPath != null) {
        ldLibraryPath.append(mpiExtraLdLibraryPath);
        LOG.info("add " + ldLibraryPath + " to LD_LIBRARY_PATH");
      }
      if (conf.getBoolean(HboxConfiguration.HBOX_MPI_INSTALL_DIR_ENABLE, HboxConfiguration.DEFAULT_HBOX_MPI_INSTALL_DIR_ENABLE)) {
        String mpiInstallDir = conf.get(HboxConfiguration.HBOX_MPI_INSTALL_DIR, HboxConfiguration.DEFAULT_HBOX_MPI_INSTALL_DIR);
        ldLibraryPath.append(":" + mpiInstallDir + File.separator + "lib");
      }
      ldLibraryPath.append(":" + System.getenv("LD_LIBRARY_PATH"));
      envList.add("PATH=" + System.getenv("PATH"));
      envList.add("PWD=" + this.mpiAppDir);
      envList.add("LD_LIBRARY_PATH=" + ldLibraryPath.toString());
    } else if (hboxAppType.equals("XFLOW")) {
      if (containerType.equals("DOCKER")) {
        cudaVisibleDevicesEnv = "";
      }
      envList.add(cudaVisibleDevicesEnv);
      envList.add(HboxConstants.Environment.HBOX_CUDA_VISIBLE_DEVICES_NUM.toString() + "=" + cudaNum);
      String dmlcID;
      String heapprofile;
      if (this.role.equals("worker")) {
        dmlcID = "DMLC_WORKER_ID";
        heapprofile = "./W";
      } else {
        dmlcID = "DMLC_SERVER_ID";
        heapprofile = "./S";
      }
      envList.add("DMLC_PS_ROOT_URI=" + System.getenv("DMLC_PS_ROOT_URI"));
      envList.add("DMLC_PS_ROOT_PORT=" + System.getenv("DMLC_PS_ROOT_PORT"));
      envList.add("DMLC_NUM_WORKER=" + System.getenv("DMLC_NUM_WORKER"));
      envList.add("DMLC_NUM_SERVER=" + System.getenv("DMLC_NUM_SERVER"));
      envList.add(dmlcID + "=" + this.index);
      envList.add("DMLC_ROLE=" + this.role);
      envList.add("HEAPPROFILE=" + heapprofile + this.index);
    } else if (hboxAppType.equals("DISTTORCH")) {
      if (containerType.equals("DOCKER")) {
        cudaVisibleDevicesEnv = "";
      }
      envList.add(cudaVisibleDevicesEnv);
      envList.add(HboxConstants.Environment.HBOX_CUDA_VISIBLE_DEVICES_NUM.toString() + "=" + cudaNum);
      envList.add("INIT_METHOD=tcp://" + this.torchRank0IP);
      envList.add("RANK=" + this.index);
      envList.add("WORLD_SIZE=" + System.getenv("WORLD_SIZE"));
    } else {
      if (containerType.equals("DOCKER")) {
        cudaVisibleDevicesEnv = "";
      }
      envList.add(cudaVisibleDevicesEnv);
      envList.add(HboxConstants.Environment.HBOX_CUDA_VISIBLE_DEVICES_NUM.toString() + "=" + cudaNum);
    }

    if (conf.get(HboxConfiguration.HBOX_INPUT_STRATEGY, HboxConfiguration.DEFAULT_HBOX_INPUT_STRATEGY).equals("PLACEHOLDER")) {
      envList.add(HboxConstants.Environment.HBOX_INPUT_FILE_LIST.toString() + "=" + this.inputFileList);
      if (envList.toString().length() > conf.getInt(HboxConfiguration.HBOX_ENV_MAXLENGTH, HboxConfiguration.DEFAULT_HBOX_ENV_MAXLENGTH)) {
        LOG.warn("Current container environments length " + envList.toString().length() + " exceed the configuration " + HboxConfiguration.HBOX_ENV_MAXLENGTH + " " + conf.getInt(HboxConfiguration.HBOX_ENV_MAXLENGTH, HboxConfiguration.DEFAULT_HBOX_ENV_MAXLENGTH));
        envList.remove(envList.size() - 1);
        LOG.warn("InputFile list had written to local file: inputFileList.txt !!");
        PrintWriter writer = new PrintWriter("inputFileList.txt", "UTF-8");
        writer.println(this.inputFileList);
        writer.close();
      }
    }

    String command;
    //String containerType = conf.get(HboxConfiguration.CONTAINER_EXECUTOR_TYPE, HboxConfiguration.DEFAULT_CONTAINER_EXECUTOR_TYPE).toUpperCase();
    if (hboxAppType.equals("VPC") || hboxAppType.equals("DIGITS")) {
      String dockerPort = envs.get("DOCKER_PORT");
      String password = envs.get("DOCKER_PASSWORD");
      String vpcCommandAndPasswd = "root@" + envs.get(ApplicationConstants.Environment.NM_HOST.toString()) + " -p " + dockerPort + ":" + password;
      amClient.reportVPCCommandAndPasswd(containerId, vpcCommandAndPasswd);
      String duration = this.conf.get(HboxConfiguration.HBOX_VPC_DURATION, HboxConfiguration.DEFAULT_HBOX_VPC_DURATION);
      if (hboxAppType.equals("VPC")) {
        if (duration.equals("0")) {
          command = "sleep 32767d";
        } else {
          command = "sleep " + duration;
        }
      } else {
        int digitsPort = reservedSocket.getLocalPort();
        LOG.info("Reserved digits available port: " + digitsPort);
        String digitsUrl = "http://" + envs.get(ApplicationConstants.Environment.NM_HOST.toString()) + ":" + digitsPort;
        amClient.reportDigitsUrl(containerId, digitsUrl);
        String digitsShellname = "digits_" + containerId.toString() + ".sh";
        String digitsServerCmd = "python -m digits -p " + digitsPort + " &";
        String digitsSleepCmd;
        if (duration.equals("0")) {
          digitsSleepCmd = "sleep 32767d";
        } else {
          digitsSleepCmd = "sleep " + duration;
        }
        PrintWriter writer = new PrintWriter(digitsShellname, "UTF-8");
        writer.println(digitsServerCmd);
        writer.println(digitsSleepCmd);
        writer.close();
        command = "sh " + digitsShellname;
      }
    } else if (hboxAppType.equals("MPI") || hboxAppType.equals("HOROVOD")) {
      command = envs.get(HboxConstants.Environment.CONTAINER_COMMAND.toString()).replaceAll("#", "\"");
    } else {
      if (containerType.equals("DOCKER")) {
        String dockerPort = envs.get("DOCKER_PORT");
        String password = envs.get("DOCKER_PASSWORD");
        String vpcCommandAndPasswd = "root@" + envs.get(ApplicationConstants.Environment.NM_HOST.toString()) + " -p " + dockerPort + ":" + password;
        amClient.reportVPCCommandAndPasswd(containerId, vpcCommandAndPasswd);
      }
      command = envs.get(HboxConstants.Environment.HBOX_EXEC_CMD.toString());
    }
    LOG.info("Executing command:" + command);

    Runtime rt = Runtime.getRuntime();

    //close reserved socket as tf will bind this port later
    this.reservedSocket.close();
    String[] env = envList.toArray(new String[envList.size()]);
    final Process hboxProcess;
    if (hboxAppType.equals("MPI") || hboxAppType.equals("HOROVOD")) {
      hboxProcess = rt.exec(command, env, new File(this.mpiAppDir));
    } else {
      hboxProcess = rt.exec(command, env);
    }
    Date now = new Date();
    heartbeatThread.setContainersStartTime(now.toString());

    if (this.conf.getBoolean(HboxConfiguration.HBOX_INPUT_STREAM, HboxConfiguration.DEFAULT_HBOX_INPUT_STREAM) ||
        conf.get(HboxConfiguration.HBOX_INPUT_STRATEGY, HboxConfiguration.DEFAULT_HBOX_INPUT_STRATEGY).equals("STREAM")) {
      LOG.info("Starting thread to redirect stdin of hbox process");
      Thread stdinRedirectThread = new Thread(new Runnable() {
        @Override
        public void run() {
          try {
            OutputStreamWriter osw = new OutputStreamWriter(hboxProcess.getOutputStream());
            File gzFile = new File("inputformatCache.gz");
            GZIPOutputStream gos = new GZIPOutputStream(new FileOutputStream(gzFile));
            boolean isCache = conf.getBoolean(HboxConfiguration.HBOX_INPUTFORMAT_CACHE, HboxConfiguration.DEFAULT_HBOX_INPUTFORMAT_CACHE);
            List<InputSplit> inputs = Arrays.asList(amClient.getStreamInputSplit(containerId));
            JobConf jobConf = new JobConf(conf);
            RecordReader reader;
            InputFormat inputFormat = ReflectionUtils.newInstance(conf.getClass(HboxConfiguration.HBOX_INPUTF0RMAT_CLASS, HboxConfiguration.DEFAULT_HBOX_INPUTF0RMAT_CLASS, InputFormat.class),
                jobConf);
            for (int j = 0; j < conf.getInt(HboxConfiguration.HBOX_STREAM_EPOCH, HboxConfiguration.DEFAULT_HBOX_STREAM_EPOCH); j++) {
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
                    if (j == 0 && isCache) {
                      if (conf.getInt(HboxConfiguration.HBOX_STREAM_EPOCH, HboxConfiguration.DEFAULT_HBOX_STREAM_EPOCH) > 1) {
                        gos.write(value.toString().getBytes());
                        gos.write("\n".getBytes());
                        if ((gzFile.length() / 1024 / 1024) > conf.getInt(HboxConfiguration.HBOX_INPUTFORMAT_CACHESIZE_LIMIT, HboxConfiguration.DEFAULT_HBOX_INPUTFORMAT_CACHESIZE_LIMIT)) {
                          LOG.info("Inputformat cache file size is:" + gzFile.length() / 1024 / 1024 + "M "
                              + "beyond the limit size:" + conf.getInt(HboxConfiguration.HBOX_INPUTFORMAT_CACHESIZE_LIMIT, HboxConfiguration.DEFAULT_HBOX_INPUTFORMAT_CACHESIZE_LIMIT) + "M.");
                          gzFile.delete();
                          LOG.info("Local cache file deleted and will not use cache.");
                          isCache = false;
                        }
                      }
                    }
                  } catch (EOFException e) {
                    finished = true;
                    e.printStackTrace();
                  }
                }
                reader.close();
                LOG.info("split " + (i + 1) + " is finished.");
              }
              LOG.info("Epoch " + (j + 1) + " finished.");
              if (isCache) {
                break;
              }
            }
            osw.close();
            gos.close();
          } catch (Exception e) {
            LOG.warn("Exception in thread stdinRedirectThread");
            e.printStackTrace();
          }
        }
      });
      stdinRedirectThread.start();
    }

    List<OutputInfo> outputs = Arrays.asList(amClient.getOutputLocation());
    if ((this.conf.getBoolean(HboxConfiguration.HBOX_OUTPUT_STREAM, HboxConfiguration.DEFAULT_HBOX_OUTPUT_STREAM)
        || this.conf.get(HboxConfiguration.HBOX_OUTPUT_STRATEGY, HboxConfiguration.DEFAULT_HBOX_OUTPUT_STRATEGY).equals("STREAM")) && outputs.size() > 0) {
      LOG.info("Starting thread to redirect stream stdout of hbox process");
      final Thread stdoutRedirectThread = new Thread(new Runnable() {
        @Override
        public void run() {
          try {
            BufferedReader reader;
            reader = new BufferedReader(new InputStreamReader(hboxProcess.getInputStream()));
            List<OutputInfo> outputs = Arrays.asList(amClient.getOutputLocation());
            JobConf jobConf = new JobConf(conf);
            jobConf.setOutputKeyClass(Text.class);
            jobConf.setOutputValueClass(Text.class);
            jobConf.setBoolean("mapred.output.compress", true);
            jobConf.set("mapred.output.compression.codec", "org.apache.hadoop.io.compress.GzipCodec");
            jobConf.setOutputFormat(TextMultiOutputFormat.class);
            Path remotePath = new Path(outputs.get(0).getDfsLocation() + "/_temporary/" + containerId.toString());
            FileSystem dfs = remotePath.getFileSystem(jobConf);
            //FileOutputFormat.setOutputPath(jobConf, remotePath.makeQualified(dfs));
            jobConf.set(HboxConstants.STREAM_OUTPUT_DIR, remotePath.makeQualified(dfs).toString());
            //TextMultiOutputFormat outputFormat = ReflectionUtils.newInstance(TextMultiOutputFormat.class, jobConf);
            OutputFormat outputFormat = ReflectionUtils.newInstance(conf.getClass(HboxConfiguration.HBOX_OUTPUTFORMAT_CLASS, HboxConfiguration.DEFAULT_HBOX_OUTPUTF0RMAT_CLASS, OutputFormat.class),
                jobConf);
            outputFormat.checkOutputSpecs(dfs, jobConf);
            JobID jobID = new JobID(new SimpleDateFormat("yyyyMMddHHmm").format(new Date()), 0);
            TaskAttemptID taId = new TaskAttemptID(new TaskID(jobID, true, 0), 0);
            jobConf.set("mapred.tip.id", taId.getTaskID().toString());
            jobConf.set("mapred.task.id", taId.toString());
            jobConf.set("mapred.job.id", jobID.toString());
            amClient.reportMapedTaskID(containerId, taId.toString());
            RecordWriter writer = outputFormat.getRecordWriter(dfs, jobConf, "part-r", Reporter.NULL);
            String hboxStreamResultLine;
            while ((hboxStreamResultLine = reader.readLine()) != null) {
              //LOG.info(hboxStreamResultLine);
              writer.write(null, hboxStreamResultLine);
              //reader.skip(1);
            }
            writer.close(Reporter.NULL);
            reader.close();
          } catch (Exception e) {
            LOG.warn("Exception in thread stdoutRedirectThread");
            e.printStackTrace();
          }
        }
      });
      stdoutRedirectThread.start();
    } else {
      LOG.info("Starting thread to redirect stdout of hbox process");
      Thread stdoutRedirectThread = new Thread(new Runnable() {
        @Override
        public void run() {
          try {
            BufferedReader reader;
            reader = new BufferedReader(new InputStreamReader(hboxProcess.getInputStream()));
            String hboxStdoutLog;
            while ((hboxStdoutLog = reader.readLine()) != null) {
              LOG.info(hboxStdoutLog);
              if (conf.getBoolean(HboxConfiguration.HBOX_CONTAINER_RUNNING_LOG_ENABLE, HboxConfiguration.DEFAULT_HBOX_CONTAINER_RUNNING_LOG_ENABLE)) {
                heartbeatThread.appendContainerStdOut(hboxStdoutLog);
              }
            }
          } catch (Exception e) {
            LOG.warn("Exception in thread stdoutRedirectThread");
            e.printStackTrace();
          }
        }
      });
      stdoutRedirectThread.start();
    }

    LOG.info("Starting thread to redirect stderr of hbox process");
    Thread stderrRedirectThread = new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          BufferedReader reader;
          reader = new BufferedReader(new InputStreamReader(hboxProcess.getErrorStream()));
          String hboxStderrLog;
          while ((hboxStderrLog = reader.readLine()) != null) {
            if (conf.getBoolean(HboxConfiguration.HBOX_CONTAINER_RUNNING_LOG_ENABLE, HboxConfiguration.DEFAULT_HBOX_CONTAINER_RUNNING_LOG_ENABLE) && !(hboxAppType.equals("HOROVOD") || hboxAppType.equals("MPI") || hboxAppType.equals("VPC") || hboxAppType.equals("DIGITS"))) {
              heartbeatThread.appendContainerStdErr(hboxStderrLog);
            }
            if (hboxStderrLog.contains("reporter progress")) {
              heartbeatThread.setProgressLog(hboxStderrLog);
            } else if (hboxStderrLog.contains("FSDataInputStream#read error") || hboxStderrLog.contains("Byte-buffer read unsupported by input stream") || hboxStderrLog.contains("org.apache.hadoop.fs.FSDataInputStream.read(FSDataInputStream.java")) {
              continue;
            } else {
              LOG.info(hboxStderrLog);
              if (hboxAppType.equals("MPI") || hboxAppType.equals("HOROVOD")) {
                if (hboxStderrLog.contains("Permission denied")) {
                  LOG.info("bind failed, now am retry.");
                  reportFailedAndExit();
                }
              }
            }
          }
        } catch (Exception e) {
          LOG.warn("Error in thread stderrRedirectThread");
          e.printStackTrace();
        }
      }
    });
    stderrRedirectThread.start();

    //amClient.reportStatus(containerId, HboxContainerStatus.RUNNING);
    heartbeatThread.setContainerStatus(HboxContainerStatus.RUNNING);

    if (!(hboxAppType.equals("VPC") || hboxAppType.equals("DIGITS"))) {
      //Start Board process
      int boardIndex = this.conf.getInt(HboxConfiguration.HBOX_TF_BOARD_WORKER_INDEX, HboxConfiguration.DEFAULT_HBOX_TF_BOARD_WORKER_INDEX);
      Boolean boardEnable = this.conf.getBoolean(HboxConfiguration.HBOX_TF_BOARD_ENABLE, HboxConfiguration.DEFAULT_HBOX_TF_BOARD_ENABLE);
      Boolean boardPsEnable = this.conf.getBoolean(HboxConfiguration.HBOX_TF_BOARD_PS_ENABLE, HboxConfiguration.DEFAULT_HBOX_TF_BOARD_PS_ENABLE);
      if(boardEnable) {
        if(boardPsEnable) {
          if (this.role.equals(HboxConstants.PS) && boardIndex == this.index) {
            Socket boardReservedSocket = new Socket();
            try {
              Utilities.getReservePort(boardReservedSocket, InetAddress.getByName(localHost).getHostAddress(), reservePortBegin, reservePortEnd);
            } catch (IOException e) {
              LOG.error("Can not get available port");
              reportFailedAndExit();
            }
            String boardHost = envs.get(ApplicationConstants.Environment.NM_HOST.toString());
            String boardLogDir = this.conf.get(HboxConfiguration.HBOX_TF_BOARD_LOG_DIR, HboxConfiguration.DEFAULT_HBOX_TF_BOARD_LOG_DIR);
            int boardPort = boardReservedSocket.getLocalPort();
            String boardCommand;
            if ("TENSORFLOW".equals(hboxAppType)) {
              int boardReloadInterval = this.conf.getInt(HboxConfiguration.HBOX_TF_BOARD_RELOAD_INTERVAL, HboxConfiguration.DEFAULT_HBOX_TF_BOARD_RELOAD_INTERVAL);
              boardCommand = "tensorboard --host=" + boardHost + " --port=" + boardPort + " --reload_interval=" + boardReloadInterval + " --logdir=" + boardLogDir;
            } else {
              int boardCacheTimeout = this.conf.getInt(HboxConfiguration.HBOX_BOARD_CACHE_TIMEOUT, HboxConfiguration.DEFAULT_HBOX_BOARD_CACHE_TIMEOUT);
              boardCommand = "visualDL --host=" + boardHost + " --port=" + boardPort + " --logdir=" + boardLogDir + " --cache_timeout=" + boardCacheTimeout;
              String modelpb = this.conf.get(HboxConfiguration.HBOX_BOARD_MODELPB, HboxConfiguration.DEFAULT_HBOX_BOARD_MODELPB);
              if (!(modelpb.equals("") || modelpb == null)) {
                boardCommand = boardCommand + " --model_pb=" + modelpb;
              }
            }
            String boardUrl = "http://" + boardHost + ":" + boardPort;
            LOG.info("Executing board command:" + boardCommand);
            boardReservedSocket.close();
            try {
              final Process boardProcess = rt.exec(boardCommand, env);
              LOG.info("Starting thread to redirect stdout of board process");
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

              LOG.info("Starting thread to redirect stderr of board process");
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
              LOG.info("Container index is " + index + ", report board url:" + boardUrl);
            } catch (Exception e) {
              LOG.error("Board Process failed. For more detail: " + e);
            }
          }
        } else {
          if(this.role.equals(HboxConstants.WORKER) && boardIndex == this.index) {
            Socket boardReservedSocket = new Socket();
            try {
              Utilities.getReservePort(boardReservedSocket, InetAddress.getByName(localHost).getHostAddress(), reservePortBegin, reservePortEnd);
            } catch (IOException e) {
              LOG.error("Can not get available port");
              reportFailedAndExit();
            }
            String boardHost = envs.get(ApplicationConstants.Environment.NM_HOST.toString());
            String boardLogDir = this.conf.get(HboxConfiguration.HBOX_TF_BOARD_LOG_DIR, HboxConfiguration.DEFAULT_HBOX_TF_BOARD_LOG_DIR);
            int boardPort = boardReservedSocket.getLocalPort();
            String boardCommand;
            if ("TENSORFLOW".equals(hboxAppType)) {
              int boardReloadInterval = this.conf.getInt(HboxConfiguration.HBOX_TF_BOARD_RELOAD_INTERVAL, HboxConfiguration.DEFAULT_HBOX_TF_BOARD_RELOAD_INTERVAL);
              boardCommand = "tensorboard --host=" + boardHost + " --port=" + boardPort + " --reload_interval=" + boardReloadInterval + " --logdir=" + boardLogDir;
            } else {
              int boardCacheTimeout = this.conf.getInt(HboxConfiguration.HBOX_BOARD_CACHE_TIMEOUT, HboxConfiguration.DEFAULT_HBOX_BOARD_CACHE_TIMEOUT);
              boardCommand = "visualDL --host=" + boardHost + " --port=" + boardPort + " --logdir=" + boardLogDir + " --cache_timeout=" + boardCacheTimeout;
              String modelpb = this.conf.get(HboxConfiguration.HBOX_BOARD_MODELPB, HboxConfiguration.DEFAULT_HBOX_BOARD_MODELPB);
              if (!(modelpb.equals("") || modelpb == null)) {
                boardCommand = boardCommand + " --model_pb=" + modelpb;
              }
            }
            String boardUrl = "http://" + boardHost + ":" + boardPort;
            LOG.info("Executing board command:" + boardCommand);
            boardReservedSocket.close();
            try {
              final Process boardProcess = rt.exec(boardCommand, env);
              LOG.info("Starting thread to redirect stdout of board process");
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

              LOG.info("Starting thread to redirect stderr of board process");
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
              LOG.info("Container index is " + index + ", report board url:" + boardUrl);
            } catch (Exception e) {
              LOG.error("Board Process failed. For more detail: " + e);
            }
          }
        }
      }
    }

    int updateAppStatusInterval = this.conf.getInt(HboxConfiguration.HBOX_CONTAINER_UPDATE_APP_STATUS_INTERVAL, HboxConfiguration.DEFAULT_HBOX_CONTAINER_UPDATE_APP_STATUS_INTERVAL);
    if (!hboxAppType.equals("MPI")) {
      this.hboxCmdProcessId = getPidOfProcess(hboxProcess);
      LOG.info("hboxCmdProcessId is:" + this.hboxCmdProcessId);
      containerReporter = new ContainerReporter(amClient, conf, containerId, cudaEnv, this.hboxCmdProcessId, containerType.equals("DOCKER"));
      containerReporter.setDaemon(true);
      containerReporter.start();
    }
    if (hboxAppType.equals("MPI") || hboxAppType.equals("HOROVOD")) {
      int updateAppStatusRetry = this.conf.getInt(HboxConfiguration.HBOX_MPI_CONTAINER_UPDATE_APP_STATUS_RETRY,
          HboxConfiguration.DEFAULT_HBOX_MPI_CONTAINER_UPDATE_APP_STATUS_RETRY);
      boolean isAppFinished = false;
      while (true) {
        int retry = 0;
        while (true) {
          try {
            isAppFinished = this.amClient.isApplicationCompleted();
            break;
          } catch (Exception e) {
            retry++;
            if (retry < updateAppStatusRetry) {
              LOG.info("Getting application status failed in retry " + retry);
              Utilities.sleep(updateAppStatusInterval);
            } else {
              LOG.info("Getting application status failed in retry " + retry
                  + ", container will suicide!", e);
              return false;
            }
          }
        }
        if (isAppFinished) {
          this.uploadOutputFiles();
          return true;
        }
        Utilities.sleep(updateAppStatusInterval);
      }
    } else {
      int code = -1;
      while (code == -1 && !heartbeatThread.isHboxTrainCompleted()) {
        Utilities.sleep(updateAppStatusInterval);
        try {
          code = hboxProcess.exitValue();
          LOG.info("code is: " + code);
          //LOG.info("heartbeatThread.isHboxTrainCompleted() is: " + heartbeatThread.isHboxTrainCompleted());
        } catch (IllegalThreadStateException e) {
          LOG.debug("Hbox Process is running");
          this.signalID = amClient.getSignal();
          if (this.signalID >= 0) {
            rt.exec("kill -" + this.signalID + " " + this.hboxCmdProcessId);
            LOG.info("Send the signal " + this.signalID + " to process " + this.hboxCmdProcessId);
            amClient.sendSignal(-1);
          }
        }
      }

      if (this.role.equals(HboxConstants.PS) && !this.hboxAppType.equals("DISTLIGHTLDA")) {
        if (code == -1) {
          this.uploadOutputFiles();
          hboxProcess.destroy();
          return true;
        } else if (code == 0) {
          this.uploadOutputFiles();
          return true;
        }
        return false;
      }

      if (this.role.equals("server")) {
        if (code == -1) {
          hboxProcess.destroy();
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
  }

  private void reportFailedAndExit() {
    Date now = new Date();
    heartbeatThread.setContainersFinishTime(now.toString());
    heartbeatThread.setContainerStatus(HboxContainerStatus.FAILED);
    Utilities.sleep(heartbeatInterval);
    System.exit(-1);
  }

  private void reportSucceededAndExit() {
    Date now = new Date();
    heartbeatThread.setContainersFinishTime(now.toString());
    heartbeatThread.setContainerStatus(HboxContainerStatus.SUCCEEDED);
    Utilities.sleep(heartbeatInterval);
    System.exit(0);
  }

  public static void main(String[] args) {
    HboxContainer container = new HboxContainer();
    try {
      container.init();
      if (container.run()) {
        LOG.info("HboxContainer " + container.getContainerId().toString() + " finish successfully");
        container.reportSucceededAndExit();
      } else {
        LOG.error("HboxContainer run failed!");
        container.reportFailedAndExit();
      }
    } catch (Exception e) {
      LOG.error("Some errors has occurred during container running!", e);
      container.reportFailedAndExit();
    }
  }
}
