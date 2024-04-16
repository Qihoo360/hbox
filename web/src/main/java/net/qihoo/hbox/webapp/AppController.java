package net.qihoo.hbox.webapp;

import com.google.gson.*;

import com.google.inject.Inject;
import net.qihoo.hbox.api.HboxConstants;
import net.qihoo.hbox.common.OutputInfo;
import net.qihoo.hbox.common.AMParams;
import net.qihoo.hbox.conf.HboxConfiguration;
import net.qihoo.hbox.container.HboxContainerId;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.util.StringUtils;
import org.apache.hadoop.yarn.api.records.Container;
import org.apache.hadoop.yarn.webapp.Controller;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;

import static org.apache.hadoop.yarn.util.StringHelper.join;

public class AppController extends Controller implements AMParams {

    private final Configuration conf;
    private final App app;

    @Inject
    public AppController(App app, Configuration conf, RequestContext ctx) {
        super(ctx);
        this.conf = conf;
        this.app = app;
        set(APP_ID, app.context.getApplicationID().toString());
        if (System.getenv().containsKey(HboxConstants.Environment.HBOX_APP_TYPE.toString())) {
            if ("hbox".equals(System.getenv(HboxConstants.Environment.HBOX_APP_TYPE.toString()).toLowerCase())) {
                set(APP_TYPE, "HBox");
            } else {
                char[] appType = System.getenv(HboxConstants.Environment.HBOX_APP_TYPE.toString()).toLowerCase().toCharArray();
                appType[0] -= 32;
                set(APP_TYPE, String.valueOf(appType));
            }
        } else {
            set(APP_TYPE, "HBox");
        }

        if ($(APP_TYPE).equals("Vpc") || $(APP_TYPE).equals("Digits") || $(APP_TYPE).equals("Distlightlda")) {
            set(BOARD_INFO, "no");
        } else {
            String boardUrl = app.context.getTensorBoardUrl();
            if (this.conf.getBoolean(HboxConfiguration.HBOX_TF_BOARD_ENABLE, HboxConfiguration.DEFAULT_HBOX_TF_BOARD_ENABLE)) {
                if (boardUrl != null) {
                    set(BOARD_INFO, boardUrl);
                } else {
                    set(BOARD_INFO, "Waiting for board process start...");
                }
            } else {
                String boardInfo = "Board server don't start, You can set argument \"--board-enable true\" in your submit script to start.";
                set(BOARD_INFO, boardInfo);
            }
        }

        List<Container> workerContainers = app.context.getWorkerContainers();
        List<Container> psContainers = app.context.getPsContainers();
        Map<HboxContainerId, String> reporterProgress = app.context.getReporterProgress();
        Map<HboxContainerId, String> containersAppStartTime = app.context.getContainersAppStartTime();
        Map<HboxContainerId, String> containersAppFinishTime = app.context.getContainersAppFinishTime();
        int workerGcores = app.context.getWorkerGcores();
        int psGcores = app.context.getPsGcores();
        int workerMemory = app.context.getWorkerMemory();
        int psMemory = app.context.getPsMemory();
        int workerVCores = app.context.getWorkerVCores();
        int psVCores = app.context.getPsVCores();

        set(CONTAINER_NUMBER, String.valueOf(workerContainers.size() + psContainers.size()));
        set(WORKER_NUMBER, String.valueOf(workerContainers.size()));
        set(PS_NUMBER, String.valueOf(psContainers.size()));
        set(USER_NAME, StringUtils.split(conf.get("hadoop.job.ugi"), ',')[0]);
        set("WORKER_GCORES", String.valueOf(workerGcores));
        set("PS_GCORES", String.valueOf(psGcores));
        set(WORKER_MEMORY, String.format("%.2f", workerMemory / 1024.0));
        set(PS_MEMORY, String.format("%.2f", psMemory / 1024.0));
        set("chiefWorkerMemory", "");
        set("evaluatorWorkerMemory", "");
        if (app.context.getChiefWorker()) {
            set("chiefWorkerMemory", String.format("%.2f", app.context.getChiefWorkerMemory() / 1024.0));
        }
        if (conf.getBoolean(HboxConfiguration.HBOX_TF_EVALUATOR, HboxConfiguration.DEFAULT_HBOX_TF_EVALUATOR)) {
            set("evaluatorWorkerMemory", String.format("%.2f", app.context.getEvaluatorWorkerMemory() / 1024.0));
        }
        set(WORKER_VCORES, String.valueOf(workerVCores));
        set(PS_VCORES, String.valueOf(psVCores));
        int i = 0;
        for (Container container : workerContainers) {
            set(CONTAINER_HTTP_ADDRESS + i, container.getNodeHttpAddress());
            set(CONTAINER_ID + i, container.getId().toString());
            if (app.context.getContainerStatus(new HboxContainerId(container.getId())) != null) {
                set(CONTAINER_STATUS + i, app.context.getContainerStatus(new HboxContainerId(container.getId())).toString());
            } else {
                set(CONTAINER_STATUS + i, "-");
            }
            if (conf.getBoolean(HboxConfiguration.HBOX_TF_EVALUATOR, HboxConfiguration.DEFAULT_HBOX_TF_EVALUATOR) && container.getId().toString().equals(app.context.getTfEvaluatorId())) {
                set(CONTAINER_ROLE + i, HboxConstants.WORKER + "/" + HboxConstants.EVALUATOR);
            } else if (app.context.getChiefWorker() && container.getId().toString().equals(app.context.getChiefWorkerId())) {
                set(CONTAINER_ROLE + i, HboxConstants.WORKER + "/" + HboxConstants.CHIEF);
            } else {
                set(CONTAINER_ROLE + i, HboxConstants.WORKER);
            }
            if (app.context.getContainerGPUDevice(new HboxContainerId(container.getId())) != null) {
                if (app.context.getContainerGPUDevice(new HboxContainerId(container.getId())).trim().length() != 0) {
                    set(CONTAINER_GPU_DEVICE + i, app.context.getContainerGPUDevice(new HboxContainerId(container.getId())).toString());
                    ConcurrentHashMap<String, LinkedBlockingDeque<List<Long>>> containersGpuMemMetrics = app.context.getContainersGpuMemMetrics().get(new HboxContainerId(container.getId()));
                    ConcurrentHashMap<String, LinkedBlockingDeque<List<Long>>> containersGpuUtilMetrics = app.context.getContainersGpuUtilMetrics().get(new HboxContainerId(container.getId()));
                    if (containersGpuMemMetrics.size() != 0) {
                        for (String str : containersGpuMemMetrics.keySet()) {
                            set("gpuMemMetrics" + i + str, new Gson().toJson(containersGpuMemMetrics.get(str)));
                        }
                    }
                    if (containersGpuUtilMetrics.size() != 0) {
                        for (String str : containersGpuUtilMetrics.keySet()) {
                            set("gpuUtilMetrics" + i + str, new Gson().toJson(containersGpuUtilMetrics.get(str)));
                        }
                    }

                    ConcurrentHashMap<String, List<Double>> containersGpuMemStatistics = app.context.getContainersGpuMemStatistics().get(new HboxContainerId(container.getId()));
                    if (containersGpuMemStatistics.size() != 0) {
                        for (String str : containersGpuMemStatistics.keySet()) {
                            set(GPU_USAGE_TYPE + CONTAINER_MEM_USAGE_STATISTICS + USAGE_AVG + i + str, String.format("%.2f", containersGpuMemStatistics.get(str).get(0)));
                            set(GPU_USAGE_TYPE + CONTAINER_MEM_USAGE_STATISTICS + USAGE_MAX + i + str, String.format("%.2f", containersGpuMemStatistics.get(str).get(1)));
                        }
                    }

                    ConcurrentHashMap<String, List<Double>> containersGpuUtilStatistics = app.context.getContainersGpuUtilStatistics().get(new HboxContainerId(container.getId()));
                    if (containersGpuUtilStatistics.size() != 0) {
                        for (String str : containersGpuUtilStatistics.keySet()) {
                            set(GPU_USAGE_TYPE + CONTAINER_UTIL_USAGE_STATISTICS + USAGE_AVG + i + str, String.format("%.2f", containersGpuUtilStatistics.get(str).get(0)));
                            set(GPU_USAGE_TYPE + CONTAINER_UTIL_USAGE_STATISTICS + USAGE_MAX + i + str, String.format("%.2f", containersGpuUtilStatistics.get(str).get(1)));
                        }
                    }

                } else {
                    set(CONTAINER_GPU_DEVICE + i, "-");
                }
            } else {
                set(CONTAINER_GPU_DEVICE + i, "-");
            }
            if (app.context.getContainersCpuMetrics().get(new HboxContainerId(container.getId())) != null) {
                ConcurrentHashMap<String, LinkedBlockingDeque<Object>> cpuMetrics = app.context.getContainersCpuMetrics().get(new HboxContainerId(container.getId()));
                if (cpuMetrics.size() != 0) {
                    set("cpuMemMetrics" + i, new Gson().toJson(cpuMetrics.get("CPUMEM")));
                    set("cpuUtilMetrics" + i, new Gson().toJson(cpuMetrics.get("CPUUTIL")));
                }
            }
            if (app.context.getContainersCpuStatistics().get(new HboxContainerId(container.getId())) != null) {
                ConcurrentHashMap<String, List<Double>> cpuStatistics = app.context.getContainersCpuStatistics().get(new HboxContainerId(container.getId()));
                if (cpuStatistics.size() != 0) {
                    set(CPU_USAGE_TYPE + CONTAINER_MEM_USAGE_STATISTICS + USAGE_AVG + i, String.format("%.2f", cpuStatistics.get("CPUMEM").get(0)));
                    set(CPU_USAGE_TYPE + CONTAINER_MEM_USAGE_STATISTICS + USAGE_MAX + i, String.format("%.2f", cpuStatistics.get("CPUMEM").get(1)));
                    set(CPU_USAGE_TYPE + CONTAINER_UTIL_USAGE_STATISTICS + USAGE_AVG + i, String.format("%.2f", cpuStatistics.get("CPUUTIL").get(0)));
                    set(CPU_USAGE_TYPE + CONTAINER_UTIL_USAGE_STATISTICS + USAGE_MAX + i, String.format("%.2f", cpuStatistics.get("CPUUTIL").get(1)));
                }
            }

            if (reporterProgress.get(new HboxContainerId(container.getId())) != null && !reporterProgress.get(new HboxContainerId(container.getId())).equals("")) {
                String progressLog = reporterProgress.get(new HboxContainerId(container.getId()));
                String[] progress = progressLog.toString().split(":");
                if (progress.length != 2) {
                    set(CONTAINER_REPORTER_PROGRESS + i, "progress log format error");
                } else {
                    try {
                        Float percentProgress = Float.parseFloat(progress[1]);
                        if (percentProgress < 0.0 || percentProgress > 1.0) {
                            set(CONTAINER_REPORTER_PROGRESS + i, "progress log format error");
                        } else {
                            DecimalFormat df = new DecimalFormat("0.00");
                            df.setRoundingMode(RoundingMode.HALF_UP);
                            set(CONTAINER_REPORTER_PROGRESS + i, df.format((Float.parseFloat(progress[1]) * 100)) + "%");
                        }
                    } catch (Exception e) {
                        set(CONTAINER_REPORTER_PROGRESS + i, "progress log format error");
                    }
                }
                //set(CONTAINER_REPORTER_PROGRESS + i, Float.toString((Float.parseFloat(progress[1])*100)) + "%");
            } else {
                set(CONTAINER_REPORTER_PROGRESS + i, "0.00%");
            }
            if (containersAppStartTime.get(new HboxContainerId(container.getId())) != null && !containersAppStartTime.get(new HboxContainerId(container.getId())).equals("")) {
                String localStartTime = containersAppStartTime.get(new HboxContainerId(container.getId()));
                set(CONTAINER_START_TIME + i, localStartTime);
            } else {
                set(CONTAINER_START_TIME + i, "N/A");
            }
            if (containersAppFinishTime.get(new HboxContainerId(container.getId())) != null && !containersAppFinishTime.get(new HboxContainerId(container.getId())).equals("")) {
                String localFinishTime = containersAppFinishTime.get(new HboxContainerId(container.getId()));
                set(CONTAINER_FINISH_TIME + i, localFinishTime);
            } else {
                set(CONTAINER_FINISH_TIME + i, "N/A");
            }
            i++;
        }
        for (Container container : psContainers) {
            set(CONTAINER_HTTP_ADDRESS + i, container.getNodeHttpAddress());
            set(CONTAINER_ID + i, container.getId().toString());
            if (app.context.getContainerStatus(new HboxContainerId(container.getId())) != null) {
                set(CONTAINER_STATUS + i, app.context.getContainerStatus(new HboxContainerId(container.getId())).toString());
            } else {
                set(CONTAINER_STATUS + i, "-");
            }
            if ($(APP_TYPE).equals("Tensorflow")) {
                set(CONTAINER_ROLE + i, "ps");
            } else if ($(APP_TYPE).equals("Mxnet") || $(APP_TYPE).equals("Distlightlda") || $(APP_TYPE).equals("Xflow")) {
                set(CONTAINER_ROLE + i, "server");
            } else if ($(APP_TYPE).equals("Xdl")) {
                if (container.getId().toString().equals(app.context.getSchedulerId())) {
                    set(CONTAINER_ROLE + i, HboxConstants.SCHEDULER);
                } else {
                    set(CONTAINER_ROLE + i, HboxConstants.PS);
                }
            }

            if (app.context.getContainerGPUDevice(new HboxContainerId(container.getId())) != null) {
                if (app.context.getContainerGPUDevice(new HboxContainerId(container.getId())).trim().length() != 0) {
                    set(CONTAINER_GPU_DEVICE + i, app.context.getContainerGPUDevice(new HboxContainerId(container.getId())).toString());
                    ConcurrentHashMap<String, LinkedBlockingDeque<List<Long>>> containersGpuMemMetrics = app.context.getContainersGpuMemMetrics().get(new HboxContainerId(container.getId()));
                    ConcurrentHashMap<String, LinkedBlockingDeque<List<Long>>> containersGpuUtilMetrics = app.context.getContainersGpuUtilMetrics().get(new HboxContainerId(container.getId()));
                    if (containersGpuMemMetrics.size() != 0) {
                        for (String str : containersGpuMemMetrics.keySet()) {
                            set("gpuMemMetrics" + i + str, new Gson().toJson(containersGpuMemMetrics.get(str)));
                        }
                    }
                    if (containersGpuUtilMetrics.size() != 0) {
                        for (String str : containersGpuUtilMetrics.keySet()) {
                            set("gpuUtilMetrics" + i + str, new Gson().toJson(containersGpuUtilMetrics.get(str)));
                        }
                    }

                    ConcurrentHashMap<String, List<Double>> containersGpuMemStatistics = app.context.getContainersGpuMemStatistics().get(new HboxContainerId(container.getId()));
                    if (containersGpuMemStatistics.size() != 0) {
                        for (String str : containersGpuMemStatistics.keySet()) {
                            set(GPU_USAGE_TYPE + CONTAINER_MEM_USAGE_STATISTICS + USAGE_AVG + i + str, String.format("%.2f", containersGpuMemStatistics.get(str).get(0)));
                            set(GPU_USAGE_TYPE + CONTAINER_MEM_USAGE_STATISTICS + USAGE_MAX + i + str, String.format("%.2f", containersGpuMemStatistics.get(str).get(1)));
                        }
                    }

                    ConcurrentHashMap<String, List<Double>> containersGpuUtilStatistics = app.context.getContainersGpuUtilStatistics().get(new HboxContainerId(container.getId()));
                    if (containersGpuUtilStatistics.size() != 0) {
                        for (String str : containersGpuUtilStatistics.keySet()) {
                            set(GPU_USAGE_TYPE + CONTAINER_UTIL_USAGE_STATISTICS + USAGE_AVG + i + str, String.format("%.2f", containersGpuUtilStatistics.get(str).get(0)));
                            set(GPU_USAGE_TYPE + CONTAINER_UTIL_USAGE_STATISTICS + USAGE_MAX + i + str, String.format("%.2f", containersGpuUtilStatistics.get(str).get(1)));
                        }
                    }
                } else {
                    set(CONTAINER_GPU_DEVICE + i, "-");
                }
            } else {
                set(CONTAINER_GPU_DEVICE + i, "-");
            }
            if (app.context.getContainersCpuMetrics().get(new HboxContainerId(container.getId())) != null) {
                ConcurrentHashMap<String, LinkedBlockingDeque<Object>> cpuMetrics = app.context.getContainersCpuMetrics().get(new HboxContainerId(container.getId()));
                if (cpuMetrics.size() != 0) {
                    set("cpuMemMetrics" + i, new Gson().toJson(cpuMetrics.get("CPUMEM")));
                    set("cpuUtilMetrics" + i, new Gson().toJson(cpuMetrics.get("CPUUTIL")));
                }
            }
            if (app.context.getContainersCpuStatistics().get(new HboxContainerId(container.getId())) != null) {
                ConcurrentHashMap<String, List<Double>> cpuStatistics = app.context.getContainersCpuStatistics().get(new HboxContainerId(container.getId()));
                if (cpuStatistics.size() != 0) {
                    set(CPU_USAGE_TYPE + CONTAINER_MEM_USAGE_STATISTICS + USAGE_AVG + i, String.format("%.2f", cpuStatistics.get("CPUMEM").get(0)));
                    set(CPU_USAGE_TYPE + CONTAINER_MEM_USAGE_STATISTICS + USAGE_MAX + i, String.format("%.2f", cpuStatistics.get("CPUMEM").get(1)));
                    set(CPU_USAGE_TYPE + CONTAINER_UTIL_USAGE_STATISTICS + USAGE_AVG + i, String.format("%.2f", cpuStatistics.get("CPUUTIL").get(0)));
                    set(CPU_USAGE_TYPE + CONTAINER_UTIL_USAGE_STATISTICS + USAGE_MAX + i, String.format("%.2f", cpuStatistics.get("CPUUTIL").get(1)));
                }
            }
            set(CONTAINER_REPORTER_PROGRESS + i, "0.00%");
            if (containersAppStartTime.get(new HboxContainerId(container.getId())) != null && !containersAppStartTime.get(new HboxContainerId(container.getId())).equals("")) {
                String localStartTime = containersAppStartTime.get(new HboxContainerId(container.getId()));
                set(CONTAINER_START_TIME + i, localStartTime);
            } else {
                set(CONTAINER_START_TIME + i, "N/A");
            }
            if (containersAppFinishTime.get(new HboxContainerId(container.getId())) != null && !containersAppFinishTime.get(new HboxContainerId(container.getId())).equals("")) {
                String localFinishTime = containersAppFinishTime.get(new HboxContainerId(container.getId()));
                set(CONTAINER_FINISH_TIME + i, localFinishTime);
            } else {
                set(CONTAINER_FINISH_TIME + i, "N/A");
            }
            i++;
        }

        set(OUTPUT_TOTAL, String.valueOf(app.context.getOutputs().size()));
        i = 0;
        for (OutputInfo output : app.context.getOutputs()) {
            Path interResult = new Path(output.getDfsLocation()
                    + conf.get(HboxConfiguration.HBOX_INTERRESULT_DIR, HboxConfiguration.DEFAULT_HBOX_INTERRESULT_DIR));
            set(OUTPUT_PATH + i, interResult.toString());
            i++;
        }

        set(TIMESTAMP_TOTAL, String.valueOf(app.context.getModelSavingList().size()));
        int j = 0;
        for (i = app.context.getModelSavingList().size(); i > 0; i--) {
            set(TIMESTAMP_LIST + j, String.valueOf(app.context.getModelSavingList().get(i - 1)));
            j++;
        }
    }

    @Override
    public void index() {
        setTitle(join($(APP_TYPE) + " Application ", $(APP_ID)));
        if (app.context.getLastSavingStatus() && app.context.getStartSavingStatus() && app.context.getSavingModelStatus() == app.context.getSavingModelTotalNum()) {
            app.context.startSavingModelStatus(false);
        }
        set(SAVE_MODEL, String.valueOf(app.context.getStartSavingStatus()));
        set(SAVE_MODEL_STATUS, String.valueOf(app.context.getSavingModelStatus()));
        set(LAST_SAVE_STATUS, String.valueOf(app.context.getLastSavingStatus()));
        set(SAVE_MODEL_TOTAL, String.valueOf(app.context.getSavingModelTotalNum()));
        render(InfoPage.class);
    }

    public void savedmodel() {
        setTitle(join($(APP_TYPE) + " Application ", $(APP_ID)));
        set(SAVE_MODEL_STATUS, String.valueOf(app.context.getSavingModelStatus()));
        set(SAVE_MODEL_TOTAL, String.valueOf(app.context.getSavingModelTotalNum()));
        if (!app.context.getStartSavingStatus()) {
            app.context.startSavingModelStatus(true);
            set(SAVE_MODEL, String.valueOf(app.context.getStartSavingStatus()));
        } else {
            set(SAVE_MODEL, String.valueOf(app.context.getStartSavingStatus()));
        }
        render(InfoPage.class);
    }
}
