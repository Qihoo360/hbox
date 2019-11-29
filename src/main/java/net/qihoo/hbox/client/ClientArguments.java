package net.qihoo.hbox.client;

import net.qihoo.hbox.AM.ApplicationMaster;
import net.qihoo.hbox.common.JobPriority;
import net.qihoo.hbox.conf.HboxConfiguration;
import org.apache.commons.cli.*;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.mapred.JobConf;

import java.io.IOException;
import java.util.Properties;

class ClientArguments {
    private static final Log LOG = LogFactory.getLog(ClientArguments.class);
    private Options allOptions;
    String appName;
    String appType;
    String clusterName;
    int driverMem;
    int driverCores;
    int workerMemory;
    int workerVCores;
    int workerGCores;
    int workerNum;
    int chiefWorkerMemory;
    int evaluatorWorkerMemory;
    int psMemory;
    int psVCores;
    int psGCores;
    int psNum;
    String duration;
    String[] hboxFiles;
    String[] libJars;
    String hboxCmd;
    String launchCmd;
    String inputStrategy;
    String outputStrategy;
    Properties inputs;
    Properties outputs;
    Properties s3outputs;
    String hboxCacheFiles;
    String hboxCacheArchives;
    int priority;
    String queue;
    String userPath;
    String appMasterJar;
    int boardIndex;
    int boardReloadInterval;
    String boardLogDir;
    Boolean boardEnable;
    String boardHistoryDir;
    String boardModelPB;
    int boardCacheTimeout;
    Boolean isRenameInputFile;
    Boolean createContaineridDir;
    public Boolean inputStream;
    public int streamEpoch;
    public Boolean outputStream;
    public Boolean inputStreamShuffle;
    public Boolean userClasspathFirst;
    public Class<?> inputFormatClass;
    public Class<?> outputFormatClass;
    public Boolean hostLocalEnable;
    public Boolean tfEvaluator;
    Properties confs;
    int outputIndex;

    public ClientArguments(String[] args) throws IOException, ParseException, ClassNotFoundException {
        this.init();
        this.cliParser(args);
    }

    private void init() {
        appName = "";
        clusterName = HboxConfiguration.DEFAULT_HBOX_CLUSTER_NAME.toLowerCase();
        appType = HboxConfiguration.DEFAULT_HBOX_APP_TYPE.toUpperCase();
        driverMem = HboxConfiguration.DEFAULT_HBOX_DRIVER_MEMORY;
        driverCores = HboxConfiguration.DEFAULT_HBOX_DRIVER_CORES;
        workerMemory = HboxConfiguration.DEFAULT_HBOX_WORKER_MEMORY;
        workerVCores = HboxConfiguration.DEFAULT_HBOX_WORKER_VCORES;
        workerGCores = HboxConfiguration.DEFAULT_HBOX_WORKER_GPU;
        workerNum = HboxConfiguration.DEFAULT_HBOX_WORKER_NUM;
        chiefWorkerMemory = workerMemory;
        evaluatorWorkerMemory = workerMemory;
        psMemory = HboxConfiguration.DEFAULT_HBOX_PS_MEMORY;
        psVCores = HboxConfiguration.DEFAULT_HBOX_PS_VCORES;
        psGCores = HboxConfiguration.DEFAULT_HBOX_PS_GPU;
        psNum = HboxConfiguration.DEFAULT_HBOX_PS_NUM;
        hboxFiles = null;
        libJars = null;
        hboxCmd = "";
        launchCmd = "";
        hboxCacheFiles = "";
        hboxCacheArchives = "";
        appMasterJar = "";
        userPath = "";
        priority = HboxConfiguration.DEFAULT_HBOX_APP_PRIORITY;
        queue = "";
        duration = HboxConfiguration.DEFAULT_HBOX_VPC_DURATION;
        boardIndex = HboxConfiguration.DEFAULT_HBOX_TF_BOARD_WORKER_INDEX;
        boardReloadInterval = HboxConfiguration.DEFAULT_HBOX_TF_BOARD_RELOAD_INTERVAL;
        boardEnable = HboxConfiguration.DEFAULT_HBOX_TF_BOARD_ENABLE;
        boardLogDir = HboxConfiguration.DEFAULT_HBOX_TF_BOARD_LOG_DIR;
        boardHistoryDir = HboxConfiguration.DEFAULT_HBOX_TF_BOARD_HISTORY_DIR;
        boardModelPB = HboxConfiguration.DEFAULT_HBOX_BOARD_MODELPB;
        boardCacheTimeout = HboxConfiguration.DEFAULT_HBOX_BOARD_CACHE_TIMEOUT;
        isRenameInputFile = HboxConfiguration.DEFAULT_HBOX_INPUTFILE_RENAME;
        userClasspathFirst = HboxConfiguration.DEFAULT_HBOX_USER_CLASSPATH_FIRST;
        hostLocalEnable = HboxConfiguration.DEFAULT_HBOX_HOST_LOCAL_ENABLE;
        inputStream = HboxConfiguration.DEFAULT_HBOX_INPUT_STREAM;
        streamEpoch = HboxConfiguration.DEFAULT_HBOX_STREAM_EPOCH;
        outputStream = HboxConfiguration.DEFAULT_HBOX_OUTPUT_STREAM;
        inputStreamShuffle = HboxConfiguration.DEFAULT_HBOX_INPUT_STREAM_SHUFFLE;
        inputFormatClass = HboxConfiguration.DEFAULT_HBOX_INPUTF0RMAT_CLASS;
        outputFormatClass = HboxConfiguration.DEFAULT_HBOX_OUTPUTF0RMAT_CLASS;
        inputStrategy = HboxConfiguration.DEFAULT_HBOX_INPUT_STRATEGY.toUpperCase();
        outputStrategy = HboxConfiguration.DEFAULT_HBOX_OUTPUT_STRATEGY.toUpperCase();
        createContaineridDir = HboxConfiguration.DEFAULT_HBOX_CREATE_CONTAINERID_DIR;
        tfEvaluator = HboxConfiguration.DEFAULT_HBOX_TF_EVALUATOR;
        outputIndex = -1;

        allOptions = new Options();
        allOptions.addOption("appName", "app-name", true,
                "set the Application name");
        allOptions.addOption("appType", "app-type", true,
                "set the Application type, default \"hbox\"");
        allOptions.addOption("cluster", "cluster", true,
                "set the cluster that application submit to");

        allOptions.addOption("driverMemory", "driver-memory", true,
                "Amount of memory in MB to be requested to run the application master");
        allOptions.addOption("driverCores", "driver-cores", true,
                "Amount of vcores to be requested to run the application master");

        allOptions.addOption("workerMemory", "worker-memory", true,
                "Amount of memory in MB to be requested to run worker");
        allOptions.addOption("workerCores", "worker-cores", true,
                "Amount of vcores to be requested to run worker");
        allOptions.addOption("workerGpus", "worker-gpus", true,
                "Amount of gpu cores to be requested to run worker");
        allOptions.addOption("workerNum", "worker-num", true,
                "No. of containers on which the worker needs to be executed");

        allOptions.addOption("chiefWorkerMemory", "chiefworker-memory", true,
                "Amount of memory in MB to be requested to run the chief worker");
        allOptions.addOption("evaluatorWorkerMemory", "evaluatorworker-memory", true,
                "Amount of memory in MB to be requested to run the evaluator worker");

        allOptions.addOption("duration", "duration", true,
                "Duration when use vpc and digits mode, default:1(hours)");

        allOptions.addOption("psMemory", "ps-memory", true,
                "Amount of memory in MB to be requested to run ps");
        allOptions.addOption("psCores", "ps-cores", true,
                "Amount of vcores to be requested to run ps");
        allOptions.addOption("psGpus", "ps-gpus", true,
                "Amount of gpu cores to be requested to run ps");
        allOptions.addOption("psNum", "ps-num", true,
                "No. of containers on which the ps needs to be executed");

        allOptions.addOption("files", "files", true,
                "Location of the hbox files used in container");

        allOptions.addOption("jars", "jars", true,
                "Location of the hbox lib jars used in container");

        allOptions.addOption("hboxCmd", "hbox-cmd", true, "Cmd for hbox program");
        allOptions.addOption("launchCmd", "launch-cmd", true, "Cmd for hbox program");
        allOptions.addOption("userPath", "user-path", true,
                "add the user set PATH");
        allOptions.addOption("cacheFile", "cacheFile", true,
                "add the hbox hdfsFile PATH");
        allOptions.addOption("cacheArchive", "cacheArchive", true,
                "add the hbox hdfsPackage PATH");
        allOptions.addOption("priority", "priority", true, "Application Priority. Default DEFAULT");
        allOptions.addOption("queue", "queue", true,
                "RM Queue in which this application is to be submitted");

        allOptions.addOption("boardIndex", "board-index", true,
                "if app type is tensorflow or tensor2tensor, worker index for run tensorboard, default:0");
        allOptions.addOption("boardReloadInterval", "board-reloadinterval", true,
                "if app type is tensorflow or tensor2tensor, How often the backend should load more data for tensorboard, default:1");
        allOptions.addOption("boardLogDir", "board-logdir", true,
                "if app type is tensorflow or tensor2tensor, tensorflow log dir, default:eventLog");
        allOptions.addOption("boardEnable", "board-enable", true,
                "if app type is tensorflow or tensor2tensor, enable to run tensorboard, default:false");
        allOptions.addOption("boardHistoryDir", "board-historydir", true,
                "if app type is not vpc or digistic, hdfs path for board event log");
        allOptions.addOption("boardModelPB", "board-modelpb", true,
                "if app type is not tensorflow or tensor2tensor, model pb for visualDL");
        allOptions.addOption("boardCacheTimeout", "board-cacheTimeout", true,
                "if app type is not tensorflow or tensor2tensor, visualDL memory cache timeout duration in seconds, default:20");

        allOptions.addOption("isRenameInputFile", "isRenameInputFile", true,
                "whether rename the inputFiles when download from hdfs");
        allOptions.addOption("createContaineridDir", "create-containerid-dir", true,
                "if worker num is 1, this param to enable create container id dir in output path, default:true");
        allOptions.addOption("userClasspathFirst", "user-classpath-first", true,
                "whether user add classpath first or not, default:true");
        allOptions.addOption("hostLocalEnable", "host-local-enable", true,
                "whether host local or not, default:false");

        allOptions.addOption("inputformatEnable", "inputformat-enable", true,
                "whether read data from hdfs in stream way or not, default:false");
        allOptions.addOption("outputformatEnable", "outputformat-enable", true,
                "whether write result to hdfs in stream way or not, default:false");
        allOptions.addOption("inputformatShuffle", "inputformat-shuffle", true,
                "If inputformat-enable is true, whether shuffle data in worker or not, default:false");
        allOptions.addOption("inputFormatClass", "inputformat", true,
                "The inputformat class, default:org.apache.hadoop.mapred.TextInputFormat");
        allOptions.addOption("outputFormatClass", "outputformat", true,
                "The outputformat class, default:org.apache.hadoop.mapred.lib.TextMultiOutputFormat");
        allOptions.addOption("streamEpoch", "stream-epoch", true,
                "The num of epoch for stream input.");

        allOptions.addOption("inputStrategy", "input-strategy", true,
                "The input strategy for user data input, DOWNLOAD,PLACEHOLDER or STREAM, default:DOWNLOAD");
        allOptions.addOption("outputStrategy", "output-strategy", true,
                "The output strategy for user result ouput, UPLOAD or STREAM, default:UPLOAD");

        allOptions.addOption("tfEvaluator", "tf-evaluator", true,
                "Using the evaluator during the tensorflow distribute training.");

        allOptions.addOption("outputIndex", "output-index", true,
                "Setting the index of worker which to upload the output, default uploading the output of all the workers.");

        allOptions.addOption("help", "help", false, "Print usage");

        OptionBuilder.withArgName("property=value");
        OptionBuilder.hasArgs(Integer.MAX_VALUE);
        OptionBuilder
                .withValueSeparator('=');
        OptionBuilder
                .withDescription("hbox configure");
        Option conf = OptionBuilder
                .create("conf");
        allOptions.addOption(conf);

        OptionBuilder.withArgName("property#value");
        OptionBuilder.hasArgs(Integer.MAX_VALUE);
        OptionBuilder
                .withValueSeparator('#');
        OptionBuilder
                .withDescription("dfs location,representing the source data of hbox");
        Option property = OptionBuilder
                .create("input");
        allOptions.addOption(property);

        OptionBuilder.withArgName("property#value");
        OptionBuilder.hasArgs(Integer.MAX_VALUE);
        OptionBuilder
                .withValueSeparator('#');
        OptionBuilder
                .withDescription("dfs location,representing the hbox result");
        Option output = OptionBuilder
                .create("output");
        allOptions.addOption(output);
        OptionBuilder.withArgName("property#value");
        OptionBuilder.hasArgs(Integer.MAX_VALUE);
        OptionBuilder
                .withValueSeparator('#');
        OptionBuilder
                .withDescription("amazon s3 location,representing the hbox result");
        Option s3output = OptionBuilder.create("s3output");
        allOptions.addOption(s3output);

    }

    private void cliParser(String[] args) throws ParseException, IOException, ClassNotFoundException {
        CommandLine cliParser = new BasicParser().parse(allOptions, args);
        if (cliParser.getOptions().length == 0 || cliParser.hasOption("help")) {
            printUsage(allOptions);
            System.exit(0);
        }

        if (cliParser.hasOption("cluster")) {
            clusterName = cliParser.getOptionValue("cluster").trim().toLowerCase();
        }

        if (cliParser.hasOption("app-name")) {
            appName = cliParser.getOptionValue("app-name");
        }

        if (appName.trim().equals("")) {
            appName = "hbox-" + System.currentTimeMillis();
        }

        if (cliParser.hasOption("app-type")) {
            appType = cliParser.getOptionValue("app-type").trim().toUpperCase();
        }

        if (!appType.equals("TENSORFLOW") && !appType.equals("TENSOR2TENSOR") && !appType.equals("MXNET") && !appType.equals("DISTLIGHTLDA") && !appType.equals("XFLOW") && !appType.equals("XDL")) {
            psNum = 0;
        }

        if (cliParser.hasOption("conf")) {
            confs = cliParser.getOptionProperties("conf");
            if (!"TENSORFLOW".equals(appType) && !"TENSOR2TENSOR".equals(appType) && !"MXNET".equals(appType) && !appType.equals("DISTLIGHTLDA") && !appType.equals("XFLOW") && !appType.equals("XDL")) {
                if (confs.containsKey("hbox.ps.num")) {
                    confs.setProperty("hbox.ps.num", "0");
                }
            }
        }

        if (cliParser.hasOption("driver-memory")) {
            driverMem = getNormalizedMem(cliParser.getOptionValue("driver-memory"));
        }

        if (cliParser.hasOption("driver-cores")) {
            String workerVCoresStr = cliParser.getOptionValue("driver-cores");
            driverCores = Integer.parseInt(workerVCoresStr);
        }

        if (cliParser.hasOption("worker-memory")) {
            workerMemory = getNormalizedMem(cliParser.getOptionValue("worker-memory"));
            chiefWorkerMemory = workerMemory;
            evaluatorWorkerMemory = workerMemory;
        }

        if (cliParser.hasOption("worker-cores")) {
            String workerVCoresStr = cliParser.getOptionValue("worker-cores");
            workerVCores = Integer.parseInt(workerVCoresStr);
        }
        if (cliParser.hasOption("worker-gpus")) {
            String workerGCoresStr = cliParser.getOptionValue("worker-gpus");
            workerGCores = Integer.parseInt(workerGCoresStr);
        }

        if (cliParser.hasOption("worker-num")) {
            String workerNumStr = cliParser.getOptionValue("worker-num");
            workerNum = Integer.parseInt(workerNumStr);
        }

        if (cliParser.hasOption("duration")) {
            duration = cliParser.getOptionValue("duration");
        }

        if ("TENSORFLOW".equals(appType) || "TENSOR2TENSOR".equals(appType) || "MXNET".equals(appType) || appType.equals("DISTLIGHTLDA") || appType.equals("XFLOW") || appType.equals("XDL")) {
            if (cliParser.hasOption("ps-memory")) {
                psMemory = getNormalizedMem(cliParser.getOptionValue("ps-memory"));
            }

            if (cliParser.hasOption("ps-cores")) {
                String psVCoresStr = cliParser.getOptionValue("ps-cores");
                psVCores = Integer.parseInt(psVCoresStr);
            }

            if (cliParser.hasOption("ps-gpus")) {
                String psGCoresStr = cliParser.getOptionValue("ps-gpus");
                psGCores = Integer.parseInt(psGCoresStr);
            }

            if (cliParser.hasOption("ps-num")) {
                String psNumStr = cliParser.getOptionValue("ps-num");
                psNum = Integer.parseInt(psNumStr);
            }
        }

        if (cliParser.hasOption("priority")) {
            String priorityStr = cliParser.getOptionValue("priority");
            for (JobPriority e : JobPriority.values()) {
                if (priorityStr.equals(e.toString())) {
                    priority = e.ordinal();
                }
            }
        }

        if (cliParser.hasOption("queue")) {
            queue = cliParser.getOptionValue("queue");
        }

        if (cliParser.hasOption("input")) {
            inputs = cliParser.getOptionProperties("input");
        }

        if (cliParser.hasOption("input-strategy")) {
            inputStrategy = cliParser.getOptionValue("input-strategy").trim().toUpperCase();
        }

        if (cliParser.hasOption("output-strategy")) {
            outputStrategy = cliParser.getOptionValue("output-strategy").trim().toUpperCase();
        }

        if (cliParser.hasOption("output")) {
            outputs = cliParser.getOptionProperties("output");
        }

        if (cliParser.hasOption("s3output")) {
            s3outputs = cliParser.getOptionProperties("s3output");
        }

        if (cliParser.hasOption("user-path")) {
            userPath = cliParser.getOptionValue("user-path");
        }

        if (cliParser.hasOption("cacheFile")) {
            hboxCacheFiles = cliParser.getOptionValue("cacheFile");
        }

        if (cliParser.hasOption("cacheArchive")) {
            hboxCacheArchives = cliParser.getOptionValue("cacheArchive");
        }

        if (cliParser.hasOption("files")) {
            hboxFiles = StringUtils.split(cliParser.getOptionValue("files"), ",");
        }

        if (cliParser.hasOption("jars")) {
            libJars = StringUtils.split(cliParser.getOptionValue("jars"), ",");
        }

        if (cliParser.hasOption("hbox-cmd")) {
            hboxCmd = cliParser.getOptionValue("hbox-cmd");
        }

        if (cliParser.hasOption("launch-cmd")) {
            launchCmd = cliParser.getOptionValue("launch-cmd");
        }

        if (cliParser.hasOption("isRenameInputFile")) {
            String renameInputFile = cliParser.getOptionValue("isRenameInputFile");
            isRenameInputFile = Boolean.parseBoolean(renameInputFile);
        }

        if (cliParser.hasOption("userClasspathFirst")) {
            String classpathFirst = cliParser.getOptionValue("userClasspathFirst");
            userClasspathFirst = Boolean.parseBoolean(classpathFirst);
        }

        if (cliParser.hasOption("hostLocalEnable")) {
            String hostLocalEnableStr = cliParser.getOptionValue("hostLocalEnable");
            hostLocalEnable = Boolean.parseBoolean(hostLocalEnableStr);
        }

        if (cliParser.hasOption("create-containerid-dir")) {
            String createContaineridDirStr = cliParser.getOptionValue("create-containerid-dir");
            createContaineridDir = Boolean.parseBoolean(createContaineridDirStr);
        }

        if (cliParser.hasOption("inputformat-enable")) {
            String inputStreamStr = cliParser.getOptionValue("inputformat-enable");
            inputStream = Boolean.parseBoolean(inputStreamStr);
        }

        if (cliParser.hasOption("outputformat-enable")) {
            String outputStreamStr = cliParser.getOptionValue("outputformat-enable");
            outputStream = Boolean.parseBoolean(outputStreamStr);
        }

        if (cliParser.hasOption("inputformat-shuffle")) {
            String inputStreamShuffleStr = cliParser.getOptionValue("inputformat-shuffle");
            inputStreamShuffle = Boolean.parseBoolean(inputStreamShuffleStr);
        }

        if (cliParser.hasOption("inputformat")) {
            inputFormatClass = Class.forName(cliParser.getOptionValue("inputformat"));
        }

        if (cliParser.hasOption("outputformat")) {
            outputFormatClass = Class.forName(cliParser.getOptionValue("outputformat"));
        }

        if (cliParser.hasOption("stream-epoch")) {
            String streamEpochStr = cliParser.getOptionValue("stream-epoch");
            streamEpoch = Integer.parseInt(streamEpochStr);
        }


        if (cliParser.hasOption("board-index")) {
            String boardIndexStr = cliParser.getOptionValue("board-index");
            boardIndex = Integer.parseInt(boardIndexStr);
        }


        if (cliParser.hasOption("board-reloadinterval")) {
            String boardReloadIntervalStr = cliParser.getOptionValue("board-reloadinterval");
            boardReloadInterval = Integer.parseInt(boardReloadIntervalStr);
        }

        if (cliParser.hasOption("board-logdir")) {
            boardLogDir = cliParser.getOptionValue("board-logdir");
        }

        if (cliParser.hasOption("board-historydir")) {
            boardHistoryDir = cliParser.getOptionValue("board-historydir");
        }

        if (cliParser.hasOption("board-enable")) {
            String boardEnableStr = cliParser.getOptionValue("board-enable");
            boardEnable = Boolean.parseBoolean(boardEnableStr);
        }

        if (cliParser.hasOption("board-modelpb")) {
            boardModelPB = cliParser.getOptionValue("board-modelpb");
        }

        if (cliParser.hasOption("board-cacheTimeout")) {
            String boardCacheTimeoutStr = cliParser.getOptionValue("board-cacheTimeout");
            boardCacheTimeout = Integer.parseInt(boardCacheTimeoutStr);
        }

        if (cliParser.hasOption("output-index")) {
            outputIndex = Integer.parseInt(cliParser.getOptionValue("output-index"));
        }

        if ("TENSORFLOW".equals(appType) || "TENSOR2TENSOR".equals(appType)) {
            if (cliParser.hasOption("tf-evaluator")) {
                tfEvaluator = Boolean.parseBoolean(cliParser.getOptionValue("tf-evaluator"));
            }
            if (cliParser.hasOption("chiefworker-memory")) {
                chiefWorkerMemory = getNormalizedMem(cliParser.getOptionValue("chiefworker-memory"));
            }
            if (cliParser.hasOption("evaluatorworker-memory")) {
                evaluatorWorkerMemory = getNormalizedMem(cliParser.getOptionValue("evaluatorworker-memory"));
            }
        }
        appMasterJar = JobConf.findContainingJar(ApplicationMaster.class);
        LOG.info("Application Master's jar is " + appMasterJar);
    }

    private void printUsage(Options opts) {
        new HelpFormatter().printHelp("Client", opts);
    }

    private int getNormalizedMem(String rawMem) {
        if (rawMem.endsWith("G") || rawMem.endsWith("g")) {
            return Integer.parseInt(rawMem.substring(0, rawMem.length() - 1)) * 1024;
        } else if (rawMem.endsWith("M") || rawMem.endsWith("m")) {
            return Integer.parseInt(rawMem.substring(0, rawMem.length() - 1));
        } else {
            return Integer.parseInt(rawMem);
        }
    }

}
