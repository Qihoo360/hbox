package net.qihoo.hbox.conf;

import net.qihoo.hbox.common.TextMultiOutputFormat;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.http.HttpConfig;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.apache.hadoop.mapred.InputFormat;
import org.apache.hadoop.mapred.OutputFormat;

public class HboxConfiguration extends YarnConfiguration {

    private static final String HBOX_DEFAULT_XML_FILE = "hbox-default.xml";

    private static final String HBOX_SITE_XML_FILE = "hbox-site.xml";

    static {
        YarnConfiguration.addDefaultResource(HBOX_DEFAULT_XML_FILE);
        YarnConfiguration.addDefaultResource(HBOX_SITE_XML_FILE);
    }

    public HboxConfiguration() {
        super();
    }

    public HboxConfiguration(Configuration conf) {
        super(conf);
    }

    /**
     * Configuration used in Client
     */
    public static final String DEFAULT_HBOX_APP_TYPE = "HBOX";

    public static final String HBOX_STAGING_DIR = "hbox.staging.dir";

    public static final String DEFAULT_HBOX_STAGING_DIR = "/tmp/Hbox/staging";

    public static final String HBOX_CONTAINER_EXTRAENV = "hbox.container.extraEnv";

    public static final String HBOX_LOG_PULL_INTERVAL = "hbox.log.pull.interval";

    public static final int DEFAULT_HBOX_LOG_PULL_INTERVAL = 10000;

    public static final String HBOX_USER_CLASSPATH_FIRST = "hbox.user.classpath.first";

    public static final boolean DEFAULT_HBOX_USER_CLASSPATH_FIRST = true;

    public static final String HBOX_HOST_LOCAL_ENABLE = "hbox.host.local.enable";

    public static final boolean DEFAULT_HBOX_HOST_LOCAL_ENABLE = false;

    public static final String HBOX_AM_NODELABELEXPRESSION = "hbox.am.nodeLabelExpression";

    public static final String HBOX_WORKER_NODELABELEXPRESSION = "hbox.worker.nodeLabelExpression";

    public static final String HBOX_PS_NODELABELEXPRESSION = "hbox.ps.nodeLabelExpression";

    public static final String HBOX_REPORT_CONTAINER_STATUS = "hbox.report.container.status";

    public static final boolean DEFAULT_HBOX_REPORT_CONTAINER_STATUS = true;

    public static final String HBOX_CONTAINER_MEM_USAGE_WARN_FRACTION = "hbox.container.mem.usage.warn.fraction";

    public static final Double DEFAULT_HBOX_CONTAINER_MEM_USAGE_WARN_FRACTION = 0.70;

    public static final String HBOX_AM_MEMORY = "hbox.am.memory";

    public static final int DEFAULT_HBOX_AM_MEMORY = 1024;

    public static final String HBOX_AM_CORES = "hbox.am.cores";

    public static final int DEFAULT_HBOX_AM_CORES = 1;

    public static final String HBOX_WORKER_MEMORY = "hbox.worker.memory";

    public static final int DEFAULT_HBOX_WORKER_MEMORY = 1024;

    public static final String HBOX_WORKER_VCORES = "hbox.worker.cores";

    public static final int DEFAULT_HBOX_WORKER_VCORES = 1;

    public static final String HBOX_WORKER_NUM = "hbox.worker.num";

    public static final int DEFAULT_HBOX_WORKER_NUM = 1;

    public static final String HBOX_CHIEF_WORKER_MEMORY = "hbox.chief.worker.memory";

    public static final String HBOX_EVALUATOR_WORKER_MEMORY = "hbox.evaluator.worker.memory";

    public static final String HBOX_PS_MEMORY = "hbox.ps.memory";

    public static final int DEFAULT_HBOX_PS_MEMORY = 1024;

    public static final String HBOX_PS_VCORES = "hbox.ps.cores";

    public static final int DEFAULT_HBOX_PS_VCORES = 1;

    public static final String HBOX_PS_NUM = "hbox.ps.num";

    public static final int DEFAULT_HBOX_PS_NUM = 0;

    public static final String HBOX_WORKER_MEM_AUTO_SCALE = "hbox.worker.mem.autoscale";

    public static final Double DEFAULT_HBOX_WORKER_MEM_AUTO_SCALE = 0.5;

    public static final String HBOX_PS_MEM_AUTO_SCALE = "hbox.ps.mem.autoscale";

    public static final Double DEFAULT_HBOX_PS_MEM_AUTO_SCALE = 0.2;

    public static final String HBOX_APP_MAX_ATTEMPTS = "hbox.app.max.attempts";

    public static final int DEFAULT_HBOX_APP_MAX_ATTEMPTS = 1;

    public static final String HBOX_MODE_SINGLE = "hbox.mode.single";

    public static Boolean DEFAULT_HBOX_MODE_SINGLE = false;

    public static final String HBOX_TF_EVALUATOR = "hbox.tf.evaluator";

    public static Boolean DEFAULT_HBOX_TF_EVALUATOR = false;

    public static final String HBOX_APP_QUEUE = "hbox.app.queue";

    public static final String DEFAULT_HBOX_APP_QUEUE = "DEFAULT";

    public static final String HBOX_APP_PRIORITY = "hbox.app.priority";

    public static final int DEFAULT_HBOX_APP_PRIORITY = 3;

    public static final String HBOX_OUTPUT_LOCAL_DIR = "hbox.output.local.dir";

    public static final String DEFAULT_HBOX_OUTPUT_LOCAL_DIR = "output";

    public static final String HBOX_INPUTF0RMAT_CLASS = "hbox.inputformat.class";

    public static final Class<? extends InputFormat> DEFAULT_HBOX_INPUTF0RMAT_CLASS = org.apache.hadoop.mapred.TextInputFormat.class;

    public static final String HBOX_OUTPUTFORMAT_CLASS = "hbox.outputformat.class";

    public static final Class<? extends OutputFormat> DEFAULT_HBOX_OUTPUTF0RMAT_CLASS = TextMultiOutputFormat.class;

    public static final String HBOX_INPUTFILE_RENAME = "hbox.inputfile.rename";

    public static final Boolean DEFAULT_HBOX_INPUTFILE_RENAME = false;

    public static final String HBOX_INPUT_STRATEGY = "hbox.input.strategy";

    public static final String DEFAULT_HBOX_INPUT_STRATEGY = "DOWNLOAD";

    public static final String HBOX_OUTPUT_STRATEGY = "hbox.output.strategy";

    public static final String DEFAULT_HBOX_OUTPUT_STRATEGY = "UPLOAD";

    public static final String HBOX_STREAM_EPOCH = "hbox.stream.epoch";

    public static final int DEFAULT_HBOX_STREAM_EPOCH = 1;

    public static final String HBOX_INPUT_STREAM_SHUFFLE = "hbox.input.stream.shuffle";

    public static final Boolean DEFAULT_HBOX_INPUT_STREAM_SHUFFLE = false;

    public static final String HBOX_INPUTFORMAT_CACHESIZE_LIMIT = "hbox.inputformat.cachesize.limit";

    public static final int DEFAULT_HBOX_INPUTFORMAT_CACHESIZE_LIMIT = 100 * 1024;

    public static final String HBOX_INPUTFORMAT_CACHE = "hbox.inputformat.cache";

    public static final boolean DEFAULT_HBOX_INPUTFORMAT_CACHE = false;

    public static final String HBOX_INPUTFORMAT_CACHEFILE_NAME = "hbox.inputformat.cachefile.name";

    public static final String DEFAULT_HBOX_INPUTFORMAT_CACHEFILE_NAME = "inputformatCache.gz";

    public static final String HBOX_INTERREAULST_DIR = "hbox.interresult.dir";

    public static final String DEFAULT_HBOX_INTERRESULT_DIR = "/interResult_";

    public static final String HBOX_INTERRESULT_SAVE_INC = "hbox.interresult.save.inc";

    public static final Boolean DEFAULT_HBOX_INTERRESULT_SAVE_INC = false;


    public static final String[] DEFAULT_HBOX_APPLICATION_CLASSPATH = {
            "$HADOOP_CONF_DIR",
            "$HADOOP_COMMON_HOME/share/hadoop/common/*",
            "$HADOOP_COMMON_HOME/share/hadoop/common/lib/*",
            "$HADOOP_HDFS_HOME/share/hadoop/hdfs/*",
            "$HADOOP_HDFS_HOME/share/hadoop/hdfs/lib/*",
            "$HADOOP_YARN_HOME/share/hadoop/yarn/*",
            "$HADOOP_YARN_HOME/share/hadoop/yarn/lib/*",
            "$HADOOP_MAPRED_HOME/share/hadoop/mapreduce/*",
            "$HADOOP_MAPRED_HOME/share/hadoop/mapreduce/lib/*"
    };

    public static final String HBOX_TF_DISTRIBUTION_STRATEGY = "hbox.tf.distribution.strategy";
    public static final Boolean DEFAULT_HBOX_TF_DISTRIBUTION_STRATEGY = false;
    public static final String HBOX_TF_BOARD_PATH = "hbox.tf.board.path";
    public static final String DEFAULT_HBOX_TF_BOARD_PATH = "tensorboard";
    public static final String HBOX_TF_BOARD_WORKER_INDEX = "hbox.tf.board.worker.index";
    public static final int DEFAULT_HBOX_TF_BOARD_WORKER_INDEX = 0;
    public static final String HBOX_TF_BOARD_RELOAD_INTERVAL = "hbox.tf.board.reload.interval";
    public static final int DEFAULT_HBOX_TF_BOARD_RELOAD_INTERVAL = 1;
    public static final String HBOX_TF_BOARD_LOG_DIR = "hbox.tf.board.log.dir";
    public static final String DEFAULT_HBOX_TF_BOARD_LOG_DIR = "eventLog";
    public static final String HBOX_TF_BOARD_ENABLE = "hbox.tf.board.enable";
    public static final Boolean DEFAULT_HBOX_TF_BOARD_ENABLE = true;
    public static final String HBOX_TF_BOARD_HISTORY_DIR = "hbox.tf.board.history.dir";
    public static final String DEFAULT_HBOX_TF_BOARD_HISTORY_DIR = "/tmp/Hbox/eventLog";
    public static final String HBOX_BOARD_PATH = "hbox.board.path";
    public static final String DEFAULT_HBOX_BOARD_PATH = "visualDL";
    public static final String HBOX_BOARD_MODELPB = "hbox.board.modelpb";
    public static final String DEFAULT_HBOX_BOARD_MODELPB = "";
    public static final String HBOX_BOARD_CACHE_TIMEOUT = "hbox.board.cache.timeout";
    public static final int DEFAULT_HBOX_BOARD_CACHE_TIMEOUT = 20;
    /**
     * Configuration used in ApplicationMaster
     */
    public static final String HBOX_CONTAINER_EXTRA_JAVA_OPTS = "hbox.container.extra.java.opts";

    public static final String DEFAULT_HBOX_CONTAINER_JAVA_OPTS_EXCEPT_MEMORY = "";

    public static final String HBOX_ALLOCATE_INTERVAL = "hbox.allocate.interval";

    public static final int DEFAULT_HBOX_ALLOCATE_INTERVAL = 1000;

    public static final String HBOX_STATUS_UPDATE_INTERVAL = "hbox.status.update.interval";

    public static final int DEFAULT_HBOX_STATUS_PULL_INTERVAL = 1000;

    public static final String HBOX_TASK_TIMEOUT = "hbox.task.timeout";

    public static final int DEFAULT_HBOX_TASK_TIMEOUT = 5 * 60 * 1000;

    public static final String HBOX_LOCALRESOURCE_TIMEOUT = "hbox.localresource.timeout";

    public static final int DEFAULT_HBOX_LOCALRESOURCE_TIMEOUT = 5 * 60 * 1000;

    public static final String HBOX_TASK_TIMEOUT_CHECK_INTERVAL_MS = "hbox.task.timeout.check.interval";

    public static final int DEFAULT_HBOX_TASK_TIMEOUT_CHECK_INTERVAL_MS = 3 * 1000;

    public static final String HBOX_INTERRESULT_UPLOAD_TIMEOUT = "hbox.interresult.upload.timeout";

    public static final int DEFAULT_HBOX_INTERRESULT_UPLOAD_TIMEOUT = 50 * 60 * 1000;

    public static final String HBOX_MESSAGES_LEN_MAX = "hbox.messages.len.max";

    public static final int DEFAULT_HBOX_MESSAGES_LEN_MAX = 1000;

    public static final String HBOX_EXECUTE_NODE_LIMIT = "hbox.execute.node.limit";

    public static final int DEFAULT_HBOX_EXECUTENODE_LIMIT = 200;

    public static final String HBOX_CLEANUP_ENABLE = "hbox.cleanup.enable";

    public static final boolean DEFAULT_HBOX_CLEANUP_ENABLE = true;

    public static final String HBOX_CONTAINER_MAX_FAILURES_RATE = "hbox.container.maxFailures.rate";

    public static final double DEFAULT_HBOX_CONTAINER_FAILURES_RATE = 0.5;

    public static final String HBOX_ENV_MAXLENGTH = "hbox.env.maxlength";

    public static final Integer DEFAULT_HBOX_ENV_MAXLENGTH = 102400;

    /**
     * Configuration used in Container
     */
    public static final String HBOX_DOWNLOAD_FILE_RETRY = "hbox.download.file.retry";

    public static final int DEFAULT_HBOX_DOWNLOAD_FILE_RETRY = 3;

    public static final String HBOX_DOWNLOAD_FILE_THREAD_NUMS = "hbox.download.file.thread.nums";

    public static final int DEFAULT_HBOX_DOWNLOAD_FILE_THREAD_NUMS = 10;

    public static final String HBOX_UPLOAD_OUTPUT_THREAD_NUMS = "hbox.upload.output.thread.nums";

    public static final String HBOX_FILE_LIST_LEVEL = "hbox.file.list.level";

    public static final int DEFAULT_HBOX_FILE_LIST_LEVEL = 2;

    public static final String HBOX_CONTAINER_HEARTBEAT_INTERVAL = "hbox.container.heartbeat.interval";

    public static final int DEFAULT_HBOX_CONTAINER_HEARTBEAT_INTERVAL = 10 * 1000;

    public static final String HBOX_CONTAINER_HEARTBEAT_RETRY = "hbox.container.heartbeat.retry";

    public static final int DEFAULT_HBOX_CONTAINER_HEARTBEAT_RETRY = 3;

    public static final String HBOX_CONTAINER_UPDATE_APP_STATUS_INTERVAL = "hbox.container.update.appstatus.interval";

    public static final int DEFAULT_HBOX_CONTAINER_UPDATE_APP_STATUS_INTERVAL = 3 * 1000;

    public static final String HBOX_CONTAINER_AUTO_CREATE_OUTPUT_DIR = "hbox.container.auto.create.output.dir";

    public static final boolean DEFAULT_HBOX_CONTAINER_AUTO_CREATE_OUTPUT_DIR = true;

    public static final String HBOX_RESERVE_PORT_BEGIN = "hbox.reserve.port.begin";

    public static final int DEFAULT_HBOX_RESERVE_PORT_BEGIN = 20000;

    public static final String HBOX_RESERVE_PORT_END = "hbox.reserve.port.end";

    public static final int DEFAULT_HBOX_RESERVE_PORT_END = 30000;

    /**
     * Configuration used in Log Dir
     */
    public static final String HBOX_HISTORY_LOG_DIR = "hbox.history.log.dir";

    public static final String DEFAULT_HBOX_HISTORY_LOG_DIR = "/tmp/Hbox/history";

    public static final String HBOX_HISTORY_LOG_DELETE_MONITOR_TIME_INTERVAL = "hbox.history.log.delete-monitor-time-interval";

    public static final int DEFAULT_HBOX_HISTORY_LOG_DELETE_MONITOR_TIME_INTERVAL = 24 * 60 * 60 * 1000;

    public static final String HBOX_HISTORY_LOG_MAX_AGE_MS = "hbox.history.log.max-age-ms";

    public static final int DEFAULT_HBOX_HISTORY_LOG_MAX_AGE_MS = 24 * 60 * 60 * 1000;

    /**
     * Configuration used in Job History
     */
    public static final String HBOX_HISTORY_ADDRESS = "hbox.history.address";

    public static final String HBOX_HISTORY_PORT = "hbox.history.port";

    public static final int DEFAULT_HBOX_HISTORY_PORT = 10021;

    public static final String DEFAULT_HBOX_HISTORY_ADDRESS = "0.0.0.0:" + DEFAULT_HBOX_HISTORY_PORT;

    public static final String HBOX_HISTORY_WEBAPP_ADDRESS = "hbox.history.webapp.address";

    public static final String HBOX_HISTORY_WEBAPP_PORT = "hbox.history.webapp.port";

    public static final int DEFAULT_HBOX_HISTORY_WEBAPP_PORT = 19886;

    public static final String DEFAULT_HBOX_HISTORY_WEBAPP_ADDRESS = "0.0.0.0:" + DEFAULT_HBOX_HISTORY_WEBAPP_PORT;

    public static final String HBOX_HISTORY_WEBAPP_HTTPS_ADDRESS = "hbox.history.webapp.https.address";

    public static final String HBOX_HISTORY_WEBAPP_HTTPS_PORT = "hbox.history.webapp.https.port";

    public static final int DEFAULT_HBOX_HISTORY_WEBAPP_HTTPS_PORT = 19885;

    public static final String DEFAULT_HBOX_HISTORY_WEBAPP_HTTPS_ADDRESS = "0.0.0.0:" + DEFAULT_HBOX_HISTORY_WEBAPP_HTTPS_PORT;

    public static final String HBOX_HISTORY_BIND_HOST = "hbox.history.bind-host";

    public static final String HBOX_HISTORY_CLIENT_THREAD_COUNT = "hbox.history.client.thread-count";

    public static final int DEFAULT_HBOX_HISTORY_CLIENT_THREAD_COUNT = 10;

    public static final String HBOX_HS_RECOVERY_ENABLE = "hbox.history.recovery.enable";

    public static final boolean DEFAULT_HBOX_HS_RECOVERY_ENABLE = false;

    public static final String HBOX_HISTORY_KEYTAB = "hbox.history.keytab";

    public static final String HBOX_HISTORY_PRINCIPAL = "hbox.history.principal";

    /**
     * To enable https in HBOX history server
     */
    public static final String HBOX_HS_HTTP_POLICY = "hbox.history.http.policy";
    public static String DEFAULT_HBOX_HS_HTTP_POLICY =
            HttpConfig.Policy.HTTP_ONLY.name();

    /**
     * The kerberos principal to be used for spnego filter for history server
     */
    public static final String HBOX_WEBAPP_SPNEGO_USER_NAME_KEY = "hbox.webapp.spnego-principal";

    /**
     * The kerberos keytab to be used for spnego filter for history server
     */
    public static final String HBOX_WEBAPP_SPNEGO_KEYTAB_FILE_KEY = "hbox.webapp.spnego-keytab-file";

    //Delegation token related keys
    public static final String DELEGATION_KEY_UPDATE_INTERVAL_KEY =
            "hbox.cluster.delegation.key.update-interval";
    public static final long DELEGATION_KEY_UPDATE_INTERVAL_DEFAULT =
            24 * 60 * 60 * 1000; // 1 day
    public static final String DELEGATION_TOKEN_RENEW_INTERVAL_KEY =
            "hbox.cluster.delegation.token.renew-interval";
    public static final long DELEGATION_TOKEN_RENEW_INTERVAL_DEFAULT =
            24 * 60 * 60 * 1000;  // 1 day
    public static final String DELEGATION_TOKEN_MAX_LIFETIME_KEY =
            "hbox.cluster.delegation.token.max-lifetime";
    public static final long DELEGATION_TOKEN_MAX_LIFETIME_DEFAULT =
            24 * 60 * 60 * 1000; // 7 days

    /**
     * hbox support docker conf
     */
    public static final String HBOX_DOCKER_REGISTRY_HOST = "hbox.docker.registry.host";

    public static final String HBOX_DOCKER_REGISTRY_PORT = "hbox.docker.registry.port";

    public static final String HBOX_DOCKER_IMAGE = "hbox.docker.image";

    public static final String HBOX_CONTAINER_TYPE = "hbox.container.type";
    public static final String DEFAULT_HBOX_CONTAINER_TYPE = "yarn";

    public static final String HBOX_DOCKER_RUN_ARGS = "hbox.docker.run.args";

    public static final String HBOX_DOCKER_WORK_DIR = "hbox.docker.worker.dir";

    public static final String DEFAULT_HBOX_DOCKER_WORK_DIR = "work";

    /**
     * Configuration for mpi app
     */
    public static final String HBOX_MPI_EXTRA_LD_LIBRARY_PATH = "hbox.mpi.extra.ld.library.path";

    public static final String HBOX_MPI_INSTALL_DIR = "hbox.mpi.install.dir";
    public static final String DEFAULT_HBOX_MPI_INSTALL_DIR = "/usr/local/openmpi/";

    public static final String HBOX_MPI_CONTAINER_UPDATE_APP_STATUS_RETRY = "hbox.mpi.container.update.status.retry";
    public static final int DEFAULT_HBOX_MPI_CONTAINER_UPDATE_APP_STATUS_RETRY = 3;

}
