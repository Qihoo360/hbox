package net.qihoo.hbox.webapp;

public interface AMParams {
    public static final String APP_TYPE = "app.type";

    public static final String APP_ID = "app.id";

    public static final String USER_NAME = "user.name";

    public static final String WORKER_NUMBER = "worker.number";

    public static final String PS_NUMBER = "ps.number";

    public static final String WORKER_VCORES = "worker.vcores";

    public static final String PS_VCORES = "ps.vcores";

    public static final String WORKER_MEMORY = "worker.memory";

    public static final String CHIEF_WORKER_MEMORY = "chief.worker.memory";

    public static final String EVALUATOR_WORKER_MEMORY = "evaluator.worker.memory";

    public static final String PS_MEMORY = "ps.memory";

    public static final String CONTAINER_HTTP_ADDRESS = "container.address";

    public static final String CONTAINER_ID = "container.id";

    public static final String CONTAINER_NUMBER = "container.number";

    public static final String CONTAINER_STATUS = "container.status";

    public static final String CONTAINER_ROLE = "container.role";

    public static final String CONTAINER_LOG_ADDRESS = "container.log.address";

    public static final String CONTAINER_CPU_METRICS_ENABLE = "container.cpu.metrics.enable";

    public static final String CONTAINER_CPU_METRICS = "container.cpu.metrics";

    public static final String CONTAINER_CPU_STATISTICS = "container.cpu.statistics";

    public static final String CONTAINER_CPU_USAGE = "container.cpu.usage";

    public static final String CONTAINER_CPU_USAGE_WARN_MEM = CONTAINER_CPU_USAGE + ".warn.mem";

    public static final String CONTAINER_CPU_STATISTICS_MEM = CONTAINER_CPU_STATISTICS + ".mem";

    public static final String CONTAINER_CPU_STATISTICS_UTIL = CONTAINER_CPU_STATISTICS + ".util";

    public static final String USAGE_AVG = "Avg";

    public static final String USAGE_MAX = "Max";

    public static final String BOARD_INFO = "board.info";

    public static final String BOARD_INFO_FLAG = "board.info.flag";

    public static final String SAVE_MODEL = "save.model";

    public static final String SAVE_MODEL_STATUS = "save.model.status";

    public static final String SAVE_MODEL_TOTAL = "save.model.total";

    public static final String LAST_SAVE_STATUS = "last.save.status";

    public static final String OUTPUT_PATH = "output.path";

    public static final String OUTPUT_TOTAL = "output.total";

    public static final String TIMESTAMP_LIST = "timestamp.list";

    public static final String TIMESTAMP_TOTAL = "timestamp.total";

    public static final String CONTAINER_REPORTER_PROGRESS = "container.reporter.progress";

    public static final String CONTAINER_START_TIME = "container.start.time";

    public static final String CONTAINER_FINISH_TIME = "container.finish.time";

}
