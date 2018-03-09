package net.qihoo.xlearning.webapp;

import com.google.gson.*;
import com.google.inject.Inject;
import net.qihoo.xlearning.api.XLearningConstants;
import net.qihoo.xlearning.common.OutputInfo;
import net.qihoo.xlearning.conf.XLearningConfiguration;
import net.qihoo.xlearning.container.XLearningContainerId;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.yarn.api.records.Container;
import org.apache.hadoop.yarn.webapp.Controller;
import org.apache.hadoop.yarn.webapp.WebApp;
import org.apache.hadoop.yarn.webapp.WebApps;

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
    if (System.getenv().containsKey(XLearningConstants.Environment.XLEARNING_APP_TYPE.toString())) {
      if ("xlearning".equals(System.getenv(XLearningConstants.Environment.XLEARNING_APP_TYPE.toString()).toLowerCase())) {
        set(APP_TYPE, "XLearning");
      } else {
        char[] appType = System.getenv(XLearningConstants.Environment.XLEARNING_APP_TYPE.toString()).toLowerCase().toCharArray();
        appType[0] -= 32;
        set(APP_TYPE, String.valueOf(appType));
      }
    } else {
      set(APP_TYPE, "XLearning");
    }

    String boardUrl = app.context.getTensorBoardUrl();
    if (this.conf.getBoolean(XLearningConfiguration.XLEARNING_TF_BOARD_ENABLE, XLearningConfiguration.DEFAULT_XLEARNING_TF_BOARD_ENABLE)) {
      if (boardUrl != null) {
        set(BOARD_INFO, boardUrl);
      } else {
        set(BOARD_INFO, "Waiting for board process start...");
      }
    } else {
      String boardInfo = "Board server don't start, You can set argument \"--board-enable true\" in your submit script to start.";
      set(BOARD_INFO, boardInfo);
    }

    List<Container> workerContainers = app.context.getWorkerContainers();
    List<Container> psContainers = app.context.getPsContainers();
    Map<XLearningContainerId, String> reporterProgress = app.context.getReporterProgress();
    Map<XLearningContainerId, String> containersAppStartTime = app.context.getContainersAppStartTime();
    Map<XLearningContainerId, String> containersAppFinishTime = app.context.getContainersAppFinishTime();
    long workerGcores = app.context.getWorkerGcores();
    set(CONTAINER_NUMBER, String.valueOf(workerContainers.size() + psContainers.size()));
    set(WORKER_NUMBER, String.valueOf(workerContainers.size()));
    set(USER_NAME, StringUtils.split(conf.get("hadoop.job.ugi"), ',')[0]);
    set(WORKER_GCORES, String.valueOf(workerGcores));
    int i = 0;
    for (Container container : workerContainers) {
      set(CONTAINER_HTTP_ADDRESS + i, container.getNodeHttpAddress());
      set(CONTAINER_ID + i, container.getId().toString());
      if (app.context.getContainerStatus(new XLearningContainerId(container.getId())) != null) {
        set(CONTAINER_STATUS + i, app.context.getContainerStatus(new XLearningContainerId(container.getId())).toString());
      } else {
        set(CONTAINER_STATUS + i, "-");
      }
      set(CONTAINER_ROLE + i, "worker");

      if (app.context.getContainerGPUDevice(new XLearningContainerId(container.getId())) != null) {
        if (app.context.getContainerGPUDevice(new XLearningContainerId(container.getId())).trim().length() != 0) {
          set(CONTAINER_GPU_DEVICE + i, app.context.getContainerGPUDevice(new XLearningContainerId(container.getId())).toString());
          ConcurrentHashMap<String, LinkedBlockingDeque<List<Long>>> containersGpuMemMetrics = app.context.getContainersGpuMemMetrics().get(new XLearningContainerId(container.getId()));
          ConcurrentHashMap<String, LinkedBlockingDeque<List<Long>>> containersGpuUtilMetrics = app.context.getContainersGpuUtilMetrics().get(new XLearningContainerId(container.getId()));
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
        } else {
          set(CONTAINER_GPU_DEVICE + i, "-");
          if (Long.valueOf($(WORKER_GCORES)) > 0) {
            set(WORKER_GCORES, "0");
          }
        }
      }

      if (app.context.getContainersCpuMetrics().get(new XLearningContainerId(container.getId())) != null) {
        ConcurrentHashMap<String, LinkedBlockingDeque<Object>> cpuMetrics = app.context.getContainersCpuMetrics().get(new XLearningContainerId(container.getId()));
        if (cpuMetrics.size() != 0) {
          set("cpuMemMetrics" + i, new Gson().toJson(cpuMetrics.get("CPUMEM")));
          set("cpuUtilMetrics" + i, new Gson().toJson(cpuMetrics.get("CPUUTIL")));
        }
      }

      if (reporterProgress.get(new XLearningContainerId(container.getId())) != null && !reporterProgress.get(new XLearningContainerId(container.getId())).equals("")) {
        String progressLog = reporterProgress.get(new XLearningContainerId(container.getId()));
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
      } else {
        set(CONTAINER_REPORTER_PROGRESS + i, "0.00%");
      }
      if (containersAppStartTime.get(new XLearningContainerId(container.getId())) != null && !containersAppStartTime.get(new XLearningContainerId(container.getId())).equals("")) {
        String localStartTime = containersAppStartTime.get(new XLearningContainerId(container.getId()));
        set(CONTAINER_START_TIME + i, localStartTime);
      } else {
        set(CONTAINER_START_TIME + i, "N/A");
      }
      if (containersAppFinishTime.get(new XLearningContainerId(container.getId())) != null && !containersAppFinishTime.get(new XLearningContainerId(container.getId())).equals("")) {
        String localFinishTime = containersAppFinishTime.get(new XLearningContainerId(container.getId()));
        set(CONTAINER_FINISH_TIME + i, localFinishTime);
      } else {
        set(CONTAINER_FINISH_TIME + i, "N/A");
      }
      i++;
    }
    for (Container container : psContainers) {
      set(CONTAINER_HTTP_ADDRESS + i, container.getNodeHttpAddress());
      set(CONTAINER_ID + i, container.getId().toString());

      if ((app.context.getContainerGPUDevice(new XLearningContainerId(container.getId())) != null) && (app.context.getContainerGPUDevice(new XLearningContainerId(container.getId())).trim().length() != 0)) {
        set(CONTAINER_GPU_DEVICE + i, app.context.getContainerGPUDevice(new XLearningContainerId(container.getId())).toString());
      } else {
        set(CONTAINER_GPU_DEVICE + i, "-");
      }

      if (app.context.getContainerStatus(new XLearningContainerId(container.getId())) != null) {
        set(CONTAINER_STATUS + i, app.context.getContainerStatus(new XLearningContainerId(container.getId())).toString());
      } else {
        set(CONTAINER_STATUS + i, "-");
      }
      if ($(APP_TYPE).equals("Tensorflow")) {
        set(CONTAINER_ROLE + i, "ps");
      } else if ($(APP_TYPE).equals("Mxnet")) {
        set(CONTAINER_ROLE + i, "server");
      }

      set(CONTAINER_REPORTER_PROGRESS + i, "0.00%");
      if (containersAppStartTime.get(new XLearningContainerId(container.getId())) != null && !containersAppStartTime.get(new XLearningContainerId(container.getId())).equals("")) {
        String localStartTime = containersAppStartTime.get(new XLearningContainerId(container.getId()));
        set(CONTAINER_START_TIME + i, localStartTime);
      } else {
        set(CONTAINER_START_TIME + i, "N/A");
      }
      if (containersAppFinishTime.get(new XLearningContainerId(container.getId())) != null && !containersAppFinishTime.get(new XLearningContainerId(container.getId())).equals("")) {
        String localFinishTime = containersAppFinishTime.get(new XLearningContainerId(container.getId()));
        set(CONTAINER_FINISH_TIME + i, localFinishTime);
      } else {
        set(CONTAINER_FINISH_TIME + i, "N/A");
      }
      i++;
    }

    if (this.conf.get(XLearningConfiguration.XLEARNING_OUTPUT_STRATEGY, XLearningConfiguration.DEFAULT_XLEARNING_OUTPUT_STRATEGY).equals("STREAM")) {
      set(OUTPUT_TOTAL, "0");
    } else {
      set(OUTPUT_TOTAL, String.valueOf(app.context.getOutputs().size()));
    }
    i = 0;
    for (OutputInfo output : app.context.getOutputs()) {
      Path interResult = new Path(output.getDfsLocation()
          + conf.get(XLearningConfiguration.XLEARNING_INTERREAULST_DIR, XLearningConfiguration.DEFAULT_XLEARNING_INTERRESULT_DIR));
      set(OUTPUT_PATH + i, interResult.toString());
      i++;
    }

    set(TIMESTAMP_TOTAL, String.valueOf(app.context.getModelSavingList().size()));
    int j = 0;
    for (i = app.context.getModelSavingList().size(); i > 0; i--) {
      set(TIMESTAMP_LIST + j, String.valueOf(app.context.getModelSavingList().get(i - 1)));
      j++;
    }

    set(CONTAINER_CPU_METRICS_ENABLE, String.valueOf(true));
    try {
      WebApps.Builder.class.getMethod("build", WebApp.class);
    } catch (NoSuchMethodException e) {
      if (Controller.class.getClassLoader().getResource("webapps/static/xlWebApp") == null) {
        LOG.debug("Don't have the xlWebApp Resource.");
        set(CONTAINER_CPU_METRICS_ENABLE, String.valueOf(false));
      }
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
