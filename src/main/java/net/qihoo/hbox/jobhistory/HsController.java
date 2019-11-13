package net.qihoo.hbox.jobhistory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import net.qihoo.hbox.api.HboxConstants;
import net.qihoo.hbox.webapp.AMParams;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.mapreduce.v2.app.webapp.App;
import org.apache.hadoop.yarn.webapp.Controller;
import org.apache.hadoop.yarn.webapp.View;

import org.apache.hadoop.fs.Path;
import net.qihoo.hbox.conf.HboxConfiguration;

import com.google.inject.Inject;

import static org.apache.hadoop.yarn.util.StringHelper.join;

import org.apache.hadoop.fs.FSDataInputStream;

/**
 * This class renders the various pages that the History Server WebApp supports
 */
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
        HboxConfiguration hboxConf = new HboxConfiguration();
        String logdir = hboxConf.get("fs.defaultFS") + conf.get(HboxConfiguration.HBOX_HISTORY_LOG_DIR,
                HboxConfiguration.DEFAULT_HBOX_HISTORY_LOG_DIR) + "/" + $(APP_ID) + "/" + $(APP_ID);
        jobLogPath = new Path(logdir);
        LOG.info("jobLogPath:" + jobLogPath);
        String line = null;
        try {
            FSDataInputStream in = jobLogPath.getFileSystem(hboxConf).open(jobLogPath);
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
            Map<String, List<String>> readLog = new TreeMap<String, List<String>>();
            readLog = (Map<String, List<String>>) gson.fromJson(line, readLog.getClass());
            set("PS_GCORES", "0");
            set("PS_NUM", "0");
            set("chiefWorkerMemory", "");
            set("evaluatorWorkerMemory", "");
            set("USAGED_INFO", "false");
            if (readLog.keySet().contains("hboxVersion")) {
                set("VERSION", readLog.get("hboxVersion").get(0));
                int i = 0;
                int workeri = 0;
                int psi = 0;
                set(OUTPUT_TOTAL, String.valueOf(0));
                set(TIMESTAMP_TOTAL, String.valueOf(0));
                for (String info : readLog.keySet()) {
                    if (info.equals("appType")) {
                        if (readLog.get(info).get(0) != null) {
                            if (readLog.get(info).get(0).equals("HBOX")) {
                                set(APP_TYPE, "HBox");
                            } else {
                                char[] appType = readLog.get(info).get(0).toLowerCase().toCharArray();
                                appType[0] -= 32;
                                set(APP_TYPE, String.valueOf(appType));
                            }
                        } else {
                            set(APP_TYPE, "HBox");
                        }
                    } else if (info.equals("tensorboard")) {
                        set(BOARD_INFO_FLAG, "true");
                        if (readLog.get(info).get(0).equals("-")) {
                            String boardInfo = "Tensorboard server don't start, You can set argument \"--boardEnable true\" in your submit script to start.";
                            set(BOARD_INFO, boardInfo);
                        } else {
                            set(BOARD_INFO, String.format("tensorboard --logdir=%s", readLog.get(info).get(0)));
                        }
                    } else if (info.equals("board")) {
                        set(BOARD_INFO_FLAG, "true");
                        set(BOARD_INFO, readLog.get(info).get(0));
                    } else if (info.equals("output")) {
                        if (readLog.get(info).size() == 0 || readLog.get(info).get(0).equals("-")) {
                            set(OUTPUT_TOTAL, String.valueOf(0));
                        } else {
                            int j = 0;
                            for (String output : readLog.get(info)) {
                                set(OUTPUT_PATH + j, output + conf.get(HboxConfiguration.HBOX_INTERRESULT_DIR, HboxConfiguration.DEFAULT_HBOX_INTERRESULT_DIR));
                                j++;
                            }
                            set(OUTPUT_TOTAL, String.valueOf(j));
                        }
                    } else if (info.equals("savedTimeStamp")) {
                        if (readLog.get(info).size() == 0 || readLog.get(info).get(0).equals("-")) {
                            set(TIMESTAMP_TOTAL, String.valueOf(0));
                        } else {
                            int j = 0;
                            for (String timeStamp : readLog.get(info)) {
                                set(TIMESTAMP_LIST + j, timeStamp);
                                j++;
                            }
                            set(TIMESTAMP_TOTAL, String.valueOf(j));
                        }
                    } else if (info.indexOf("container") > -1) {
                        set(CONTAINER_ID + i, info);
                        set(CONTAINER_HTTP_ADDRESS + i, readLog.get(info).get(0));
                        if (readLog.get(info).get(1).trim().length() != 0) {
                            set(CONTAINER_GPU_DEVICE + i, readLog.get(info).get(1));
                            if ((readLog.get(info).get(2).equals("worker") || readLog.get(info).get(2).equals(HboxConstants.EVALUATOR) || readLog.get(info).get(2).equals(HboxConstants.CHIEF)) && !readLog.get(info).get(1).equals("-") && readLog.get(info).size() > 8) {
                                String containersGpuMemMetrics = readLog.get(info).get(5);
                                String containersGpuUtilMetrics = readLog.get(info).get(6);
                                if (containersGpuMemMetrics != null && containersGpuMemMetrics != "-") {
                                    Type type = new TypeToken<ConcurrentHashMap<String, LinkedBlockingDeque<List<Long>>>>() {
                                    }.getType();
                                    ConcurrentHashMap<String, LinkedBlockingDeque<List<Long>>> map = new Gson().fromJson(containersGpuMemMetrics, type);
                                    for (String str : map.keySet()) {
                                        set("workergpuMemMetrics" + workeri + str, new Gson().toJson(map.get(str)));
                                    }
                                }
                                if (containersGpuUtilMetrics != null && containersGpuUtilMetrics != "-") {
                                    Type type = new TypeToken<ConcurrentHashMap<String, LinkedBlockingDeque<List<Long>>>>() {
                                    }.getType();
                                    ConcurrentHashMap<String, LinkedBlockingDeque<List<Long>>> map = new Gson().fromJson(containersGpuUtilMetrics, type);
                                    for (String str : map.keySet()) {
                                        set("workergpuUtilMetrics" + workeri + str, new Gson().toJson(map.get(str)));
                                    }
                                }
                                set("WORKER_GPU_DEVICE" + workeri, readLog.get(info).get(1));
                                if (readLog.get(info).size() > 11) {
                                    String containersGpuMemStatistics = readLog.get(info).get(12);
                                    String containersGpuUtilStatistics = readLog.get(info).get(13);
                                    if (containersGpuMemStatistics != null) {
                                        Type type = new TypeToken<Map<String, List<Double>>>() {
                                        }.getType();
                                        Map<String, List<Double>> map = new Gson().fromJson(containersGpuMemStatistics, type);
                                        for (String str : map.keySet()) {
                                            set("worker" + GPU_USAGE_TYPE + CONTAINER_MEM_USAGE_STATISTICS + USAGE_AVG + workeri + str, String.format("%.2f", map.get(str).get(0)));
                                            set("worker" + GPU_USAGE_TYPE + CONTAINER_MEM_USAGE_STATISTICS + USAGE_MAX + workeri + str, String.format("%.2f", map.get(str).get(1)));
                                        }
                                    }
                                    if (containersGpuUtilStatistics != null) {
                                        Type type = new TypeToken<Map<String, List<Double>>>() {
                                        }.getType();
                                        Map<String, List<Double>> map = new Gson().fromJson(containersGpuUtilStatistics, type);
                                        for (String str : map.keySet()) {
                                            set("worker" + GPU_USAGE_TYPE + CONTAINER_UTIL_USAGE_STATISTICS + USAGE_AVG + workeri + str, String.format("%.2f", map.get(str).get(0)));
                                            set("worker" + GPU_USAGE_TYPE + CONTAINER_UTIL_USAGE_STATISTICS + USAGE_MAX + workeri + str, String.format("%.2f", map.get(str).get(1)));
                                        }
                                    }
                                }
                            }
                            if ((readLog.get(info).get(2).equals("ps") || readLog.get(info).get(2).equals("server") || readLog.get(info).get(2).equals("scheduler")) && !readLog.get(info).get(1).equals("-") && readLog.get(info).size() > 8) {
                                String containersGpuMemMetrics = readLog.get(info).get(5);
                                String containersGpuUtilMetrics = readLog.get(info).get(6);
                                if (containersGpuMemMetrics != null && containersGpuMemMetrics != "-") {
                                    Type type = new TypeToken<ConcurrentHashMap<String, LinkedBlockingDeque<List<Long>>>>() {
                                    }.getType();
                                    ConcurrentHashMap<String, LinkedBlockingDeque<List<Long>>> map = new Gson().fromJson(containersGpuMemMetrics, type);
                                    for (String str : map.keySet()) {
                                        set("psgpuMemMetrics" + psi + str, new Gson().toJson(map.get(str)));
                                    }
                                }
                                if (containersGpuUtilMetrics != null && containersGpuUtilMetrics != "-") {
                                    Type type = new TypeToken<ConcurrentHashMap<String, LinkedBlockingDeque<List<Long>>>>() {
                                    }.getType();
                                    ConcurrentHashMap<String, LinkedBlockingDeque<List<Long>>> map = new Gson().fromJson(containersGpuUtilMetrics, type);
                                    for (String str : map.keySet()) {
                                        set("psgpuUtilMetrics" + psi + str, new Gson().toJson(map.get(str)));
                                    }
                                }
                                set("PS_GPU_DEVICE" + psi, readLog.get(info).get(1));
                                if (readLog.get(info).size() > 11) {
                                    String containersGpuMemStatistics = readLog.get(info).get(12);
                                    String containersGpuUtilStatistics = readLog.get(info).get(13);
                                    if (containersGpuMemStatistics != null && containersGpuMemStatistics != "-") {
                                        Type type = new TypeToken<Map<String, List<Double>>>() {
                                        }.getType();
                                        Map<String, List<Double>> map = new Gson().fromJson(containersGpuMemStatistics, type);
                                        for (String str : map.keySet()) {
                                            set("ps" + GPU_USAGE_TYPE + CONTAINER_MEM_USAGE_STATISTICS + USAGE_AVG + psi + str, String.format("%.2f", map.get(str).get(0)));
                                            set("ps" + GPU_USAGE_TYPE + CONTAINER_MEM_USAGE_STATISTICS + USAGE_MAX + psi + str, String.format("%.2f", map.get(str).get(1)));
                                        }
                                    }
                                    if (containersGpuUtilStatistics != null && containersGpuUtilStatistics != "-") {
                                        Type type = new TypeToken<Map<String, List<Double>>>() {
                                        }.getType();
                                        Map<String, List<Double>> map = new Gson().fromJson(containersGpuUtilStatistics, type);
                                        for (String str : map.keySet()) {
                                            set("ps" + GPU_USAGE_TYPE + CONTAINER_UTIL_USAGE_STATISTICS + USAGE_AVG + psi + str, String.format("%.2f", map.get(str).get(0)));
                                            set("ps" + GPU_USAGE_TYPE + CONTAINER_UTIL_USAGE_STATISTICS + USAGE_MAX + psi + str, String.format("%.2f", map.get(str).get(1)));
                                        }
                                    }
                                }
                            }
                        } else {
                            set(CONTAINER_GPU_DEVICE + i, "-");
                        }
                        if ((readLog.get(info).get(2).equals("worker") || readLog.get(info).get(2).equals(HboxConstants.EVALUATOR) || readLog.get(info).get(2).equals(HboxConstants.CHIEF)) && readLog.get(info).size() > 8) {
                            String cpuMetrics = readLog.get(info).get(4);
                            if (cpuMetrics != null && cpuMetrics != "-") {
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
                                set("workercpuMemMetrics" + workeri, new Gson().toJson(map.get("CPUMEM")));
                                set("workercpuUtilMetrics" + workeri, new Gson().toJson(map.get("CPUUTIL")));
                            }
                            if (readLog.get(info).size() > 11) {
                                String cpuStatistics = readLog.get(info).get(11);
                                if (cpuStatistics != null && cpuStatistics != "-") {
                                    Type type = new TypeToken<Map<String, List<Double>>>() {
                                    }.getType();
                                    Map<String, List<Double>> map = new Gson().fromJson(cpuStatistics, type);
                                    if (map.size() > 0) {
                                        set("worker" + CPU_USAGE_TYPE + CONTAINER_MEM_USAGE_STATISTICS + USAGE_AVG + workeri, String.format("%.2f", map.get("CPUMEM").get(0)));
                                        set("worker" + CPU_USAGE_TYPE + CONTAINER_MEM_USAGE_STATISTICS + USAGE_MAX + workeri, String.format("%.2f", map.get("CPUMEM").get(1)));
                                        set("worker" + CPU_USAGE_TYPE + CONTAINER_UTIL_USAGE_STATISTICS + USAGE_AVG + workeri, String.format("%.2f", map.get("CPUUTIL").get(0)));
                                        set("worker" + CPU_USAGE_TYPE + CONTAINER_UTIL_USAGE_STATISTICS + USAGE_MAX + workeri, String.format("%.2f", map.get("CPUUTIL").get(1)));
                                    } else {
                                        set("worker" + CPU_USAGE_TYPE + CONTAINER_MEM_USAGE_STATISTICS + USAGE_AVG + workeri, "");
                                        set("worker" + CPU_USAGE_TYPE + CONTAINER_MEM_USAGE_STATISTICS + USAGE_MAX + workeri, "");
                                        set("worker" + CPU_USAGE_TYPE + CONTAINER_UTIL_USAGE_STATISTICS + USAGE_AVG + workeri, "");
                                        set("worker" + CPU_USAGE_TYPE + CONTAINER_UTIL_USAGE_STATISTICS + USAGE_MAX + workeri, "");
                                    }
                                }
                                if (readLog.get(info).size() > 14) {
                                    set("worker" + CONTAINER_MEM_USAGE_WARN + workeri, readLog.get(info).get(14));
                                }
                                set("USAGED_INFO", "true");
                            }
                        }
                        if ((readLog.get(info).get(2).equals("ps") || readLog.get(info).get(2).equals("server") || readLog.get(info).get(2).equals("scheduler")) && readLog.get(info).size() > 8) {
                            String cpuMetrics = readLog.get(info).get(4);
                            if (cpuMetrics != null && cpuMetrics != "-") {
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
                                set("pscpuMemMetrics" + psi, new Gson().toJson(map.get("CPUMEM")));
                                set("pscpuUtilMetrics" + psi, new Gson().toJson(map.get("CPUUTIL")));
                            }

                            if (readLog.get(info).size() > 11) {
                                String cpuStatistics = readLog.get(info).get(11);
                                if (cpuStatistics != null && cpuStatistics != "-") {
                                    Type type = new TypeToken<Map<String, List<Double>>>() {
                                    }.getType();
                                    Map<String, List<Double>> map = new Gson().fromJson(cpuStatistics, type);
                                    if (map.size() > 0) {
                                        set("ps" + CPU_USAGE_TYPE + CONTAINER_MEM_USAGE_STATISTICS + USAGE_AVG + psi, String.format("%.2f", map.get("CPUMEM").get(0)));
                                        set("ps" + CPU_USAGE_TYPE + CONTAINER_MEM_USAGE_STATISTICS + USAGE_MAX + psi, String.format("%.2f", map.get("CPUMEM").get(1)));
                                        set("ps" + CPU_USAGE_TYPE + CONTAINER_UTIL_USAGE_STATISTICS + USAGE_AVG + psi, String.format("%.2f", map.get("CPUUTIL").get(0)));
                                        set("ps" + CPU_USAGE_TYPE + CONTAINER_UTIL_USAGE_STATISTICS + USAGE_MAX + psi, String.format("%.2f", map.get("CPUUTIL").get(1)));
                                    } else {
                                        set("ps" + CPU_USAGE_TYPE + CONTAINER_MEM_USAGE_STATISTICS + USAGE_AVG + psi, "");
                                        set("ps" + CPU_USAGE_TYPE + CONTAINER_MEM_USAGE_STATISTICS + USAGE_MAX + psi, "");
                                        set("ps" + CPU_USAGE_TYPE + CONTAINER_UTIL_USAGE_STATISTICS + USAGE_AVG + psi, "");
                                        set("ps" + CPU_USAGE_TYPE + CONTAINER_UTIL_USAGE_STATISTICS + USAGE_MAX + psi, "");
                                    }
                                }
                                if (readLog.get(info).size() > 14) {
                                    set("ps" + CONTAINER_MEM_USAGE_WARN + psi, readLog.get(info).get(14));
                                }
                                set("USAGED_INFO", "true");
                            }
                        }
                        if (readLog.get(info).get(2).equals(HboxConstants.EVALUATOR) || readLog.get(info).get(2).equals(HboxConstants.CHIEF)) {
                            set(CONTAINER_ROLE + i, HboxConstants.WORKER + "/" + readLog.get(info).get(2));
                        } else {
                            set(CONTAINER_ROLE + i, readLog.get(info).get(2));
                        }
                        set(CONTAINER_STATUS + i, readLog.get(info).get(3));
                        if (readLog.get(info).size() > 8) {
                            set(CONTAINER_START_TIME + i, readLog.get(info).get(7));
                            set(CONTAINER_FINISH_TIME + i, readLog.get(info).get(8));
                            set(CONTAINER_REPORTER_PROGRESS + i, readLog.get(info).get(9));
                            set(CONTAINER_LOG_ADDRESS + i, readLog.get(info).get(10));
                        } else {
                            set(CONTAINER_START_TIME + i, readLog.get(info).get(4));
                            set(CONTAINER_FINISH_TIME + i, readLog.get(info).get(5));
                            set(CONTAINER_REPORTER_PROGRESS + i, readLog.get(info).get(6));
                            set(CONTAINER_LOG_ADDRESS + i, readLog.get(info).get(7));
                        }
                        if (readLog.get(info).get(2).equals("worker") || readLog.get(info).get(2).equals(HboxConstants.EVALUATOR) || readLog.get(info).get(2).equals(HboxConstants.CHIEF)) {
                            set("WORKER_CONTAINER_ID" + workeri, info);
                            workeri++;
                        }
                        if (readLog.get(info).get(2).equals("ps") || readLog.get(info).get(2).equals("server") || readLog.get(info).get(2).equals("scheduler")) {
                            set("PS_CONTAINER_ID" + psi, info);
                            psi++;
                        }
                        i++;
                    } else if (info.equals("workerGcores")) {
                        set("WORKER_GCORES", readLog.get(info).get(0));
                    } else if (info.equals("workerNums")) {
                        set("WORKER_NUM", readLog.get(info).get(0));
                    } else if (info.equals("psGcores")) {
                        set("PS_GCORES", readLog.get(info).get(0));
                    } else if (info.equals("psNums")) {
                        set("PS_NUM", readLog.get(info).get(0));
                    } else if (info.equals("workerMemory")) {
                        set(WORKER_MEMORY, readLog.get(info).get(0));
                    } else if (info.equals("workerVCores")) {
                        set(WORKER_VCORES, readLog.get(info).get(0));
                    } else if (info.equals("psMemory")) {
                        set(PS_MEMORY, readLog.get(info).get(0));
                    } else if (info.equals("psVCores")) {
                        set(PS_VCORES, readLog.get(info).get(0));
                    } else if (info.equals("chiefWorkerMemory")) {
                        set("chiefWorkerMemory", readLog.get(info).get(0));
                    } else if (info.equals("evaluatorWorkerMemory")) {
                        set("evaluatorWorkerMemory", readLog.get(info).get(0));
                    }
                }
                set(CONTAINER_NUMBER, String.valueOf(i));

                if ($("VERSION").indexOf("1.2") != 0 && Boolean.parseBoolean($(BOARD_INFO_FLAG))) {
                    if ($(BOARD_INFO).equals("-")) {
                        String boardInfo = "Board server don't start, You can set argument \"--boardEnable true\" in your submit script to start.";
                        set(BOARD_INFO, boardInfo);
                    } else {
                        String boardLogDir = $(BOARD_INFO);
                        if ($(APP_TYPE).equals("Tensorflow")) {
                            set(BOARD_INFO, String.format("tensorboard --logdir=%s", boardLogDir));
                        } else {
                            set(BOARD_INFO, String.format("VisualDL not support the hdfs path for logdir. Please download the log from %s first. Then using \" visualDL \" to start the board", boardLogDir));
                        }
                    }
                }
            } else {
                int i = 0;
                set(OUTPUT_TOTAL, String.valueOf(0));
                set(TIMESTAMP_TOTAL, String.valueOf(0));
                for (String info : readLog.keySet()) {
                    if (info.equals("appType")) {
                        if (readLog.get(info).get(0) != null) {
                            if (readLog.get(info).get(0).equals("HBOX")) {
                                set(APP_TYPE, "HBox");
                            } else {
                                char[] appType = readLog.get(info).get(0).toLowerCase().toCharArray();
                                appType[0] -= 32;
                                set(APP_TYPE, String.valueOf(appType));
                            }
                        } else {
                            set(APP_TYPE, "HBox");
                        }
                    } else if (info.equals("tensorboard")) {
                        set(BOARD_INFO_FLAG, "true");
                        if (readLog.get(info).get(0).equals("-")) {
                            String boardInfo = "Tensorboard server don't start, You can set argument \"--boardEnable true\" in your submit script to start.";
                            set(BOARD_INFO, boardInfo);
                        } else {
                            set(BOARD_INFO, String.format("tensorboard --logdir=%s", readLog.get(info).get(0)));
                        }
                    } else if (info.equals("output")) {
                        if (readLog.get(info).size() == 0 || readLog.get(info).get(0).equals("-")) {
                            set(OUTPUT_TOTAL, String.valueOf(0));
                        } else {
                            int j = 0;
                            for (String output : readLog.get(info)) {
                                set(OUTPUT_PATH + j, output + conf.get(HboxConfiguration.HBOX_INTERRESULT_DIR, HboxConfiguration.DEFAULT_HBOX_INTERRESULT_DIR));
                                j++;
                            }
                            set(OUTPUT_TOTAL, String.valueOf(j));
                        }
                    } else if (info.equals("savedTimeStamp")) {
                        if (readLog.get(info).size() == 0 || readLog.get(info).get(0).equals("-")) {
                            set(TIMESTAMP_TOTAL, String.valueOf(0));
                        } else {
                            int j = 0;
                            for (String timeStamp : readLog.get(info)) {
                                set(TIMESTAMP_LIST + j, timeStamp);
                                j++;
                            }
                            set(TIMESTAMP_TOTAL, String.valueOf(j));
                        }
                    } else if (info.indexOf("container") > -1) {
                        set(CONTAINER_ID + i, info);
                        set(CONTAINER_HTTP_ADDRESS + i, readLog.get(info).get(0));
                        if (readLog.get(info).get(1).trim().length() != 0) {
                            set(CONTAINER_GPU_DEVICE + i, readLog.get(info).get(1));
                        } else {
                            set(CONTAINER_GPU_DEVICE + i, "-");
                        }
                        if (readLog.get(info).get(2).equals(HboxConstants.EVALUATOR) || readLog.get(info).get(2).equals(HboxConstants.CHIEF)) {
                            set(CONTAINER_ROLE + i, HboxConstants.WORKER + "/" + readLog.get(info).get(2));
                        } else {
                            set(CONTAINER_ROLE + i, readLog.get(info).get(2));
                        }
                        set(CONTAINER_STATUS + i, readLog.get(info).get(3));
                        set(CONTAINER_START_TIME + i, readLog.get(info).get(4));
                        set(CONTAINER_FINISH_TIME + i, readLog.get(info).get(5));
                        set(CONTAINER_REPORTER_PROGRESS + i, readLog.get(info).get(6));
                        set(CONTAINER_LOG_ADDRESS + i, readLog.get(info).get(7));

                        i++;
                    } else if (info.equals("workerGcores")) {
                        set("WORKER_GCORES", readLog.get(info).get(0));
                    } else if (info.equals("workerNums")) {
                        set("WORKER_NUM", readLog.get(info).get(0));
                    }
                }
                set(CONTAINER_NUMBER, String.valueOf(i));
            }

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
