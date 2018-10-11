package net.qihoo.xlearning.client;

import net.qihoo.xlearning.AM.ApplicationMaster;
import net.qihoo.xlearning.common.JobPriority;
import net.qihoo.xlearning.conf.XLearningConfiguration;
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
  int amMem;
  int amCores;
  int workerMemory;
  int workerVCores;
  int workerNum;
  int psMemory;
  int psVCores;
  int psNum;
  String[] xlearningFiles;
  String[] libJars;
  String launchCmd;
  String inputStrategy;
  String outputStrategy;
  Properties inputs;
  Properties outputs;
  String xlearningCacheFiles;
  String xlearningCacheArchives;
  int priority;
  String queue;
  String userPath;
  String userLD_LIBRARY_PATH;
  String appMasterJar;
  int boardIndex;
  int boardReloadInterval;
  String boardLogDir;
  Boolean boardEnable;
  String boardHistoryDir;
  String boardModelPB;
  int boardCacheTimeout;
  Boolean isRenameInputFile;
  public Boolean tfEvaluator;
  public Boolean userClasspathFirst;
  public int streamEpoch;
  public Boolean inputStreamShuffle;
  public Class<?> inputFormatClass;
  public Class<?> outputFormatClass;
  Properties confs;
  int outputIndex;

  public ClientArguments(String[] args) throws IOException, ParseException, ClassNotFoundException {
    this.init();
    this.cliParser(args);
  }

  private void init() {
    appName = "";
    appType = XLearningConfiguration.DEFAULT_XLEARNING_APP_TYPE.toUpperCase();
    amMem = XLearningConfiguration.DEFAULT_XLEARNING_AM_MEMORY;
    amCores = XLearningConfiguration.DEFAULT_XLEARNING_AM_CORES;
    workerMemory = XLearningConfiguration.DEFAULT_XLEARNING_WORKER_MEMORY;
    workerVCores = XLearningConfiguration.DEFAULT_XLEARNING_WORKER_VCORES;
    workerNum = XLearningConfiguration.DEFAULT_XLEARNING_WORKER_NUM;
    psMemory = XLearningConfiguration.DEFAULT_XLEARNING_PS_MEMORY;
    psVCores = XLearningConfiguration.DEFAULT_XLEARNING_PS_VCORES;
    psNum = XLearningConfiguration.DEFAULT_XLEARNING_PS_NUM;
    xlearningFiles = null;
    libJars = null;
    launchCmd = "";
    xlearningCacheFiles = "";
    xlearningCacheArchives = "";
    appMasterJar = "";
    userPath = "";
    userPath = "";
    priority = XLearningConfiguration.DEFAULT_XLEARNING_APP_PRIORITY;
    queue = "";
    userClasspathFirst = XLearningConfiguration.DEFAULT_XLEARNING_USER_CLASSPATH_FIRST;
    boardIndex = XLearningConfiguration.DEFAULT_XLEARNING_TF_BOARD_WORKER_INDEX;
    boardReloadInterval = XLearningConfiguration.DEFAULT_XLEARNING_TF_BOARD_RELOAD_INTERVAL;
    boardEnable = XLearningConfiguration.DEFAULT_XLEARNING_TF_BOARD_ENABLE;
    boardLogDir = XLearningConfiguration.DEFAULT_XLEARNING_TF_BOARD_LOG_DIR;
    boardHistoryDir = XLearningConfiguration.DEFAULT_XLEARNING_TF_BOARD_HISTORY_DIR;
    boardModelPB = XLearningConfiguration.DEFAULT_XLEARNING_BOARD_MODELPB;
    boardCacheTimeout = XLearningConfiguration.DEFAULT_XLEARNING_BOARD_CACHE_TIMEOUT;
    isRenameInputFile = XLearningConfiguration.DEFAULT_XLEARNING_INPUTFILE_RENAME;
    streamEpoch = XLearningConfiguration.DEFAULT_XLEARNING_STREAM_EPOCH;
    inputStreamShuffle = XLearningConfiguration.DEFAULT_XLEARNING_INPUT_STREAM_SHUFFLE;
    inputFormatClass = XLearningConfiguration.DEFAULT_XLEARNING_INPUTF0RMAT_CLASS;
    outputFormatClass = XLearningConfiguration.DEFAULT_XLEARNING_OUTPUTF0RMAT_CLASS;
    inputStrategy = XLearningConfiguration.DEFAULT_XLEARNING_INPUT_STRATEGY.toUpperCase();
    outputStrategy = XLearningConfiguration.DEFAULT_XLEARNING_OUTPUT_STRATEGY.toUpperCase();
    tfEvaluator = XLearningConfiguration.DEFAULT_XLEARNING_TF_EVALUATOR;
    outputIndex = -1;

    allOptions = new Options();
    allOptions.addOption("appName", "app-name", true,
        "set the Application name");
    allOptions.addOption("appType", "app-type", true,
        "set the Application type, default \"XLEARNING\"");

    allOptions.addOption("amMemory", "am-memory", true,
        "Amount of memory in MB to be requested to run the application master");
    allOptions.addOption("amCores", "am-cores", true,
        "Amount of vcores to be requested to run the application master");

    allOptions.addOption("workerMemory", "worker-memory", true,
        "Amount of memory in MB to be requested to run worker");
    allOptions.addOption("workerCores", "worker-cores", true,
        "Amount of vcores to be requested to run worker");
    allOptions.addOption("workerNum", "worker-num", true,
        "No. of containers on which the worker needs to be executed");

    allOptions.addOption("psMemory", "ps-memory", true,
        "Amount of memory in MB to be requested to run ps");
    allOptions.addOption("psCores", "ps-cores", true,
        "Amount of vcores to be requested to run ps");
    allOptions.addOption("psNum", "ps-num", true,
        "No. of containers on which the ps needs to be executed");

    allOptions.addOption("files", "files", true,
        "Location of the XLearning files used in container");
    allOptions.addOption("jars", "jars", true,
        "Location of the XLearning lib jars used in container");

    allOptions.addOption("launchCmd", "launch-cmd", true, "Cmd for XLearning program");
    allOptions.addOption("userPath", "user-path", true,
        "add the user set PATH");
    allOptions.addOption("userLDLIBRARYPATH", "user-ldlibrarypath", true,
        "add the user LD_LIBRARY_PATH");
    allOptions.addOption("cacheFile", "cacheFile", true,
        "add the XLearning hdfsFile PATH");
    allOptions.addOption("cacheArchive", "cacheArchive", true,
        "add the XLearning hdfsPackage PATH");
    allOptions.addOption("priority", "priority", true, "Application Priority. Default DEFAULT");
    allOptions.addOption("queue", "queue", true,
        "RM Queue in which this application is to be submitted");
    allOptions.addOption("userClasspathFirst", "user-classpath-first", true,
        "whether user add classpath first or not, default:true");

    allOptions.addOption("boardIndex", "board-index", true,
        "if app type is tensorflow, worker index for run tensorboard, default:0");
    allOptions.addOption("boardReloadInterval", "board-reloadinterval", true,
        "if app type is tensorflow, How often the backend should load more data for tensorboard, default:1");
    allOptions.addOption("boardLogDir", "board-logdir", true,
        "if app type is tensorflow, tensorflow log dir, default:eventLog");
    allOptions.addOption("boardEnable", "board-enable", true,
        "if app type is tensorflow, enable to run tensorboard, default:true");
    allOptions.addOption("boardHistoryDir", "board-historydir", true,
        "hdfs path for board event log");
    allOptions.addOption("boardModelPB", "board-modelpb", true,
        "if app type is not tensorflow, model pb for visualDL");
    allOptions.addOption("boardCacheTimeout", "board-cacheTimeout", true,
        "if app type is not tensorflow, visualDL memory cache timeout duration in seconds, default:20");
    allOptions.addOption("isRenameInputFile", "isRenameInputFile", true,
        "whether rename the inputFiles when download from hdfs");

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
        "The output strategy for user result output, UPLOAD or STREAM, default:UPLOAD");

    allOptions.addOption("outputIndex", "output-index", true,
        "Setting the index of worker which to upload the output, default uploading the output of all the workers.");

    allOptions.addOption("tfEvaluator", "tf-evaluator", true,
        "Using the evaluator during the tensorflow distribute training.");

    allOptions.addOption("help", "help", false, "Print usage");


    OptionBuilder.withArgName("property=value");
    OptionBuilder.hasArgs(Integer.MAX_VALUE);
    OptionBuilder
        .withValueSeparator('=');
    OptionBuilder
        .withDescription("XLearning configure");
    Option conf = OptionBuilder
        .create("conf");
    allOptions.addOption(conf);

    OptionBuilder.withArgName("property#value");
    OptionBuilder.hasArgs(Integer.MAX_VALUE);
    OptionBuilder
        .withValueSeparator('#');
    OptionBuilder
        .withDescription("dfs location,representing the source data of XLearning");
    Option property = OptionBuilder
        .create("input");
    allOptions.addOption(property);

    OptionBuilder.withArgName("property#value");
    OptionBuilder.hasArgs(Integer.MAX_VALUE);
    OptionBuilder
        .withValueSeparator('#');
    OptionBuilder
        .withDescription("dfs location,representing the XLearning result");
    Option output = OptionBuilder
        .create("output");
    allOptions.addOption(output);
  }

  private void cliParser(String[] args) throws ParseException, IOException, ClassNotFoundException {
    CommandLine cliParser = new BasicParser().parse(allOptions, args);
    if (cliParser.getOptions().length == 0 || cliParser.hasOption("help")) {
      printUsage(allOptions);
      System.exit(0);
    }

    if (cliParser.hasOption("app-name")) {
      appName = cliParser.getOptionValue("app-name");
    }

    if (appName.trim().equals("")) {
      appName = "XLearning-" + System.currentTimeMillis();
    }

    if (cliParser.hasOption("app-type")) {
      appType = cliParser.getOptionValue("app-type").trim().toUpperCase();
    }

    if (!appType.equals("TENSORFLOW") && !appType.equals("MXNET") && !appType.equals("LIGHTLDA") && !appType.equals("XFLOW")) {
      psNum = 0;
    }

    if (cliParser.hasOption("conf")) {
      confs = cliParser.getOptionProperties("conf");
      if (!"TENSORFLOW".equals(appType) && !"MXNET".equals(appType) && !appType.equals("LIGHTLDA") && !appType.equals("XFLOW")) {
        if (confs.containsKey("xlearning.ps.num")) {
          confs.setProperty("xlearning.ps.num", "0");
        }
      }
    }

    if (cliParser.hasOption("am-memory")) {
      amMem = getNormalizedMem(cliParser.getOptionValue("am-memory"));
    }

    if (cliParser.hasOption("am-cores")) {
      String workerVCoresStr = cliParser.getOptionValue("am-cores");
      amCores = Integer.parseInt(workerVCoresStr);
    }

    if (cliParser.hasOption("worker-memory")) {
      workerMemory = getNormalizedMem(cliParser.getOptionValue("worker-memory"));
    }

    if (cliParser.hasOption("worker-cores")) {
      String workerVCoresStr = cliParser.getOptionValue("worker-cores");
      workerVCores = Integer.parseInt(workerVCoresStr);
    }

    if (cliParser.hasOption("worker-num")) {
      String workerNumStr = cliParser.getOptionValue("worker-num");
      workerNum = Integer.parseInt(workerNumStr);
    }

    if ("TENSORFLOW".equals(appType) || "MXNET".equals(appType) || appType.equals("LIGHTLDA") || appType.equals("XFLOW")) {
      if (cliParser.hasOption("ps-memory")) {
        psMemory = getNormalizedMem(cliParser.getOptionValue("ps-memory"));
      }

      if (cliParser.hasOption("ps-cores")) {
        String psVCoresStr = cliParser.getOptionValue("ps-cores");
        psVCores = Integer.parseInt(psVCoresStr);
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

    if (cliParser.hasOption("user-path")) {
      userPath = cliParser.getOptionValue("user-path");
    }

    if (cliParser.hasOption("user-ldlibrarypath")) {
      userLD_LIBRARY_PATH = cliParser.getOptionValue("user-ldlibrarypath");
    }

    if (cliParser.hasOption("cacheFile")) {
      xlearningCacheFiles = cliParser.getOptionValue("cacheFile");
    }

    if (cliParser.hasOption("cacheArchive")) {
      xlearningCacheArchives = cliParser.getOptionValue("cacheArchive");
    }

    if (cliParser.hasOption("files")) {
      xlearningFiles = StringUtils.split(cliParser.getOptionValue("files"), ",");
    }

    if (cliParser.hasOption("jars")) {
      libJars = StringUtils.split(cliParser.getOptionValue("jars"), ",");
    }

    if (cliParser.hasOption("userClasspathFirst")) {
      String classpathFirst = cliParser.getOptionValue("userClasspathFirst");
      userClasspathFirst = Boolean.parseBoolean(classpathFirst);
    }

    if (cliParser.hasOption("launch-cmd")) {
      launchCmd = cliParser.getOptionValue("launch-cmd");
    }

    if (cliParser.hasOption("isRenameInputFile")) {
      String renameInputFile = cliParser.getOptionValue("isRenameInputFile");
      isRenameInputFile = Boolean.parseBoolean(renameInputFile);
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

    if (cliParser.hasOption("tf-evaluator")){
      tfEvaluator = Boolean.parseBoolean(cliParser.getOptionValue("tf-evaluator"));
    }

    if (cliParser.hasOption("output-index")) {
      outputIndex = Integer.parseInt(cliParser.getOptionValue("output-index"));
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
