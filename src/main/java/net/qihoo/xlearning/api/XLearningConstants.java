package net.qihoo.xlearning.api;

import org.apache.hadoop.classification.InterfaceAudience;
import org.apache.hadoop.classification.InterfaceStability;
import org.apache.hadoop.util.Shell;
import org.apache.hadoop.yarn.api.ApplicationConstants;

@InterfaceAudience.Public
@InterfaceStability.Evolving
public interface XLearningConstants {

  String XLEARNING_JOB_CONFIGURATION = "core-site.xml";

  String XLEARNING_APPLICATION_JAR = "AppMaster.jar";

  String WORKER = "worker";

  String PS = "ps";

  String STREAM_INPUT_DIR = "mapreduce.input.fileinputformat.inputdir";

  String STREAM_OUTPUT_DIR = "mapreduce.output.fileoutputformat.outputdir";

  enum Environment {
    HADOOP_USER_NAME("HADOOP_USER_NAME"),

    XLEARNING_APP_TYPE("XLEARNING_APP_TYPE"),

    XLEARNING_CONTAINER_MAX_MEMORY("XLEARNING_MAX_MEM"),

    XLEARNING_TF_ROLE("TF_ROLE"),

    XLEARNING_TF_INDEX("TF_INDEX"),

    XLEARNING_TF_CLUSTER_DEF("TF_CLUSTER_DEF"),

    XLEARNING_MXNET_WORKER_NUM("DMLC_NUM_WORKER"),

    XLEARNING_MXNET_SERVER_NUM("DMLC_NUM_SERVER"),

    XLEARNING_INPUT_FILE_LIST("INPUT_FILE_LIST"),

    XLEARNING_STAGING_LOCATION("XLEARNING_STAGING_LOCATION"),

    XLEARNING_CACHE_FILE_LOCATION("XLEARNING_CACHE_FILE_LOCATION"),

    XLEARNING_CACHE_ARCHIVE_LOCATION("XLEARNING_CACHE_ARCHIVE_LOCATION"),

    XLEARNING_FILES_LOCATION("XLEARNING_FILES_LOCATION"),

    APP_JAR_LOCATION("APP_JAR_LOCATION"),

    XLEARNING_JOB_CONF_LOCATION("XLEARNING_JOB_CONF_LOCATION"),

    XLEARNING_EXEC_CMD("XLEARNING_EXEC_CMD"),

    USER_PATH("USER_PATH"),

    XLEARNING_OUTPUTS("XLEARNING_OUTPUTS"),

    XLEARNING_INPUTS("XLEARNING_INPUTS"),

    APPMASTER_HOST("APPMASTER_HOST"),

    APPMASTER_PORT("APPMASTER_PORT"),

    APP_ID("APP_ID"),

    APP_ATTEMPTID("APP_ATTEMPTID");

    private final String variable;

    Environment(String variable) {
      this.variable = variable;
    }

    public String key() {
      return variable;
    }

    public String toString() {
      return variable;
    }

    public String $() {
      if (Shell.WINDOWS) {
        return "%" + variable + "%";
      } else {
        return "$" + variable;
      }
    }

    @InterfaceAudience.Public
    @InterfaceStability.Unstable
    public String $$() {
      return ApplicationConstants.PARAMETER_EXPANSION_LEFT +
          variable +
          ApplicationConstants.PARAMETER_EXPANSION_RIGHT;
    }
  }
}
