package net.qihoo.xlearning.jobhistory;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.mapreduce.v2.app.webapp.App;
import org.apache.hadoop.yarn.webapp.Controller;
import org.apache.hadoop.yarn.webapp.View;
import org.apache.hadoop.fs.Path;
import com.google.inject.Inject;

import static org.apache.hadoop.yarn.util.StringHelper.join;

import org.apache.hadoop.fs.FSDataInputStream;
import net.qihoo.xlearning.conf.XLearningConfiguration;
import net.qihoo.xlearning.webapp.AMParams;

public class HsController extends Controller implements AMParams {
  private final App app;
  public Path jobLogPath;
  private final Configuration conf;
  private static final Log LOG = LogFactory.getLog(HsController.class);

  @Inject
  HsController(App app, Configuration conf, RequestContext ctx) {
    super(ctx);
    this.app = app;
    this.conf = conf;
  }

  @Override
  public void index() {
    setTitle("JobHistory");
  }

  public void job() throws IOException {
    XLearningConfiguration xlearningConf = new XLearningConfiguration();
    jobLogPath = new Path(conf.get(XLearningConfiguration.XLEARNING_HISTORY_LOG_DIR,
        XLearningConfiguration.DEFAULT_XLEARNING_HISTORY_LOG_DIR) + "/" + $(APP_ID) + "/" + $(APP_ID));
    LOG.info("jobLogPath:" + jobLogPath);
    String line = null;
    try {
      FSDataInputStream in = jobLogPath.getFileSystem(xlearningConf).open(jobLogPath);
      BufferedReader br = new BufferedReader(new InputStreamReader(in));
      line = br.readLine();
      in.close();
    } catch (IOException e) {
      LOG.info("open and read the log from " + jobLogPath + " error, " + e);
    }

    if (line == null) {
      set(CONTAINER_NUMBER, String.valueOf(0));
    } else {
      Gson gson = new Gson();
      Map<String, Object> readLog = new TreeMap<>();
      readLog = (Map) gson.fromJson(line, readLog.getClass());
      int i = 0;
      int workeri = 0;
      set(OUTPUT_TOTAL, String.valueOf(0));
      set(TIMESTAMP_TOTAL, String.valueOf(0));
      for (String info : readLog.keySet()) {
        if (info.equals(AMParams.APP_TYPE)) {
          if (readLog.get(info) != null) {
            if (readLog.get(info).equals("XLEARNING")) {
              set(APP_TYPE, "XLearning");
            } else {
              char[] appType = String.valueOf(readLog.get(info)).toLowerCase().toCharArray();
              appType[0] -= 32;
              set(APP_TYPE, String.valueOf(appType));
            }
          } else {
            set(APP_TYPE, "XLearning");
          }
        } else if (info.equals(AMParams.BOARD_INFO)) {
          set(BOARD_INFO_FLAG, "true");
          if (readLog.get(info).equals("-")) {
            String boardInfo = "Tensorboard server don't start, You can set argument \"--boardEnable true\" in your submit script to start.";
            set(BOARD_INFO, boardInfo);
          } else {
            set(BOARD_INFO, String.format("tensorboard --logdir=%s", readLog.get(info)));
          }
        } else if (info.equals(AMParams.OUTPUT_PATH)) {
          if (readLog.get(info) instanceof ArrayList<?>) {
            List<String> outputList = (ArrayList<String>) readLog.get(info);
            if (outputList.size() == 0 || outputList.get(0).equals("-")) {
              set(OUTPUT_TOTAL, String.valueOf(0));
            } else {
              int j = 0;
              for (String output : outputList) {
                set(OUTPUT_PATH + j, output + conf.get(XLearningConfiguration.XLEARNING_INTERREAULST_DIR, XLearningConfiguration.DEFAULT_XLEARNING_INTERRESULT_DIR));
                j++;
              }
              set(OUTPUT_TOTAL, String.valueOf(j));
            }
          } else {
            set(OUTPUT_TOTAL, String.valueOf(0));
          }
        } else if (info.equals(AMParams.TIMESTAMP_LIST)) {
          if (readLog.get(info) instanceof ArrayList<?>) {
            List<String> savedTimeList = (ArrayList<String>) readLog.get(info);
            if (savedTimeList.size() == 0 || savedTimeList.get(0).equals("-")) {
              set(TIMESTAMP_TOTAL, String.valueOf(0));
            } else {
              int j = 0;
              for (String timeStamp : savedTimeList) {
                set(TIMESTAMP_LIST + j, timeStamp);
                j++;
              }
              set(TIMESTAMP_TOTAL, String.valueOf(j));
            }
          } else {
            set(TIMESTAMP_TOTAL, String.valueOf(0));
          }
        } else if (info.indexOf("container") > -1) {
          set(CONTAINER_ID + i, info);
          if (readLog.get(info) instanceof Map<?, ?>) {
            Map<String, String> containerMessage = (Map<String, String>) readLog.get(info);
            set(CONTAINER_HTTP_ADDRESS + i, containerMessage.get(AMParams.CONTAINER_HTTP_ADDRESS));
            set(CONTAINER_ROLE + i, containerMessage.get(AMParams.CONTAINER_ROLE));
            set(CONTAINER_STATUS + i, containerMessage.get(AMParams.CONTAINER_STATUS));
            set(CONTAINER_START_TIME + i, containerMessage.get(AMParams.CONTAINER_START_TIME));
            set(CONTAINER_FINISH_TIME + i, containerMessage.get(AMParams.CONTAINER_FINISH_TIME));
            set(CONTAINER_REPORTER_PROGRESS + i, containerMessage.get(AMParams.CONTAINER_REPORTER_PROGRESS));
            set(CONTAINER_LOG_ADDRESS + i, containerMessage.get(AMParams.CONTAINER_LOG_ADDRESS));
            if (containerMessage.get(AMParams.CONTAINER_ROLE).equals("worker")) {
              String cpuMetrics = containerMessage.get(AMParams.CONTAINER_CPU_METRICS);
              if (cpuMetrics != null) {
                Gson gson2 = new GsonBuilder()
                    .registerTypeAdapter(
                        new TypeToken<ConcurrentHashMap<String, Object>>() {
                        }.getType(),
                        new JsonDeserializer<ConcurrentHashMap<String, Object>>() {
                          @Override
                          public ConcurrentHashMap<String, Object> deserialize(
                              JsonElement json, Type typeOfT,
                              JsonDeserializationContext context) throws JsonParseException {
                            ConcurrentHashMap<String, Object> treeMap = new ConcurrentHashMap<>();
                            JsonObject jsonObject = json.getAsJsonObject();
                            Set<Map.Entry<String, JsonElement>> entrySet = jsonObject.entrySet();
                            for (Map.Entry<String, JsonElement> entry : entrySet) {
                              treeMap.put(entry.getKey(), entry.getValue());
                            }
                            return treeMap;
                          }
                        }).create();

                Type type = new TypeToken<ConcurrentHashMap<String, Object>>() {
                }.getType();
                ConcurrentHashMap<String, Object> map = gson2.fromJson(cpuMetrics, type);
                set("cpuMemMetrics" + workeri, new Gson().toJson(map.get("CPUMEM")));
                set("cpuUtilMetrics" + workeri, new Gson().toJson(map.get("CPUUTIL")));
              }
              set("WORKER_CONTAINER_ID" + workeri, info);
              workeri++;
            }
            i++;
          }
        } else if (info.equals(AMParams.WORKER_NUMBER)){
          set(WORKER_NUMBER, String.valueOf(readLog.get(info)));
        }
      }
      set(CONTAINER_NUMBER, String.valueOf(i));
    }
    setTitle(join($(APP_TYPE) + " Application ", $(APP_ID)));
    render(jobPage());
  }

  protected Class<? extends View> jobPage() {
    return HsJobPage.class;
  }

  /**
   * Render the logs page.
   */
  public void logs() {
    render(HsLogsPage.class);
  }

}
