package net.qihoo.hbox.api;

import org.apache.hadoop.classification.InterfaceAudience;
import org.apache.hadoop.classification.InterfaceStability;
import org.apache.hadoop.util.Shell;
import org.apache.hadoop.yarn.api.ApplicationConstants;

@InterfaceAudience.Public
@InterfaceStability.Evolving
public interface HboxConstants {

    String HBOX_JOB_CONFIGURATION = "core-site.xml";

    String HBOX_APPLICATION_JAR = "AppMaster.jar";

    String WORKER = "worker";

    String PS = "ps";

    String EVALUATOR = "evaluator";

    String CHIEF = "chief";

    String SCHEDULER = "scheduler";

    String STREAM_INPUT_DIR = "mapreduce.input.fileinputformat.inputdir";

    String STREAM_OUTPUT_DIR = "mapreduce.output.fileoutputformat.outputdir";

    String AM_ENV_PREFIX = "hbox.am.env.";

    String CONTAINER_ENV_PREFIX = "hbox.container.env.";

    String HDFS = "hdfs";

    String S3 = "s3";

    enum Environment {
        HADOOP_USER_NAME("HADOOP_USER_NAME"),

        HBOX_APP_TYPE("HBOX_APP_TYPE"),

        HBOX_APP_NAME("HBOX_APP_NAME"),

        HBOX_DOCKER_CONTAINER_EXECUTOR_IMAGE_NAME("yarn.nodemanager.docker-container-executor.image-name"),

        HBOX_DOCKER_CONTAINER_EXECUTOR_EXEC_NAME("yarn.nodemanager.docker-container-executor.exec-name"),

        HBOX_CONTAINER_EXECUTOR_TYPE("yarn.nodemanager.container-executor.type"),

        HBOX_CONTAINER_MAX_MEMORY("HBOX_MAX_MEM"),

        HBOX_TF_ROLE("TF_ROLE"),

        HBOX_TF_INDEX("TF_INDEX"),

        HBOX_OUTPUT_INDEX("HBOX_OUTPUT_INDEX"),

        HBOX_TF_CLUSTER_DEF("TF_CLUSTER_DEF"),

        HBOX_TF_CONFIG("TF_CONFIG"),

        HBOX_LOCAL_ADDRESS("LOCAL_ADDRESS"),

        HBOX_DMLC_WORKER_NUM("DMLC_NUM_WORKER"),

        HBOX_DMLC_SERVER_NUM("DMLC_NUM_SERVER"),

        HBOX_LIGHTGBM_WORKER_NUM("LIGHTGBM_NUM_WORKER"),

        HBOX_LIGHTLDA_WORKER_NUM("LIGHTLDA_NUM_WORKER"),

        HBOX_LIGHTLDA_PS_NUM("LIGHTLDA_NUM_PS"),

        HBOX_CUDA_VISIBLE_DEVICES("CUDA_VISIBLE_DEVICES"),

        HBOX_CUDA_VISIBLE_DEVICES_NUM("GPU_NUM"),

        HBOX_INPUT_FILE_LIST("INPUT_FILE_LIST"),

        HBOX_S3_INPUT_FILE_LIST("INPUT_FILE_LIST"),

        HBOX_INPUT_PATH("INPUT_PATH"),

        HBOX_STAGING_LOCATION("HBOX_STAGING_LOCATION"),

        HBOX_CACHE_FILE_LOCATION("HBOX_CACHE_FILE_LOCATION"),

        HBOX_CACHE_ARCHIVE_LOCATION("HBOX_CACHE_ARCHIVE_LOCATION"),

        HBOX_FILES_LOCATION("HBOX_FILES_LOCATION"),

        HBOX_LIBJARS_LOCATION("HBOX_LIBJARS_LOCATION"),

        APP_JAR_LOCATION("APP_JAR_LOCATION"),

        HBOX_JOB_CONF_LOCATION("HBOX_JOB_CONF_LOCATION"),

        HBOX_EXEC_CMD("HBOX_EXEC_CMD"),

        USER_PATH("USER_PATH"),

        HBOX_OUTPUTS("HBOX_OUTPUTS"),

        HBOX_S3_OUTPUTS("HBOX_S3_OUTPUTS"),

        HBOX_INPUTS("HBOX_INPUTS"),

        HBOX_S3_INPUTS("HBOX_S3_INPUTS"),

        CONTAINER_COMMAND("CONTAINER_COMMAND"),

        MPI_EXEC_DIR("MPI_EXEC_DIR"),

        MPI_FILES_LINKS("LINKS"),

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
