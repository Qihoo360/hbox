package net.qihoo.hbox.client;

import net.qihoo.hbox.api.ApplicationMessageProtocol;
import net.qihoo.hbox.api.HboxConstants;
import net.qihoo.hbox.common.LogType;
import net.qihoo.hbox.common.Message;
import net.qihoo.hbox.common.exceptions.RequestOverLimitException;
import net.qihoo.hbox.conf.HboxConfiguration;
import net.qihoo.hbox.util.Utilities;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.ipc.RPC;
import org.apache.hadoop.mapred.InputFormat;
import org.apache.hadoop.mapred.OutputFormat;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.hadoop.yarn.api.ApplicationConstants;
import org.apache.hadoop.yarn.api.protocolrecords.GetNewApplicationResponse;
import org.apache.hadoop.yarn.api.records.*;
import org.apache.hadoop.yarn.client.api.YarnClient;
import org.apache.hadoop.yarn.client.api.YarnClientApplication;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.apache.hadoop.yarn.exceptions.YarnException;
import org.apache.hadoop.yarn.util.Records;

import java.io.IOException;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

public class Client {

    private static final Log LOG = LogFactory.getLog(Client.class);
    private ClientArguments clientArguments;
    private final HboxConfiguration conf;
    private YarnClient yarnClient;
    private YarnClientApplication newAPP;
    private ApplicationMessageProtocol hboxClient;
    private transient AtomicBoolean isRunning;
    private StringBuffer appFilesRemotePath;
    private StringBuffer appLibJarsRemotePath;
    private ApplicationId applicationId;
    private int maxContainerMem;
    private FileSystem dfs;
    private final ConcurrentHashMap<String, String> inputPaths;
    private final ConcurrentHashMap<String, String> s3InputPaths;
    private final ConcurrentHashMap<String, String> outputPaths;
    private final ConcurrentHashMap<String, String> s3OutputPaths;
    private static FsPermission JOB_FILE_PERMISSION;

    private final Map<String, String> appMasterUserEnv;
    private final Map<String, String> containerUserEnv;

    private Boolean boardUpload;

    private Client(String[] args) throws IOException, ParseException, ClassNotFoundException {
        this.conf = new HboxConfiguration();
        this.clientArguments = new ClientArguments(args);
        this.isRunning = new AtomicBoolean(false);
        this.appFilesRemotePath = new StringBuffer(1000);
        this.appLibJarsRemotePath = new StringBuffer(1000);
        this.inputPaths = new ConcurrentHashMap<>();
        this.s3InputPaths = new ConcurrentHashMap<>();
        this.outputPaths = new ConcurrentHashMap<>();
        this.s3OutputPaths = new ConcurrentHashMap<>();
        JOB_FILE_PERMISSION = FsPermission.createImmutable((short) 0644);
        this.appMasterUserEnv = new HashMap<>();
        this.containerUserEnv = new HashMap<>();
        this.boardUpload = true;
    }

    private void init() throws IOException, YarnException {
        String appSubmitterUserName = System.getenv(ApplicationConstants.Environment.USER.name());
        if (conf.get("hadoop.job.ugi") == null) {
            UserGroupInformation ugi = UserGroupInformation.createRemoteUser(appSubmitterUserName);
            conf.set("hadoop.job.ugi", ugi.getUserName() + "," + ugi.getUserName());
        }

        conf.set(HboxConfiguration.HBOX_CLUSTER_NAME, String.valueOf(clientArguments.clusterName));
        conf.set(HboxConfiguration.HBOX_DRIVER_MEMORY, String.valueOf(clientArguments.driverMem));
        conf.set(HboxConfiguration.HBOX_DRIVER_CORES, String.valueOf(clientArguments.driverCores));
        conf.set(HboxConfiguration.HBOX_WORKER_MEMORY, String.valueOf(clientArguments.workerMemory));
        conf.set(HboxConfiguration.HBOX_WORKER_VCORES, String.valueOf(clientArguments.workerVCores));
        conf.set(HboxConfiguration.HBOX_WORKER_GPU, String.valueOf(clientArguments.workerGCores));
        conf.set(HboxConfiguration.HBOX_WORKER_NUM, String.valueOf(clientArguments.workerNum));
        conf.set(HboxConfiguration.HBOX_CHIEF_WORKER_MEMORY, String.valueOf(clientArguments.chiefWorkerMemory));
        conf.set(HboxConfiguration.HBOX_EVALUATOR_WORKER_MEMORY, String.valueOf(clientArguments.evaluatorWorkerMemory));
        conf.set(HboxConfiguration.HBOX_VPC_DURATION, clientArguments.duration);
        conf.set(HboxConfiguration.HBOX_PS_MEMORY, String.valueOf(clientArguments.psMemory));
        conf.set(HboxConfiguration.HBOX_PS_VCORES, String.valueOf(clientArguments.psVCores));
        conf.set(HboxConfiguration.HBOX_PS_GPU, String.valueOf(clientArguments.psGCores));
        conf.set(HboxConfiguration.HBOX_PS_NUM, String.valueOf(clientArguments.psNum));
        conf.set(HboxConfiguration.HBOX_APP_PRIORITY, String.valueOf(clientArguments.priority));
        conf.set(HboxConfiguration.HBOX_TF_BOARD_WORKER_INDEX, String.valueOf(clientArguments.boardIndex));
        conf.set(HboxConfiguration.HBOX_TF_BOARD_RELOAD_INTERVAL, String.valueOf(clientArguments.boardReloadInterval));
        conf.set(HboxConfiguration.HBOX_TF_BOARD_ENABLE, String.valueOf(clientArguments.boardEnable));
        conf.set(HboxConfiguration.HBOX_TF_BOARD_LOG_DIR, clientArguments.boardLogDir);
        conf.set(HboxConfiguration.HBOX_TF_BOARD_HISTORY_DIR, clientArguments.boardHistoryDir);
        conf.set(HboxConfiguration.HBOX_BOARD_MODELPB, clientArguments.boardModelPB);
        conf.set(HboxConfiguration.HBOX_BOARD_CACHE_TIMEOUT, String.valueOf(clientArguments.boardCacheTimeout));
        conf.set(HboxConfiguration.HBOX_INPUT_STRATEGY, clientArguments.inputStrategy);
        conf.set(HboxConfiguration.HBOX_OUTPUT_STRATEGY, clientArguments.outputStrategy);
        conf.setBoolean(HboxConfiguration.HBOX_INPUTFILE_RENAME, clientArguments.isRenameInputFile);
        conf.setBoolean(HboxConfiguration.HBOX_USER_CLASSPATH_FIRST, clientArguments.userClasspathFirst);
        conf.setBoolean(HboxConfiguration.HBOX_HOST_LOCAL_ENABLE, clientArguments.hostLocalEnable);
        conf.setBoolean(HboxConfiguration.HBOX_CREATE_CONTAINERID_DIR, clientArguments.createContaineridDir);
        conf.setBoolean(HboxConfiguration.HBOX_INPUT_STREAM, clientArguments.inputStream);
        conf.setBoolean(HboxConfiguration.HBOX_OUTPUT_STREAM, clientArguments.outputStream);
        conf.setBoolean(HboxConfiguration.HBOX_INPUT_STREAM_SHUFFLE, clientArguments.inputStreamShuffle);
        conf.setClass(HboxConfiguration.HBOX_INPUTF0RMAT_CLASS, clientArguments.inputFormatClass, InputFormat.class);
        conf.setClass(HboxConfiguration.HBOX_OUTPUTFORMAT_CLASS, clientArguments.outputFormatClass, OutputFormat.class);
        conf.set(HboxConfiguration.HBOX_STREAM_EPOCH, String.valueOf(clientArguments.streamEpoch));
        conf.setBoolean(HboxConfiguration.HBOX_TF_EVALUATOR, clientArguments.tfEvaluator);


        if (clientArguments.queue == null || clientArguments.queue.equals("")) {
            clientArguments.queue = appSubmitterUserName;
        }
        conf.set(HboxConfiguration.HBOX_APP_QUEUE, clientArguments.queue);

        if (clientArguments.confs != null) {
            setConf();
            if (containerUserEnv.size() > 0) {
                StringBuilder userEnv = new StringBuilder();
                for (String key : containerUserEnv.keySet()) {
                    userEnv.append(key);
                    userEnv.append("=");
                    userEnv.append(containerUserEnv.get(key));
                    userEnv.append("|");
                }
                conf.set(HboxConfiguration.HBOX_CONTAINER_ENV, userEnv.deleteCharAt(userEnv.length() - 1).toString());
            }
        }
        readClusterConf();

        if ("TENSORFLOW".equals(clientArguments.appType) || "TENSOR2TENSOR".equals(clientArguments.appType)) {
            if (conf.getBoolean(HboxConfiguration.HBOX_TF_DISTRIBUTION_STRATEGY, HboxConfiguration.DEFAULT_HBOX_TF_DISTRIBUTION_STRATEGY)) {
                if ((conf.getInt(HboxConfiguration.HBOX_PS_NUM, HboxConfiguration.DEFAULT_HBOX_PS_NUM) + conf.getInt(HboxConfiguration.HBOX_WORKER_NUM, HboxConfiguration.DEFAULT_HBOX_WORKER_NUM)) == 1) {
                    conf.setBoolean(HboxConfiguration.HBOX_TF_MODE_SINGLE, true);
                }
            } else {
                if (conf.getInt(HboxConfiguration.HBOX_PS_NUM, HboxConfiguration.DEFAULT_HBOX_PS_NUM) == 0) {
                    conf.setBoolean(HboxConfiguration.HBOX_TF_MODE_SINGLE, true);
                }
            }
            if (conf.getBoolean(HboxConfiguration.HBOX_TF_EVALUATOR, HboxConfiguration.DEFAULT_HBOX_TF_EVALUATOR)) {
                if ((!conf.getBoolean(HboxConfiguration.HBOX_TF_MODE_SINGLE, HboxConfiguration.DEFAULT_HBOX_TF_MODE_SINGLE)) && conf.getInt(HboxConfiguration.HBOX_WORKER_NUM, HboxConfiguration.DEFAULT_HBOX_WORKER_NUM) > 1) {

                } else {
                    conf.setBoolean(HboxConfiguration.HBOX_TF_EVALUATOR, false);
                }
            }
        }

        if ("MXNET".equals(clientArguments.appType)) {
            if (conf.getInt(HboxConfiguration.HBOX_PS_NUM, HboxConfiguration.DEFAULT_HBOX_PS_NUM) == 0) {
                conf.setBoolean(HboxConfiguration.HBOX_MXNET_MODE_SINGLE, true);
            }
        }

        if ("XDL".equals(clientArguments.appType)) {
            int psNum = conf.getInt(HboxConfiguration.HBOX_PS_NUM, HboxConfiguration.DEFAULT_HBOX_PS_NUM);
            if (psNum == 0) {
                conf.setBoolean(HboxConfiguration.HBOX_TF_MODE_SINGLE, true);
            } else {
                conf.setInt(HboxConfiguration.HBOX_PS_NUM, psNum + 1);
            }
        }

        if (!("VPC".equals(clientArguments.appType) || "DIGITS".equals(clientArguments.appType))) {
            if (conf.getInt(HboxConfiguration.HBOX_WORKER_NUM, HboxConfiguration.DEFAULT_HBOX_WORKER_NUM) == 1) {
                conf.setInt(HboxConfiguration.HBOX_TF_BOARD_WORKER_INDEX, 0);
            }

            if (conf.get(HboxConfiguration.HBOX_TF_BOARD_LOG_DIR, HboxConfiguration.DEFAULT_HBOX_TF_BOARD_LOG_DIR).indexOf("/") == 0) {
                Path tf_board_log_dir = new Path(conf.get("fs.defaultFS"), conf.get(HboxConfiguration.HBOX_TF_BOARD_LOG_DIR));
                conf.set(HboxConfiguration.HBOX_TF_BOARD_LOG_DIR, tf_board_log_dir.toString());
            }
            if ((conf.get(HboxConfiguration.HBOX_TF_BOARD_LOG_DIR).indexOf("hdfs") == 0) && (!("TENSORFLOW".equals(clientArguments.appType) || "TENSOR2TENSOR".equals(clientArguments.appType)))) {
                LOG.warn("VisualDL not support the hdfs path for logdir. Please ensure the logdir setting is right.");
            }
        }

        yarnClient = YarnClient.createYarnClient();
        yarnClient.init(conf);
        yarnClient.start();
        LOG.info("Requesting a new application from cluster with " + yarnClient.getYarnClusterMetrics().getNumNodeManagers() + " NodeManagers");
        newAPP = yarnClient.createApplication();
    }

    private static void showWelcome() {
        System.err.println("Welcome to\n " +
                "\t _  _   ___\n" +
                "\t| || | | _ )  ___  __ __\n" +
                "\t| __ | | _ \\ / _ \\ \\ \\ /\n" +
                "\t|_||_| |___/ \\___/ /_\\_\\\n");
    }

    private void setConf() {
        Enumeration<String> confSet = (Enumeration<String>) clientArguments.confs.propertyNames();
        while (confSet.hasMoreElements()) {
            String confArg = confSet.nextElement();
            if (confArg.startsWith(HboxConstants.AM_ENV_PREFIX)) {
                String key = confArg.substring(HboxConstants.AM_ENV_PREFIX.length()).trim();
                Utilities.addPathToEnvironment(appMasterUserEnv, key, clientArguments.confs.getProperty(confArg).trim());
            } else if (confArg.startsWith(HboxConstants.CONTAINER_ENV_PREFIX)) {
                String key = confArg.substring(HboxConstants.CONTAINER_ENV_PREFIX.length()).trim();
                Utilities.addPathToEnvironment(containerUserEnv, key, clientArguments.confs.getProperty(confArg).trim());
            } else {
                conf.set(confArg, clientArguments.confs.getProperty(confArg));
            }
        }
    }

    private void readClusterConf() {
        String cluster = conf.get(HboxConfiguration.HBOX_CLUSTER_NAME, HboxConfiguration.DEFAULT_HBOX_CLUSTER_NAME);
        if (!cluster.equals("")) {
            String[] clusterConfPath = conf.getStrings(HboxConfiguration.HBOX_CLUSTER_CONF_PATH.replace("cluster.name", cluster));
            if (clusterConfPath == null || clusterConfPath.length == 0) {
                LOG.warn("Note that not set the cluster yarn-site.xml path. Current application is submitted to default cluster");
            } else {
                for (String confPath : clusterConfPath) {
                    LOG.info("update the conf : " + confPath);
                    conf.addResource(new Path(confPath));
                }
                LOG.info("cluster yarn-site.xml has updated! ");
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void addOutputPath(String type, Properties outputProperty) throws IOException {
        ConcurrentHashMap<String, String> outputMap;
        if (type.equals(HboxConstants.S3))
            outputMap = this.s3OutputPaths;
        else
            outputMap = this.outputPaths;
        Enumeration<String> outputs = (Enumeration<String>) outputProperty.propertyNames();
        while (outputs.hasMoreElements()) {
            String outputRemote = outputs.nextElement();
            String outputLocal = outputProperty.getProperty(outputRemote);
            if (outputLocal.equals("true")) {
                outputLocal = conf.get(HboxConfiguration.HBOX_OUTPUT_LOCAL_DIR, HboxConfiguration.DEFAULT_HBOX_OUTPUT_LOCAL_DIR);
                LOG.info("Remote output path: " + outputRemote + " not defined the local output path. Default path: output.");
            }
            if (type.equals(HboxConstants.HDFS)) {
                Path path = new Path(outputRemote);
                if (path.getFileSystem(conf).exists(path)) {
                    throw new IOException("Output path " + path + " already existed!");
                }
            }
            if (outputMap.containsKey(outputLocal)) {
                outputMap.put(outputLocal, outputMap.get(outputLocal) + "," + outputRemote);
            } else {
                outputMap.put(outputLocal, outputRemote);
            }
            LOG.info("Local output path: " + outputLocal + " and remote output path: " + outputRemote);
        }
    }

    private void assignOutput() throws IOException {
        if (clientArguments.outputs != null) {
            addOutputPath(HboxConstants.HDFS, clientArguments.outputs);
        }
        if (clientArguments.s3Outputs != null) {
            addOutputPath(HboxConstants.S3, clientArguments.s3Outputs);
        }
        if (conf.getBoolean(HboxConfiguration.HBOX_OUTPUT_STREAM, HboxConfiguration.DEFAULT_HBOX_OUTPUT_STREAM)
                || conf.get(HboxConfiguration.HBOX_OUTPUT_STRATEGY, HboxConfiguration.DEFAULT_HBOX_OUTPUT_STRATEGY).equals("STREAM")) {
            boardUpload = false;
        } else {
            if (outputPaths.size() > 0) {
                String boardLogDir = conf.get(HboxConfiguration.HBOX_TF_BOARD_LOG_DIR, HboxConfiguration.DEFAULT_HBOX_TF_BOARD_LOG_DIR);
                String outputRefBoardDir = null;
                if (boardUpload && !boardLogDir.contains("hdfs://")) {
                    String replaceBoardLogDir = "/" + boardLogDir.replaceAll("\\.", " ").trim().replaceAll(" ", ".").replaceAll("/", " ").trim().replaceAll(" ", "/") + "/";
                    for (String outputInfo : outputPaths.keySet()) {
                        String replaceOutputDir = "/" + outputInfo.replaceAll("\\.", " ").trim().replaceAll(" ", ".").replaceAll("/", " ").trim().replaceAll(" ", "/") + "/";
                        if (replaceBoardLogDir.indexOf(replaceOutputDir) == 0) {
                            boardUpload = false;
                            outputRefBoardDir = outputPaths.get(outputInfo).split(",")[0];
                            break;
                        }
                    }
                    if (!boardUpload) {
                        if (conf.get(HboxConfiguration.HBOX_TF_BOARD_HISTORY_DIR, HboxConfiguration.DEFAULT_HBOX_TF_BOARD_HISTORY_DIR).equals(new HboxConfiguration().get(HboxConfiguration.HBOX_TF_BOARD_HISTORY_DIR, HboxConfiguration.DEFAULT_HBOX_TF_BOARD_HISTORY_DIR))) {
                            LOG.info("Set the Board History: " + outputRefBoardDir);
                            conf.set(HboxConfiguration.HBOX_TF_BOARD_HISTORY_DIR, outputRefBoardDir);
                        } else {
                            boardUpload = false;
                        }
                    }
                }
            }
        }
        conf.setBoolean(HboxConfiguration.HBOX_TF_BOARD_UPLOAD, boardUpload);
    }

    @SuppressWarnings("unchecked")
    private void addInputPath(String type, Properties inputProperty) throws IOException {
        ConcurrentHashMap<String, String> inputMap;
        if (type.equals(HboxConstants.S3))
            inputMap = this.s3InputPaths;
        else
            inputMap = this.inputPaths;
        Enumeration<String> inputs = (Enumeration<String>) inputProperty.propertyNames();
        while (inputs.hasMoreElements()) {
            String inputRemote = inputs.nextElement();
            String inputLocal = clientArguments.inputs.getProperty(inputRemote);
            if (inputLocal.equals("true")) {
                inputLocal = "input";
            }
            if (type.equals(HboxConstants.HDFS)) {
                for (String pathdir : StringUtils.split(inputRemote, ",")) {
                    Path path = new Path(pathdir);
                    FileSystem fs = path.getFileSystem(conf);
                    FileStatus[] pathStatus = fs.globStatus(path);
                    if (pathStatus == null || pathStatus.length <= 0) {
                        throw new IOException("Input path " + path + " not existed!");
                    }
                    fs.close();
                }
            }
            if (inputMap.containsKey(inputLocal)) {
                inputMap.put(inputLocal, inputMap.get(inputLocal) + "," + inputRemote);
            } else {
                inputMap.put(inputLocal, inputRemote);
            }
            LOG.info("Local input path: " + inputLocal + " and remote input path: " + inputRemote);
        }
    }

    @SuppressWarnings("unchecked")
    private void assignInput() throws IOException {
        if (clientArguments.inputs != null) {
            addInputPath(HboxConstants.HDFS, clientArguments.inputs);
        }
        if (clientArguments.s3Inputs != null) {
            addInputPath(HboxConstants.S3, clientArguments.s3Inputs);
        }
    }

    private static ApplicationReport getApplicationReport(ApplicationId appId, YarnClient yarnClient)
            throws YarnException, IOException {
        return yarnClient.getApplicationReport(appId);
    }

    private static ApplicationMessageProtocol getAppMessageHandler(
            YarnConfiguration conf, String appMasterAddress, int appMasterPort) throws IOException {
        ApplicationMessageProtocol appMessageHandler = null;
        if (!StringUtils.isBlank(appMasterAddress) && !appMasterAddress.equalsIgnoreCase("N/A")) {
            InetSocketAddress addr = new InetSocketAddress(appMasterAddress, appMasterPort);
            appMessageHandler = RPC.getProxy(ApplicationMessageProtocol.class, ApplicationMessageProtocol.versionID, addr, conf);
        }
        return appMessageHandler;
    }

    private void checkArguments(HboxConfiguration conf, GetNewApplicationResponse newApplication) {
        int maxMem = newApplication.getMaximumResourceCapability().getMemory();
        LOG.info("Max mem capability of resources in this cluster " + maxMem);
        int maxVCores = newApplication.getMaximumResourceCapability().getVirtualCores();
        LOG.info("Max vcores capability of resources in this cluster " + maxVCores);
        int maxGCores = newApplication.getMaximumResourceCapability().getGpuCores();
        LOG.info("Max gpu cores capability of resources in this cluster " + maxGCores);

        int driverMem = conf.getInt(HboxConfiguration.HBOX_DRIVER_MEMORY, HboxConfiguration.DEFAULT_HBOX_DRIVER_MEMORY);
        int driverCores = conf.getInt(HboxConfiguration.HBOX_DRIVER_CORES, HboxConfiguration.DEFAULT_HBOX_DRIVER_CORES);
        if (driverMem > maxMem) {
            throw new RequestOverLimitException("AM memory requested " + driverMem +
                    " above the max threshold of yarn cluster " + maxMem);
        }
        if (driverMem <= 0) {
            throw new IllegalArgumentException(
                    "Invalid memory specified for application master, exiting."
                            + " Specified memory=" + driverMem);
        }
        LOG.info("Apply for am Memory " + driverMem + "M");
        if (driverCores > maxVCores) {
            throw new RequestOverLimitException("driver vcores requested " + driverCores +
                    " above the max threshold of yarn cluster " + maxVCores);
        }
        if (driverCores <= 0) {
            throw new IllegalArgumentException(
                    "Invalid vcores specified for driver, exiting."
                            + "Specified vcores=" + driverCores);
        }
        LOG.info("Apply for driver vcores " + driverCores);

        int workerNum = conf.getInt(HboxConfiguration.HBOX_WORKER_NUM, HboxConfiguration.DEFAULT_HBOX_WORKER_NUM);
        int workerMemory = conf.getInt(HboxConfiguration.HBOX_WORKER_MEMORY, HboxConfiguration.DEFAULT_HBOX_WORKER_MEMORY);
        int workerVcores = conf.getInt(HboxConfiguration.HBOX_WORKER_VCORES, HboxConfiguration.DEFAULT_HBOX_WORKER_VCORES);
        int workerGcores = conf.getInt(HboxConfiguration.HBOX_WORKER_GPU, HboxConfiguration.DEFAULT_HBOX_WORKER_GPU);
        if (workerNum < 1) {
            throw new IllegalArgumentException(
                    "Invalid no. of worker specified, exiting."
                            + " Specified container number=" + workerNum);
        }
        LOG.info("Apply for worker number " + workerNum);
        if (workerMemory > maxMem) {
            throw new RequestOverLimitException("Worker memory requested " + workerMemory +
                    " above the max threshold of yarn cluster " + maxMem);
        }
        if (workerMemory <= 0) {
            throw new IllegalArgumentException(
                    "Invalid memory specified for worker, exiting."
                            + "Specified memory=" + workerMemory);
        }
        LOG.info("Apply for worker Memory " + workerMemory + "M");

        int chiefWorkerMemory = conf.getInt(HboxConfiguration.HBOX_CHIEF_WORKER_MEMORY, HboxConfiguration.DEFAULT_HBOX_WORKER_MEMORY);
        if (chiefWorkerMemory != workerMemory) {
            if (chiefWorkerMemory > maxMem) {
                throw new RequestOverLimitException("CHIEF Worker memory requested " + chiefWorkerMemory +
                        " above the max threshold of yarn cluster " + maxMem);
            }
            if (chiefWorkerMemory <= 0) {
                throw new IllegalArgumentException(
                        "Invalid memory specified for chief worker, exiting."
                                + "Specified memory=" + chiefWorkerMemory);
            }
            LOG.info("Apply for chief worker Memory " + chiefWorkerMemory + "M");
        }

        int evaluatorWorkerMemory = conf.getInt(HboxConfiguration.HBOX_EVALUATOR_WORKER_MEMORY, HboxConfiguration.DEFAULT_HBOX_WORKER_MEMORY);
        if (evaluatorWorkerMemory != workerMemory && conf.getBoolean(HboxConfiguration.HBOX_TF_EVALUATOR, HboxConfiguration.DEFAULT_HBOX_TF_EVALUATOR)) {
            if (evaluatorWorkerMemory > maxMem) {
                throw new RequestOverLimitException("Evaluator Worker memory requested " + evaluatorWorkerMemory +
                        " above the max threshold of yarn cluster " + maxMem);
            }
            if (evaluatorWorkerMemory <= 0) {
                throw new IllegalArgumentException(
                        "Invalid memory specified for evaluator worker, exiting."
                                + "Specified memory=" + evaluatorWorkerMemory);
            }
            LOG.info("Apply for evaluator worker Memory " + evaluatorWorkerMemory + "M");
        }

        if (workerVcores > maxVCores) {
            throw new RequestOverLimitException("Worker vcores requested " + workerVcores +
                    " above the max threshold of yarn cluster " + maxVCores);
        }
        if (workerVcores <= 0) {
            throw new IllegalArgumentException(
                    "Invalid vcores specified for worker, exiting."
                            + "Specified vcores=" + workerVcores);
        }
        LOG.info("Apply for worker vcores " + workerVcores);
        if (workerGcores > maxGCores) {
            throw new RequestOverLimitException("Worker gpu cores requested " + workerGcores +
                    " above the max threshold of yarn cluster " + maxGCores);
        }
        if (workerGcores < 0) {
            throw new IllegalArgumentException(
                    "Invalid gpu cores specified for worker, exiting."
                            + "Specified gpu cores=" + workerGcores);
        }
        LOG.info("Apply for worker gpu cores " + workerGcores);

        if (clientArguments.appType.equals("VPC") || clientArguments.appType.equals("DIGITS")) {
            String durationStr = conf.get(HboxConfiguration.HBOX_VPC_DURATION, HboxConfiguration.DEFAULT_HBOX_VPC_DURATION);
            String pattern = "^\\d+(\\.\\d+)?([smhd])?";
            boolean isMatch = Pattern.matches(pattern, durationStr);
            if (!isMatch) {
                throw new IllegalArgumentException(
                        "Invalid no. of debug duration specified, exiting."
                                + " Specified debug duration=" + durationStr);
            }
        }

        if ("TENSORFLOW".equals(clientArguments.appType) || "TENSOR2TENSOR".equals(clientArguments.appType) || "MXNET".equals(clientArguments.appType) || "DISTLIGHTLDA".equals(clientArguments.appType) || "XFLOW".equals(clientArguments.appType) || "XDL".equals(clientArguments.appType)) {
            Boolean single;
            if ("TENSORFLOW".equals(clientArguments.appType) || "TENSOR2TENSOR".equals(clientArguments.appType)) {
                single = conf.getBoolean(HboxConfiguration.HBOX_TF_MODE_SINGLE, HboxConfiguration.DEFAULT_HBOX_TF_MODE_SINGLE);
            } else {
                single = conf.getBoolean(HboxConfiguration.HBOX_MXNET_MODE_SINGLE, HboxConfiguration.DEFAULT_HBOX_MXNET_MODE_SINGLE);
            }
            if ("DISTLIGHTLDA".equals(clientArguments.appType)) {
                single = false;
            }
            if ("XFLOW".equals(clientArguments.appType)) {
                single = false;
            }
            int psNum = conf.getInt(HboxConfiguration.HBOX_PS_NUM, HboxConfiguration.DEFAULT_HBOX_PS_NUM);
            if (psNum < 0) {
                throw new IllegalArgumentException(
                        "Invalid no. of ps specified, exiting."
                                + " Specified container number=" + psNum);
            }
            LOG.info("Apply for ps number " + psNum);
            if (!single) {
                if (!conf.getBoolean(HboxConfiguration.HBOX_TF_DISTRIBUTION_STRATEGY, HboxConfiguration.DEFAULT_HBOX_TF_DISTRIBUTION_STRATEGY)) {
                    if (psNum < 1) {
                        throw new IllegalArgumentException(
                                "Invalid no. of ps specified for distributed job, exiting."
                                        + " Specified container number=" + psNum);
                    }
                    int psMemory = conf.getInt(HboxConfiguration.HBOX_PS_MEMORY, HboxConfiguration.DEFAULT_HBOX_PS_MEMORY);
                    int psVcores = conf.getInt(HboxConfiguration.HBOX_PS_VCORES, HboxConfiguration.DEFAULT_HBOX_PS_VCORES);
                    int psGcores = conf.getInt(HboxConfiguration.HBOX_PS_GPU, HboxConfiguration.DEFAULT_HBOX_PS_GPU);
                    if (psMemory > maxMem) {
                        throw new RequestOverLimitException("ps memory requested " + psMemory +
                                " above the max threshold of yarn cluster " + maxMem);
                    }
                    if (psMemory <= 0) {
                        throw new IllegalArgumentException(
                                "Invalid memory specified for ps, exiting."
                                        + "Specified memory=" + psMemory);
                    }
                    LOG.info("Apply for ps Memory " + psMemory + "M");
                    if (psVcores > maxVCores) {
                        throw new RequestOverLimitException("ps vcores requested " + psVcores +
                                " above the max threshold of yarn cluster " + maxVCores);
                    }
                    if (psVcores <= 0) {
                        throw new IllegalArgumentException(
                                "Invalid vcores specified for ps, exiting."
                                        + "Specified vcores=" + psVcores);
                    }
                    LOG.info("Apply for ps vcores " + psVcores);
                    if (psGcores > maxGCores) {
                        throw new RequestOverLimitException("ps gpu cores requested " + psGcores +
                                " above the max threshold of yarn cluster " + maxGCores);
                    }
                    if (psGcores < 0) {
                        throw new IllegalArgumentException(
                                "Invalid gpu cores specified for ps, exiting."
                                        + "Specified gpu cores=" + psGcores);
                    }
                    LOG.info("Apply for ps gpu cores " + psGcores);
                }
            }
            int limitNode = conf.getInt(HboxConfiguration.HBOX_EXECUTE_NODE_LIMIT, HboxConfiguration.DEFAULT_HBOX_EXECUTENODE_LIMIT);
            if (workerNum + psNum > limitNode) {
                throw new RequestOverLimitException("Container num requested over the limit " + limitNode);
            }
        }
    }

    private void configXDLAmContainer() {
        conf.set(HboxConfiguration.HBOX_CONTAINER_TYPE, "docker");
        conf.set("DOCKER_CONTAINER_NETWORK", "host");
        conf.set("DOCKER_PORT", "");
        conf.set("RESERVED_PORT", "");
        String imageName = conf.get(HboxConfiguration.HBOX_DOCKER_IMAGE_NAME,
                HboxConfiguration.DEFAULT_HBOX_DOCKER_IMAGE_NAME);
        LOG.info("Docker image name is:" + imageName);
        if (imageName == null || imageName.equals("")) {
            LOG.info("Docker image name is empty");
            conf.set(HboxConfiguration.HBOX_CONTAINER_TYPE, HboxConfiguration.DEFAULT_HBOX_CONTAINER_TYPE);
        }
    }

    private void assignCacheFiles() throws IOException {
        String[] cacheFiles = StringUtils.split(clientArguments.hboxCacheFiles, ",");
        for (String path : cacheFiles) {
            Path pathRemote;
            if (path.contains("#")) {
                String[] paths = StringUtils.split(path, "#");
                if (paths.length != 2) {
                    throw new RuntimeException("Error cacheFile path format " + path);
                }
                pathRemote = new Path(paths[0]);
            } else {
                pathRemote = new Path(path);
            }
            if (Boolean.parseBoolean(conf.get(HboxConfiguration.HBOX_CACHEFILE_CHECK_ENABLE, String.valueOf(HboxConfiguration.DEFAULT_HBOX_CACHEFILE_CHECK_ENABLE)))) {
                if (!pathRemote.getFileSystem(conf).exists(pathRemote)) {
                    throw new IOException("cacheFile path " + pathRemote + " not existed!");
                }
            }
        }
    }

    private void assignCacheArchives() throws IOException {
        String[] cacheArchives = StringUtils.split(clientArguments.hboxCacheArchives, ",");
        for (String path : cacheArchives) {
            Path pathRemote;
            if (path.contains("#")) {
                String[] paths = StringUtils.split(path, "#");
                if (paths.length != 2) {
                    throw new RuntimeException("Error cacheArchives path format " + path);
                }
                pathRemote = new Path(paths[0]);
            } else {
                pathRemote = new Path(path);
            }
            if (Boolean.parseBoolean(conf.get(HboxConfiguration.HBOX_CACHEFILE_CHECK_ENABLE, String.valueOf(HboxConfiguration.DEFAULT_HBOX_CACHEFILE_CHECK_ENABLE)))) {
                if (!pathRemote.getFileSystem(conf).exists(pathRemote)) {
                    throw new IOException("cacheArchive path " + pathRemote + " not existed!");
                }
            }
        }
    }

    private void prepareFilesForAM(Map<String, String> appMasterEnv, Map<String, LocalResource> localResources) throws IOException {
        Path[] tfFilesDst = new Path[clientArguments.hboxFiles.length];
        LOG.info("Copy hbox files from local filesystem to remote.");
        for (int i = 0; i < clientArguments.hboxFiles.length; i++) {
            assert (!clientArguments.hboxFiles[i].isEmpty());
            Path tfFilesSrc = new Path(clientArguments.hboxFiles[i]);
            tfFilesDst[i] = Utilities.getRemotePath(
                    conf, applicationId, new Path(clientArguments.hboxFiles[i]).getName());
            LOG.info("Copying " + clientArguments.hboxFiles[i] + " to remote path " + tfFilesDst[i].toString());
            dfs.copyFromLocalFile(false, true, tfFilesSrc, tfFilesDst[i]);
            appFilesRemotePath.append(tfFilesDst[i].toUri().toString()).append(",");
        }
        appMasterEnv.put(HboxConstants.Environment.HBOX_FILES_LOCATION.toString(),
                appFilesRemotePath.deleteCharAt(appFilesRemotePath.length() - 1).toString());

        if ((clientArguments.appType.equals("MXNET") && !conf.getBoolean(HboxConfiguration.HBOX_MXNET_MODE_SINGLE, HboxConfiguration.DEFAULT_HBOX_MXNET_MODE_SINGLE))
                || clientArguments.appType.equals("XFLOW")
                || (clientArguments.appType.equals("XDL") && !conf.getBoolean(HboxConfiguration.HBOX_TF_MODE_SINGLE, HboxConfiguration.DEFAULT_HBOX_TF_MODE_SINGLE))) {
            String appFilesRemoteLocation = appMasterEnv.get(HboxConstants.Environment.HBOX_FILES_LOCATION.toString());
            String[] tfFiles = StringUtils.split(appFilesRemoteLocation, ",");
            for (String file : tfFiles) {
                Path path = new Path(file);
                localResources.put(path.getName(),
                        Utilities.createApplicationResource(path.getFileSystem(conf),
                                path,
                                LocalResourceType.FILE));
            }
        }
    }

    private void prepareJarsForAM(Map<String, String> appMasterEnv, Map<String, LocalResource> localResources) throws IOException {
        Path[] tfFilesDst = new Path[clientArguments.libJars.length];
        LOG.info("Copy hbox lib jars from local filesystem to remote.");
        for (int i = 0; i < clientArguments.libJars.length; i++) {
            assert (!clientArguments.libJars[i].isEmpty());
            if (!clientArguments.libJars[i].startsWith("hdfs://")) {
                Path tfFilesSrc = new Path(clientArguments.libJars[i]);
                tfFilesDst[i] = Utilities.getRemotePath(
                        conf, applicationId, new Path(clientArguments.libJars[i]).getName());
                LOG.info("Copying " + clientArguments.libJars[i] + " to remote path " + tfFilesDst[i].toString());
                dfs.copyFromLocalFile(false, true, tfFilesSrc, tfFilesDst[i]);
                appLibJarsRemotePath.append(tfFilesDst[i].toUri().toString()).append(",");
            } else {
                Path pathRemote = new Path(clientArguments.libJars[i]);
                if (!pathRemote.getFileSystem(conf).exists(pathRemote)) {
                    throw new IOException("hdfs lib jars path " + pathRemote + " not existed!");
                }
                appLibJarsRemotePath.append(clientArguments.libJars[i]).append(",");
            }
        }
        appMasterEnv.put(HboxConstants.Environment.HBOX_LIBJARS_LOCATION.toString(),
                appLibJarsRemotePath.deleteCharAt(appLibJarsRemotePath.length() - 1).toString());

        if ((clientArguments.appType.equals("MXNET") && !conf.getBoolean(HboxConfiguration.HBOX_MXNET_MODE_SINGLE, HboxConfiguration.DEFAULT_HBOX_MXNET_MODE_SINGLE))
                || clientArguments.appType.equals("XFLOW")
                || (clientArguments.appType.equals("XDL") && !conf.getBoolean(HboxConfiguration.HBOX_TF_MODE_SINGLE, HboxConfiguration.DEFAULT_HBOX_TF_MODE_SINGLE))) {
            String appFilesRemoteLocation = appMasterEnv.get(HboxConstants.Environment.HBOX_LIBJARS_LOCATION.toString());
            String[] tfFiles = StringUtils.split(appFilesRemoteLocation, ",");
            for (String file : tfFiles) {
                Path path = new Path(file);
                localResources.put(path.getName(),
                        Utilities.createApplicationResource(path.getFileSystem(conf),
                                path,
                                LocalResourceType.FILE));
            }
        }
    }

    private void prepareJobConfForAM(Map<String, String> appMasterEnv, Map<String, LocalResource> localResources) throws IOException {
        Path jobConfPath = Utilities
                .getRemotePath(conf, applicationId, HboxConstants.HBOX_JOB_CONFIGURATION);
        FSDataOutputStream out =
                FileSystem.create(jobConfPath.getFileSystem(conf), jobConfPath,
                        new FsPermission(JOB_FILE_PERMISSION));
        //write conf to hbox hdfs
        conf.writeXml(out);
        out.close();
        localResources.put(HboxConstants.HBOX_JOB_CONFIGURATION,
                Utilities.createApplicationResource(dfs, jobConfPath, LocalResourceType.FILE));

        appMasterEnv.put(HboxConstants.Environment.HBOX_JOB_CONF_LOCATION.toString(), jobConfPath.toString());
    }

    private void prepareCacheFiles(Map<String, String> appMasterEnv, Map<String, LocalResource> localResources) throws IOException {
        appMasterEnv.put(HboxConstants.Environment.HBOX_CACHE_FILE_LOCATION.toString(), clientArguments.hboxCacheFiles);
        if ((clientArguments.appType.equals("MXNET") && !conf.getBoolean(HboxConfiguration.HBOX_MXNET_MODE_SINGLE, HboxConfiguration.DEFAULT_HBOX_MXNET_MODE_SINGLE))
                || clientArguments.appType.equals("DISTXGBOOST") || clientArguments.appType.equals("XFLOW")
                || (clientArguments.appType.equals("XDL") && !conf.getBoolean(HboxConfiguration.HBOX_TF_MODE_SINGLE, HboxConfiguration.DEFAULT_HBOX_TF_MODE_SINGLE))) {
            URI defaultUri = new Path(conf.get("fs.defaultFS")).toUri();
            LOG.info("default URI is " + defaultUri.toString());
            String appCacheFilesRemoteLocation = appMasterEnv.get(HboxConstants.Environment.HBOX_CACHE_FILE_LOCATION.toString());
            String[] cacheFiles = StringUtils.split(appCacheFilesRemoteLocation, ",");
            for (String path : cacheFiles) {
                Path pathRemote;
                String aliasName;
                if (path.contains("#")) {
                    String[] paths = StringUtils.split(path, "#");
                    if (paths.length != 2) {
                        throw new RuntimeException("Error cacheFile path format " + appCacheFilesRemoteLocation);
                    }
                    pathRemote = new Path(paths[0]);
                    aliasName = paths[1];
                } else {
                    pathRemote = new Path(path);
                    aliasName = pathRemote.getName();
                }
                URI pathRemoteUri = pathRemote.toUri();
                if (Boolean.parseBoolean(conf.get(HboxConfiguration.HBOX_APPEND_DEFAULTFS_ENABLE, String.valueOf(HboxConfiguration.DEFAULT_HBOX_APPEND_DEFAULTFS_ENABLE)))) {
                    if (pathRemoteUri.getScheme() == null || pathRemoteUri.getHost() == null) {
                        pathRemote = new Path(defaultUri.toString(), pathRemote.toString());
                    }
                }
                LOG.info("Cache file remote path is " + pathRemote + " and alias name is " + aliasName);
                localResources.put(aliasName,
                        Utilities.createApplicationResource(pathRemote.getFileSystem(conf),
                                pathRemote,
                                LocalResourceType.FILE));
            }
        }
    }

    private void prepareCacheArchives(Map<String, String> appMasterEnv, Map<String, LocalResource> localResources) throws IOException {
        appMasterEnv.put(HboxConstants.Environment.HBOX_CACHE_ARCHIVE_LOCATION.toString(), clientArguments.hboxCacheArchives);
        if ((clientArguments.appType.equals("MXNET") && !conf.getBoolean(HboxConfiguration.HBOX_MXNET_MODE_SINGLE, HboxConfiguration.DEFAULT_HBOX_MXNET_MODE_SINGLE))
                || clientArguments.appType.equals("DISTXGBOOST") || clientArguments.appType.equals("XFLOW")
                || (clientArguments.appType.equals("XDL") && !conf.getBoolean(HboxConfiguration.HBOX_TF_MODE_SINGLE, HboxConfiguration.DEFAULT_HBOX_TF_MODE_SINGLE))) {
            URI defaultUri = new Path(conf.get("fs.defaultFS")).toUri();
            String appCacheArchivesRemoteLocation = appMasterEnv.get(HboxConstants.Environment.HBOX_CACHE_ARCHIVE_LOCATION.toString());
            String[] cacheArchives = StringUtils.split(appCacheArchivesRemoteLocation, ",");
            for (String path : cacheArchives) {
                Path pathRemote;
                String aliasName;
                if (path.contains("#")) {
                    String[] paths = StringUtils.split(path, "#");
                    if (paths.length != 2) {
                        throw new RuntimeException("Error cacheArchive path format " + appCacheArchivesRemoteLocation);
                    }
                    pathRemote = new Path(paths[0]);
                    aliasName = paths[1];
                } else {
                    pathRemote = new Path(path);
                    aliasName = pathRemote.getName();
                }
                URI pathRemoteUri = pathRemote.toUri();
                if (Boolean.parseBoolean(conf.get(HboxConfiguration.HBOX_APPEND_DEFAULTFS_ENABLE, String.valueOf(HboxConfiguration.DEFAULT_HBOX_APPEND_DEFAULTFS_ENABLE)))) {
                    if (pathRemoteUri.getScheme() == null || pathRemoteUri.getHost() == null) {
                        pathRemote = new Path(defaultUri.toString(), pathRemote.toString());
                    }
                }
                LOG.info("Cache archive remote path is " + pathRemote + " and alias name is " + aliasName);
                localResources.put(aliasName,
                        Utilities.createApplicationResource(pathRemote.getFileSystem(conf),
                                pathRemote,
                                LocalResourceType.ARCHIVE));
            }
        }
    }

    private void prepareAppMasterJar(Map<String, String> appMasterEnv, Map<String, LocalResource> localResources) throws IOException {
        Path appJarSrc = new Path(clientArguments.appMasterJar);
        Path appJarDst = Utilities
                .getRemotePath(conf, applicationId, HboxConstants.HBOX_APPLICATION_JAR);
        LOG.info("Copying " + appJarSrc + " to remote path " + appJarDst.toString());
        dfs.copyFromLocalFile(false, true, appJarSrc, appJarDst);

        localResources.put(HboxConstants.HBOX_APPLICATION_JAR,
                Utilities.createApplicationResource(dfs, appJarDst, LocalResourceType.FILE));
        appMasterEnv.put(HboxConstants.Environment.APP_JAR_LOCATION.toString(), appJarDst.toUri().toString());
    }

    private void prepareInputEnvForAM(Map<String, String> appMasterEnv) {
        putInputEnv(HboxConstants.S3, appMasterEnv);
        putInputEnv(HboxConstants.HDFS, appMasterEnv);
    }

    private void putInputEnv(String inputType, Map<String, String> appMasterEnv) {
        ConcurrentHashMap<String, String> inputMap;
        String envName;
        if (inputType.equals(HboxConstants.S3)) {
            inputMap = this.s3InputPaths;
            envName = HboxConstants.Environment.HBOX_S3_INPUTS.toString();
        } else {
            inputMap = this.inputPaths;
            envName = HboxConstants.Environment.HBOX_INPUTS.toString();
        }
        Set<String> inputPathKeys = inputMap.keySet();
        StringBuilder inputLocation = new StringBuilder(1000);
        if (inputPathKeys.size() > 0) {
            for (String key : inputPathKeys) {
                inputLocation.append(inputMap.get(key)).
                        append("#").
                        append(key).
                        append("|");
            }
            appMasterEnv.put(envName,
                    inputLocation.deleteCharAt(inputLocation.length() - 1).toString());
        }
    }

    private void prepareOutputEnvForAM(Map<String, String> appMasterEnv) {
        putOutputEnv(HboxConstants.S3, appMasterEnv);
        putOutputEnv(HboxConstants.HDFS, appMasterEnv);
    }

    private void putOutputEnv(String outputType, Map<String, String> appMasterEnv) {
        ConcurrentHashMap<String, String> outputMap;
        String envName;
        if (outputType.equals(HboxConstants.S3)) {
            outputMap = this.s3OutputPaths;
            envName = HboxConstants.Environment.HBOX_S3_OUTPUTS.toString();
        } else {
            outputMap = this.outputPaths;
            envName = HboxConstants.Environment.HBOX_OUTPUTS.toString();
        }
        Set<String> outputPathKeys = outputMap.keySet();
        StringBuilder outputLocation = new StringBuilder(1000);
        if (outputPathKeys.size() > 0) {
            for (String key : outputPathKeys) {
                for (String value : StringUtils.split(outputMap.get(key), ",")) {
                    outputLocation.append(value).
                            append("#").
                            append(key).
                            append("|");
                }
            }
            appMasterEnv.put(envName, outputLocation.deleteCharAt(outputLocation.length() - 1).toString());
        }
    }


    private void prepareClassPathEnvForAM(Map<String, String> appMasterEnv) {
        StringBuilder classPathEnv = new StringBuilder("${CLASSPATH}:./*");
        for (String cp : conf.getStrings(HboxConfiguration.YARN_APPLICATION_CLASSPATH,
                HboxConfiguration.DEFAULT_HBOX_APPLICATION_CLASSPATH)) {
            classPathEnv.append(':');
            classPathEnv.append(cp.trim());
        }
        appMasterEnv.put("CLASSPATH", classPathEnv.toString());
        if (clientArguments.userPath != null && !clientArguments.userPath.equals("")) {
            appMasterEnv.put(HboxConstants.Environment.USER_PATH.toString(), clientArguments.userPath);
        }
    }

    private List<String> prepareLaunchCommandForAM(Map<String, String> appMasterEnv, ApplicationSubmissionContext applicationContext) {
        LOG.info("Building application master launch command");
        int driverMem = conf.getInt(HboxConfiguration.HBOX_DRIVER_MEMORY, HboxConfiguration.DEFAULT_HBOX_DRIVER_MEMORY);
        List<String> appMasterArgs = new ArrayList<>(20);
        appMasterArgs.add("${JAVA_HOME}" + "/bin/java");
        appMasterArgs.add("-Xms" + Math.min(driverMem, maxContainerMem) + "m");
        appMasterArgs.add("-Xmx" + Math.min(driverMem, maxContainerMem) + "m");
        appMasterArgs.add("net.qihoo.hbox.AM.ApplicationMaster");
        appMasterArgs.add("1>" + ApplicationConstants.LOG_DIR_EXPANSION_VAR
                + "/" + ApplicationConstants.STDOUT);
        appMasterArgs.add("2>" + ApplicationConstants.LOG_DIR_EXPANSION_VAR
                + "/" + ApplicationConstants.STDERR);

        StringBuilder command = new StringBuilder();
        for (String arg : appMasterArgs) {
            command.append(arg).append(" ");
        }

        LOG.info("Application master launch command: " + command.toString());
        List<String> appMasterLaunchcommands = new ArrayList<>();
        appMasterLaunchcommands.add(command.toString());
        Resource capability = Records.newRecord(Resource.class);
        int overHeadMem = (int) Math.max(driverMem * conf.getDouble(HboxConfiguration.HBOX_MEMORY_OVERHEAD_FRACTION, HboxConfiguration.DEFAULT_HBOX_MEMORY_OVERHEAD_FRACTION),
                conf.getInt(HboxConfiguration.HBOX_MEMORY_OVERHEAD_MINIMUM, HboxConfiguration.DEFAULT_HBOX_MEMORY_OVERHEAD_MINIMUM));
        driverMem += overHeadMem;
        int driverCores = conf.getInt(HboxConfiguration.HBOX_DRIVER_CORES, HboxConfiguration.DEFAULT_HBOX_DRIVER_CORES);
        capability.setMemory(Math.min(driverMem, maxContainerMem));
        capability.setVirtualCores(driverCores);
        applicationContext.setResource(capability);
        appMasterEnv.put("DOCKER_CONTAINER_MEMORY", driverMem + "");
        appMasterEnv.put("DOCKER_CONTAINER_CPU", driverCores + "");
        return appMasterLaunchcommands;
    }

    private void prepareOtherEnvsForAM(Map<String, String> appMasterEnv, Map<String, LocalResource> localResources) throws IOException {
        LOG.info("Building environments for the application master");
        String containerType = conf.get(HboxConfiguration.CONTAINER_EXECUTOR_TYPE,
                HboxConfiguration.DEFAULT_CONTAINER_EXECUTOR_TYPE);
        appMasterEnv.put(HboxConstants.Environment.HADOOP_USER_NAME.toString(), conf.get("hadoop.job.ugi").split(",")[0]);
        appMasterEnv.put(HboxConstants.Environment.HBOX_CONTAINER_EXECUTOR_TYPE.toString(), containerType);
        appMasterEnv.put(HboxConstants.Environment.HBOX_CONTAINER_MAX_MEMORY.toString(), String.valueOf(maxContainerMem));

        if (clientArguments.appType.equals("VPC") || clientArguments.appType.equals("DIGITS") || containerType.toUpperCase().equals("DOCKER")) {
            String imageName = conf.get(HboxConfiguration.DOCKER_CONTAINER_EXECUTOR_IMAGE_NAME,
                    HboxConfiguration.DEFALUT_DOCKER_CONTAINER_EXECUTOR_IMAGE_NAME);
            if (clientArguments.appType.equals("DIGITS")) {
                imageName = conf.get(HboxConfiguration.HBOX_DIGITS_IMAGE_NAME,
                        HboxConfiguration.DEFAULT_HBOX_DIGITS_IMAGE_NAME);
            }
            if (imageName.equals("")) {
                throw new IllegalArgumentException(
                        "Invalid image name for docker, exiting."
                                + "Specified image name=" + imageName);
            }
            appMasterEnv.put(HboxConstants.Environment.HBOX_DOCKER_CONTAINER_EXECUTOR_IMAGE_NAME.toString(), imageName);
            appMasterEnv.put(HboxConstants.Environment.HBOX_DOCKER_CONTAINER_EXECUTOR_EXEC_NAME.toString(), conf.get(HboxConfiguration.DOCKER_CONTAINER_EXECUTOR_EXEC_NAME,
                    HboxConfiguration.DEFAULT_DOCKER_CONTAINER_EXECUTOR_EXEC_NAME));
            appMasterEnv.put(HboxConstants.Environment.HBOX_CONTAINER_EXECUTOR_TYPE.toString(), "docker");
        }

        if (clientArguments.appType != null && !clientArguments.appType.equals("")) {
            appMasterEnv.put(HboxConstants.Environment.HBOX_APP_TYPE.toString(), clientArguments.appType);
        } else {
            appMasterEnv.put(HboxConstants.Environment.HBOX_APP_TYPE.toString(), HboxConfiguration.DEFAULT_HBOX_APP_TYPE.toUpperCase());
        }
        appMasterEnv.put(HboxConstants.Environment.HBOX_APP_NAME.toString(), clientArguments.appName);
        if (clientArguments.hboxFiles != null) {
            prepareFilesForAM(appMasterEnv, localResources);
        }

        if (clientArguments.libJars != null) {
            prepareJarsForAM(appMasterEnv, localResources);
        }

        appMasterEnv.put(HboxConstants.Environment.HBOX_STAGING_LOCATION.toString(), Utilities
                .getRemotePath(conf, applicationId, "").toString());

        if (!clientArguments.appType.equals("VPC") && !clientArguments.appType.equals("DIGITS")) {
            if (clientArguments.hboxCmd != null && !clientArguments.hboxCmd.equals("")) {
                appMasterEnv.put(HboxConstants.Environment.HBOX_EXEC_CMD.toString(), clientArguments.hboxCmd);
            } else if (clientArguments.launchCmd != null && !clientArguments.launchCmd.equals("")) {
                appMasterEnv.put(HboxConstants.Environment.HBOX_EXEC_CMD.toString(), clientArguments.launchCmd);
            } else {
                throw new IllegalArgumentException("Invalid hbox cmd for the application");
            }
        }
        //HBOX specific one worker to upload output dir
        if (clientArguments.outputIndex >= 0) {
            appMasterEnv.put(HboxConstants.Environment.HBOX_OUTPUT_INDEX.toString(), String.valueOf(clientArguments.outputIndex));
        }
    }

    private boolean submitAndMonitor() throws IOException, YarnException {
        if (clientArguments.inputs != null || clientArguments.s3Inputs != null) {
            assignInput();
        }

        if (clientArguments.outputs != null || clientArguments.s3Outputs != null) {
            assignOutput();
        }

        if (clientArguments.hboxCacheFiles != null) {
            assignCacheFiles();
        }

        if (clientArguments.hboxCacheArchives != null) {
            assignCacheArchives();
        }

        GetNewApplicationResponse newAppResponse = newAPP.getNewApplicationResponse();
        applicationId = newAppResponse.getApplicationId();
        LOG.info("Got new Application: " + applicationId.toString());
        conf.set(HboxConfiguration.HBOX_APP_ID, applicationId.toString());
        maxContainerMem = newAppResponse.getMaximumResourceCapability().getMemory();

        if (clientArguments.appType.equals("VPC") || clientArguments.appType.equals("DIGITS") || clientArguments.appType.equals("MPI") || clientArguments.appType.equals("HOROVOD")) {
            conf.set(HboxConfiguration.HBOX_CONTAINER_TYPE, HboxConfiguration.DEFAULT_HBOX_CONTAINER_TYPE);
        } else if (clientArguments.appType.equals("XDL")) {
            configXDLAmContainer();
        }

        LOG.info("Current container type: " + conf.get(HboxConfiguration.HBOX_CONTAINER_TYPE, HboxConfiguration.DEFAULT_HBOX_CONTAINER_TYPE));

        dfs = FileSystem.get(conf);
        Map<String, LocalResource> localResources = new HashMap<>();
        Map<String, String> appMasterEnv = new HashMap<>();
        prepareJobConfForAM(appMasterEnv, localResources);

        checkArguments(conf, newAppResponse);

        ApplicationSubmissionContext applicationContext = newAPP.getApplicationSubmissionContext();
        applicationContext.setApplicationId(applicationId);
        applicationContext.setApplicationName(clientArguments.appName);
        applicationContext.setApplicationType(clientArguments.appType);
        applicationContext.setNodeLabelExpression(conf.get(HboxConfiguration.HBOX_JOB_LABEL_NAME));
        LOG.info("Application maxAppAttempts: " + applicationContext.getMaxAppAttempts());

        prepareAppMasterJar(appMasterEnv, localResources);
        prepareOtherEnvsForAM(appMasterEnv, localResources);

        if (clientArguments.hboxCacheFiles != null && !clientArguments.hboxCacheFiles.equals("")) {
            prepareCacheFiles(appMasterEnv, localResources);
        }

        if (clientArguments.hboxCacheArchives != null && !clientArguments.hboxCacheArchives.equals("")) {
            prepareCacheArchives(appMasterEnv, localResources);
        }
        prepareInputEnvForAM(appMasterEnv);
        prepareOutputEnvForAM(appMasterEnv);
        prepareClassPathEnvForAM(appMasterEnv);

        if (appMasterUserEnv.size() > 0) {
            for (String envKey : appMasterUserEnv.keySet()) {
                Utilities.addPathToEnvironment(appMasterEnv, envKey, appMasterUserEnv.get(envKey));
            }
        }
        List<String> appMasterLaunchcommands = prepareLaunchCommandForAM(appMasterEnv, applicationContext);
        ContainerLaunchContext amContainer = ContainerLaunchContext.newInstance(
                localResources, appMasterEnv, appMasterLaunchcommands, null, null, null);

        applicationContext.setAMContainerSpec(amContainer);

        Priority priority = Records.newRecord(Priority.class);
        priority.setPriority(conf.getInt(HboxConfiguration.HBOX_APP_PRIORITY, HboxConfiguration.DEFAULT_HBOX_APP_PRIORITY));
        applicationContext.setPriority(priority);
        applicationContext.setQueue(conf.get(HboxConfiguration.HBOX_APP_QUEUE, HboxConfiguration.DEFAULT_HBOX_APP_QUEUE));

        try {
            LOG.info("Submitting application to ResourceManager");
            applicationId = yarnClient.submitApplication(applicationContext);
            isRunning.set(applicationId != null);
            if (isRunning.get()) {
                LOG.info("Application submitAndMonitor succeed");
            } else {
                throw new RuntimeException("Application submitAndMonitor failed!");
            }
        } catch (YarnException e) {
            throw new RuntimeException("Application submitAndMonitor failed!");
        }
        /*TODO
         *  hbox-kill command use the HBOX_HOME/
         * */
        LOG.info("To kill this job: /usr/bin/hadoop/software/hbox/bin/hbox-kill " + applicationId.toString());
        boolean isApplicationSucceed = waitCompleted();
        return isApplicationSucceed;
    }

    private boolean waitCompleted() throws IOException, YarnException {
        ApplicationReport applicationReport = getApplicationReport(applicationId, yarnClient);
        LOG.info("The url to track the job: " + applicationReport.getTrackingUrl());
        while (true) {
            assert (applicationReport != null);
            if (hboxClient == null && isRunning.get()) {
                LOG.info("Application report for " + applicationId +
                        " (state: " + applicationReport.getYarnApplicationState().toString() + ")");
                hboxClient = getAppMessageHandler(conf, applicationReport.getHost(),
                        applicationReport.getRpcPort());
            }

            YarnApplicationState yarnApplicationState = applicationReport.getYarnApplicationState();
            FinalApplicationStatus finalApplicationStatus = applicationReport.getFinalApplicationStatus();
            if (YarnApplicationState.FINISHED == yarnApplicationState) {
                hboxClient = null;
                isRunning.set(false);
                if (FinalApplicationStatus.SUCCEEDED == finalApplicationStatus) {
                    return true;
                } else {
                    LOG.info("Application has completed failed with YarnApplicationState=" + yarnApplicationState.toString() +
                            " and FinalApplicationStatus=" + finalApplicationStatus.toString());
                    return false;
                }
            } else if (YarnApplicationState.KILLED == yarnApplicationState
                    || YarnApplicationState.FAILED == yarnApplicationState) {
                hboxClient = null;
                isRunning.set(false);
                LOG.info("Application has completed with YarnApplicationState=" + yarnApplicationState.toString() +
                        " and FinalApplicationStatus=" + finalApplicationStatus.toString());
                return false;
            }

            if (hboxClient != null) {
                try {
                    Message[] messages = hboxClient.fetchApplicationMessages();
                    if (messages != null && messages.length > 0) {
                        for (Message message : messages) {
                            if (message.getLogType() == LogType.STDERR) {
                                LOG.info(message.getMessage());
                            } else {
                                System.out.println(message.getMessage());
                            }
                        }
                    }
                } catch (UndeclaredThrowableException e) {
                    hboxClient = null;
                    LOG.info("Connecting to ApplicationManager failed, try again later ", e);
                }
            }

            int logInterval = conf.getInt(HboxConfiguration.HBOX_LOG_PULL_INTERVAL, HboxConfiguration.DEFAULT_HBOX_LOG_PULL_INTERVAL);
            Utilities.sleep(logInterval);
            applicationReport = getApplicationReport(applicationId, yarnClient);
        }
    }

    public static void main(String[] args) {
        showWelcome();
        boolean result = false;
        try {
            LOG.info("Initializing Client");
            Client client = new Client(args);
            client.init();
            result = client.submitAndMonitor();
        } catch (Exception e) {
            LOG.fatal("Error running Client", e);
            System.exit(1);
        }
        if (result) {
            LOG.info("Application completed successfully");
            System.exit(0);
        }
        LOG.error("Application run failed!");
        System.exit(2);
    }
}
