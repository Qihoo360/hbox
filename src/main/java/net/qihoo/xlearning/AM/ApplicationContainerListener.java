package net.qihoo.xlearning.AM;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import net.qihoo.xlearning.api.*;
import net.qihoo.xlearning.common.*;
import net.qihoo.xlearning.conf.XLearningConfiguration;
import net.qihoo.xlearning.container.XLearningContainerId;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.ipc.ProtocolSignature;
import org.apache.hadoop.ipc.RPC;
import org.apache.hadoop.ipc.Server;
import org.apache.hadoop.service.AbstractService;
import org.apache.hadoop.yarn.util.Clock;
import org.apache.hadoop.yarn.util.SystemClock;
import org.apache.hadoop.mapred.InputSplit;

import java.io.IOException;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingDeque;

public class ApplicationContainerListener extends AbstractService implements ApplicationContainerProtocol,
    ContainerListener {

  private static final Log LOG = LogFactory.getLog(ApplicationContainerListener.class);

  private final ApplicationContext applicationContext;

  private Server server;

  private final Map<XLearningContainerId, String> containerId2Role;

  private final Map<XLearningContainerId, XLearningContainerStatus> containerId2Status;

  private final Map<String, List<ContainerHostPair>> clusterDef;

  private final Map<XLearningContainerId, String> lightGBMIpPortMap;

  private final Map<XLearningContainerId, String> reporterProgress;

  private final Map<XLearningContainerId, String> mapedTaskID;

  private final Map<XLearningContainerId, String> containersAppStartTimeMap;

  private final Map<XLearningContainerId, String> containersAppFinishTimeMap;

  private final Map<XLearningContainerId, ConcurrentHashMap<String, LinkedBlockingDeque<Object>>> containersCpuMetrics;

  private final Map<XLearningContainerId, ConcurrentHashMap<String, LinkedBlockingDeque<List<Long>>>> containersGpuMemMetrics;

  private final Map<XLearningContainerId, ConcurrentHashMap<String, LinkedBlockingDeque<List<Long>>>> containersGpuUtilMetrics;

  private String clusterDefStr;

  private String lightGBMIpPortStr;

  private final Clock clock;

  private final ConcurrentMap<XLearningContainerId, LastTime> runningContainers;

  private Thread containerTimeoutMonitor;

  private int containerTimeOut;

  private int localResourceTimeOut;

  private int monitorInterval;

  private boolean isXLearningTrainFinished;

  private String tensorboardUrl;

  private boolean isSaveInnerModel;

  private Long interResultTimeStamp;

  private final Map<XLearningContainerId, InnerModelSavedPair> containerId2InnerModel;

  private String xlearningAppType;

  private final Map<XLearningContainerId, String> containerId2GPU;

  public ApplicationContainerListener(ApplicationContext applicationContext, Configuration conf) {
    super(ApplicationContainerListener.class.getSimpleName());
    this.setConfig(conf);
    this.containerId2Role = new ConcurrentHashMap<>();
    this.containerId2Status = new ConcurrentHashMap<>();
    this.reporterProgress = new ConcurrentHashMap<>();
    this.mapedTaskID = new ConcurrentHashMap<>();
    this.containersAppStartTimeMap = new ConcurrentHashMap<>();
    this.containersAppFinishTimeMap = new ConcurrentHashMap<>();
    this.clusterDef = new ConcurrentHashMap<>();
    this.clusterDef.put(XLearningConstants.WORKER, Collections.synchronizedList(new ArrayList<ContainerHostPair>()));
    this.clusterDef.put(XLearningConstants.PS, Collections.synchronizedList(new ArrayList<ContainerHostPair>()));
    this.clusterDefStr = null;
    this.lightGBMIpPortMap = new ConcurrentHashMap<>();
    this.lightGBMIpPortStr = null;
    this.applicationContext = applicationContext;
    this.clock = new SystemClock();
    this.runningContainers = new ConcurrentHashMap<>();
    this.containerTimeOut = conf.getInt(XLearningConfiguration.XLEARNING_TASK_TIMEOUT, XLearningConfiguration.DEFAULT_XLEARNING_TASK_TIMEOUT);
    this.localResourceTimeOut = conf.getInt(XLearningConfiguration.XLEARNING_LOCALRESOURCE_TIMEOUT, XLearningConfiguration.DEFAULT_XLEARNING_LOCALRESOURCE_TIMEOUT);
    this.monitorInterval = conf.getInt(XLearningConfiguration.XLEARNING_TASK_TIMEOUT_CHECK_INTERVAL_MS, XLearningConfiguration.DEFAULT_XLEARNING_TASK_TIMEOUT_CHECK_INTERVAL_MS);
    this.isXLearningTrainFinished = false;
    this.tensorboardUrl = null;
    this.isSaveInnerModel = false;
    this.interResultTimeStamp = Long.MIN_VALUE;
    this.containerId2InnerModel = new ConcurrentHashMap<>();
    this.containersCpuMetrics = new ConcurrentHashMap<>();
    this.containersGpuMemMetrics = new ConcurrentHashMap<>();
    this.containersGpuUtilMetrics = new ConcurrentHashMap<>();
    if (System.getenv().containsKey(XLearningConstants.Environment.XLEARNING_APP_TYPE.toString())) {
      xlearningAppType = System.getenv(XLearningConstants.Environment.XLEARNING_APP_TYPE.toString()).toUpperCase();
    } else {
      xlearningAppType = "XLEARNING";
    }
    this.containerId2GPU = new ConcurrentHashMap<>();
  }

  @Override
  public void start() {
    LOG.info("Starting application containers handler server");
    RPC.Builder builder = new RPC.Builder(getConfig());
    builder.setProtocol(ApplicationContainerProtocol.class);
    builder.setInstance(this);
    builder.setBindAddress("0.0.0.0");
    builder.setPort(0);
    try {
      server = builder.build();
    } catch (Exception e) {
      LOG.error("Error starting application containers handler server!", e);
      e.printStackTrace();
      return;
    }
    server.start();

    containerTimeoutMonitor = new Thread(new TimeoutMonitor());
    containerTimeoutMonitor.setName("Container-timeout-monitor");
    containerTimeoutMonitor.setDaemon(true);
    containerTimeoutMonitor.start();
    LOG.info("Container timeout monitor thread had started");
  }

  public String getTensorboardUrl() {
    return this.tensorboardUrl;
  }

  public Map<XLearningContainerId, String> getReporterProgress() {
    return this.reporterProgress;
  }

  public Map<XLearningContainerId, String> getContainersAppStartTime() {
    return this.containersAppStartTimeMap;
  }

  public Map<XLearningContainerId, String> getContainersAppFinishTime() {
    return this.containersAppFinishTimeMap;
  }

  public Map<XLearningContainerId, String> getMapedTaskID() {
    return this.mapedTaskID;
  }

  public Map<XLearningContainerId, ConcurrentHashMap<String, LinkedBlockingDeque<Object>>> getContainersCpuMetrics() {
    return this.containersCpuMetrics;
  }

  public Map<XLearningContainerId, ConcurrentHashMap<String, LinkedBlockingDeque<List<Long>>>> getContainersGpuMemMetrics() {
    return this.containersGpuMemMetrics;
  }

  public Map<XLearningContainerId, ConcurrentHashMap<String, LinkedBlockingDeque<List<Long>>>> getContainersGpuUtilMetrics() {
    return this.containersGpuUtilMetrics;
  }

  public int getServerPort() {
    return server.getPort();
  }

  public void setTrainFinished() {
    isXLearningTrainFinished = true;
  }

  public void setSaveInnerModel(Boolean isSaveInnerModel) {
    LOG.debug("last saved status:" + this.isSaveInnerModel + "; new saved status:" + isSaveInnerModel);
    if (this.isSaveInnerModel != isSaveInnerModel) {
      this.isSaveInnerModel = isSaveInnerModel;
      if (this.isSaveInnerModel) {
        this.interResultTimeStamp = System.currentTimeMillis();
        try {
          Configuration conf = this.getConfig();
          FileSystem fs = FileSystem.get(conf);
          for (OutputInfo output : this.applicationContext.getOutputs()) {
            Path innerResult = new Path(output.getDfsLocation()
                + conf.get(XLearningConfiguration.XLEARNING_INTERREAULST_DIR, XLearningConfiguration.DEFAULT_XLEARNING_INTERRESULT_DIR)
                + new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss").format(new Date(this.interResultTimeStamp)));
            fs.mkdirs(innerResult);
            LOG.info("interResult path:" + innerResult);
          }
          fs.close();
        } catch (IOException e) {
          LOG.error("create the interResult path error: " + e);
        }
      }
    }
  }

  public int getInnerSavingContainerNum() {
    return containerId2InnerModel.size();
  }

  public XLearningContainerStatus getContainerStatus(XLearningContainerId xlearningContainerId) {
    if (containerId2Status.containsKey(xlearningContainerId)) {
      return containerId2Status.get(xlearningContainerId);
    }
    return null;
  }

  @Override
  public void registerContainer(XLearningContainerId containerId, String role) {
    containerId2Status.put(containerId, XLearningContainerStatus.UNDEFINED);
    containerId2Role.put(containerId, role);
    reporterProgress.put(containerId, "");
    containersAppStartTimeMap.put(containerId, "");
    containersAppFinishTimeMap.put(containerId, "");
    containersCpuMetrics.put(containerId, new ConcurrentHashMap<String, LinkedBlockingDeque<Object>>());
    containersGpuMemMetrics.put(containerId, new ConcurrentHashMap<String, LinkedBlockingDeque<List<Long>>>());
    containersGpuUtilMetrics.put(containerId, new ConcurrentHashMap<String, LinkedBlockingDeque<List<Long>>>());
    if (role.equals(XLearningConstants.WORKER.toString())) {
      containerId2InnerModel.put(containerId, new InnerModelSavedPair());
    }
    runningContainers.put(containerId, new LastTime(clock.getTime()));
  }

  @Override
  public boolean isAllPsContainersFinished() {
    if (containerId2Status.isEmpty()) {
      return false;
    }
    for (Entry<XLearningContainerId, XLearningContainerStatus> e : containerId2Status.entrySet()) {
      if (containerId2Role.get(e.getKey()).equals(XLearningConstants.PS)) {
        if (e.getValue().equals(XLearningContainerStatus.UNDEFINED)
            || e.getValue().equals(XLearningContainerStatus.INITIALIZING)
            || e.getValue().equals(XLearningContainerStatus.RUNNING)) {
          return false;
        }
      }
    }
    return true;
  }

  @Override
  public boolean isTrainCompleted() {
    if (containerId2Status.isEmpty()) {
      return false;
    }

    boolean isCompleted = true;
    Double failedNum = 0.0;

    for (Entry<XLearningContainerId, XLearningContainerStatus> e : containerId2Status.entrySet()) {
      if (e.getValue().equals(XLearningContainerStatus.FAILED)) {
        LOG.error("Container " + e.getKey().toString() + " run failed!");
        failedNum += 1;
      } else if (containerId2Role.get(e.getKey()).equals(XLearningConstants.WORKER)) {
        if (e.getValue().equals(XLearningContainerStatus.UNDEFINED)
            || e.getValue().equals(XLearningContainerStatus.INITIALIZING)
            || e.getValue().equals(XLearningContainerStatus.RUNNING)) {
          isCompleted = false;
        }
      }
    }

    if ("TENSORFLOW".equals(xlearningAppType) && !this.getConfig().getBoolean(XLearningConfiguration.XLEARNING_TF_MODE_SINGLE, XLearningConfiguration.DEFAULT_XLEARNING_TF_MODE_SINGLE)) {
      if (failedNum > 0) {
        return true;
      }
    } else if ("MXNET".equals(xlearningAppType) && !this.getConfig().getBoolean(XLearningConfiguration.XLEARNING_MXNET_MODE_SINGLE, XLearningConfiguration.DEFAULT_XLEARNING_MXNET_MODE_SINGLE)) {
      if (failedNum > 0) {
        return true;
      }
    } else if ("DISTXGBOOST".equals(xlearningAppType)) {
      if (failedNum > 0) {
        return true;
      }
    } else if ("DISTLIGHTGBM".equals(xlearningAppType)) {
      if (failedNum > 0) {
        return true;
      }
    } else {
      Double jobFailedNum = containerId2Status.size() * this.getConfig().getDouble(XLearningConfiguration.XLEARNING_CONTAINER_MAX_FAILURES_RATE, XLearningConfiguration.DEFAULT_XLEARNING_CONTAINER_FAILURES_RATE);
      if (failedNum > jobFailedNum) {
        return true;
      }
    }
    return isCompleted;
  }

  @Override
  public boolean isAllWorkerContainersSucceeded() {
    if (containerId2Status.isEmpty()) {
      return false;
    }
    Double failedNum = 0.0;
    for (Entry<XLearningContainerId, XLearningContainerStatus> e : containerId2Status.entrySet()) {
      if (!e.getValue().equals(XLearningContainerStatus.SUCCEEDED)) {
        failedNum += 1;
      }
    }
    if ("TENSORFLOW".equals(xlearningAppType) && !this.getConfig().getBoolean(XLearningConfiguration.XLEARNING_TF_MODE_SINGLE, XLearningConfiguration.DEFAULT_XLEARNING_TF_MODE_SINGLE)) {
      if (failedNum > 0) {
        return false;
      }
    } else if ("MXNET".equals(xlearningAppType) && !this.getConfig().getBoolean(XLearningConfiguration.XLEARNING_MXNET_MODE_SINGLE, XLearningConfiguration.DEFAULT_XLEARNING_MXNET_MODE_SINGLE)) {
      if (failedNum > 0) {
        return false;
      }
    } else {
      Double jobFailedNum = containerId2Status.size() * this.getConfig().getDouble(XLearningConfiguration.XLEARNING_CONTAINER_MAX_FAILURES_RATE, XLearningConfiguration.DEFAULT_XLEARNING_CONTAINER_FAILURES_RATE);
      if (failedNum > jobFailedNum) {
        return false;
      }
    }
    return true;
  }

  @Override
  public int interResultCompletedNum(Long lastInnerModelStr) {
    if (containerId2InnerModel.isEmpty()) {
      return -1;
    }

    int completedNum = 0;
    for (Entry<XLearningContainerId, InnerModelSavedPair> e : containerId2InnerModel.entrySet()) {
      if (e.getValue().getInnerModelTimeStamp().equals(lastInnerModelStr) && e.getValue().getModelSavedStatus()) {
        completedNum++;
      }
    }
    LOG.debug("current interResult saved completed num:" + completedNum);
    return completedNum;
  }

  @Override
  public void reportReservedPort(String host, int port, String role, int index) {
    if (this.clusterDef.keySet().contains(role)) {
      this.clusterDef.get(role).add(new ContainerHostPair(index, host + ":" + port));
      LOG.info("Received reserved port report from " + host + " role is " +
          role + " reserved port is " + port);
    } else {
      LOG.warn("Unknow role " + role + " reported from " + host);
    }
  }

  @Override
  public void reportLightGbmIpPort(XLearningContainerId containerId, String lightGbmIpPort) {
    this.lightGBMIpPortMap.put(containerId, lightGbmIpPort);
    LOG.info("From container " + containerId.toString() + "Received reported lightGBM ip port: " + lightGbmIpPort);
  }

  @Override
  public synchronized String getLightGbmIpPortStr() {
    if (this.lightGBMIpPortMap.size() == applicationContext.getWorkerNum()) {
      LOG.info("Sending lightGBM ip port list \"" + new Gson().toJson(lightGBMIpPortMap) + "\"to container");
      this.lightGBMIpPortStr = new Gson().toJson(lightGBMIpPortMap);
    }
    return this.lightGBMIpPortStr;
  }

  @Override
  public void reportTensorBoardURL(String url) {
    this.tensorboardUrl = url;
    LOG.info("Received reported board url:" + url);
  }

  @Override
  public void reportMapedTaskID(XLearningContainerId containerId, String taskid) {
    this.mapedTaskID.put(containerId, taskid);
    LOG.info("STREAM containerId is:" + containerId.toString() + " taskid is:" + taskid);
  }


  @Override
  public void reportGpuMemeoryUsed(XLearningContainerId containerId, String gpuMemeoryUsed) {
    if (this.containersGpuMemMetrics.get(containerId).size() == 0) {
      Type type = new TypeToken<ConcurrentHashMap<String, List<Long>>>() {
      }.getType();
      ConcurrentHashMap<String, List<Long>> map = new Gson().fromJson(gpuMemeoryUsed, type);
      for (String str : map.keySet()) {
        LinkedBlockingDeque<List<Long>> queue = new LinkedBlockingDeque<>();
        queue.add(map.get(str));
        this.containersGpuMemMetrics.get(containerId).put(str, queue);
      }
    } else {
      Type type = new TypeToken<ConcurrentHashMap<String, List<Long>>>() {
      }.getType();
      ConcurrentHashMap<String, List<Long>> map = new Gson().fromJson(gpuMemeoryUsed, type);
      for (String str : map.keySet()) {
        if (this.containersGpuMemMetrics.get(containerId).keySet().contains(str)) {
          if (this.containersGpuMemMetrics.get(containerId).get(str).size() < 1800) {
            this.containersGpuMemMetrics.get(containerId).get(str).add(map.get(str));
          } else {
            this.containersGpuMemMetrics.get(containerId).get(str).poll();
            this.containersGpuMemMetrics.get(containerId).get(str).add(map.get(str));
          }
        } else {
          LinkedBlockingDeque<List<Long>> queue = new LinkedBlockingDeque<>();
          queue.add(map.get(str));
          this.containersGpuMemMetrics.get(containerId).put(str, queue);
        }
      }
    }
  }

  @Override
  public void reportGpuUtilization(XLearningContainerId containerId, String gpuUtilization) {
    if (this.containersGpuUtilMetrics.get(containerId).size() == 0) {
      Type type = new TypeToken<ConcurrentHashMap<String, List<Long>>>() {
      }.getType();
      ConcurrentHashMap<String, List<Long>> map = new Gson().fromJson(gpuUtilization, type);
      for (String str : map.keySet()) {
        LinkedBlockingDeque<List<Long>> queue = new LinkedBlockingDeque<>();
        queue.add(map.get(str));
        this.containersGpuUtilMetrics.get(containerId).put(str, queue);
      }
    } else {
      Type type = new TypeToken<ConcurrentHashMap<String, List<Long>>>() {
      }.getType();
      ConcurrentHashMap<String, List<Long>> map = new Gson().fromJson(gpuUtilization, type);
      for (String str : map.keySet()) {
        if (this.containersGpuUtilMetrics.get(containerId).keySet().contains(str)) {
          if (this.containersGpuUtilMetrics.get(containerId).get(str).size() < 1800) {
            this.containersGpuUtilMetrics.get(containerId).get(str).add(map.get(str));
          } else {
            this.containersGpuUtilMetrics.get(containerId).get(str).poll();
            this.containersGpuUtilMetrics.get(containerId).get(str).add(map.get(str));
          }
        } else {
          LinkedBlockingDeque<List<Long>> queue = new LinkedBlockingDeque<>();
          queue.add(map.get(str));
          this.containersGpuUtilMetrics.get(containerId).put(str, queue);
        }
      }
    }
  }

  @Override
  public void reportCpuMetrics(XLearningContainerId containerId, String cpuMetrics) {
    if (this.containersCpuMetrics.get(containerId).size() == 0) {
      Gson gson = new GsonBuilder()
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
      ConcurrentHashMap<String, Object> map = gson.fromJson(cpuMetrics, type);
      for (String str : map.keySet()) {
        LinkedBlockingDeque<Object> queue = new LinkedBlockingDeque<>();
        queue.add(map.get(str));
        this.containersCpuMetrics.get(containerId).put(str, queue);
      }
    } else {
      Gson gson = new GsonBuilder()
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
      ConcurrentHashMap<String, Object> map = gson.fromJson(cpuMetrics, type);
      for (String str : map.keySet()) {
        if (this.containersCpuMetrics.get(containerId).keySet().contains(str)) {
          if (this.containersCpuMetrics.get(containerId).get(str).size() < 1800) {
            this.containersCpuMetrics.get(containerId).get(str).add(map.get(str));
          } else {
            this.containersCpuMetrics.get(containerId).get(str).poll();
            this.containersCpuMetrics.get(containerId).get(str).add(map.get(str));
          }
        } else {
          LinkedBlockingDeque<Object> queue = new LinkedBlockingDeque<>();
          queue.add(map.get(str));
          this.containersCpuMetrics.get(containerId).put(str, queue);
        }
      }
    }
  }

  @Override
  public void reportGPUDevice(XLearningContainerId containerId, String containerGPUDevice){
    try {
      LOG.info("Get container " + containerId.toString() + " GPU Device ID: " + containerGPUDevice);
      containerId2GPU.put(containerId, containerGPUDevice);
    } catch (Exception e) {
      LOG.error("Get the container " + containerId.toString() + " GPU Device ID failed, ", e);
    }
  }

  public String getContainerGPUDevice(XLearningContainerId containerId){
    if (containerId2GPU.containsKey(containerId)) {
      return containerId2GPU.get(containerId);
    }
    return null;
  }

  @Override
  public synchronized Long interResultTimeStamp() {
    return this.interResultTimeStamp;
  }

  @Override
  public synchronized String getClusterDef() {
    if (this.clusterDef.get(XLearningConstants.WORKER.toString()).size() == applicationContext.getWorkerNum()
        && this.clusterDef.get(XLearningConstants.PS.toString()).size() == applicationContext.getPsNum()) {
      if (this.clusterDefStr == null) {
        Collections.sort(this.clusterDef.get(XLearningConstants.PS.toString()), new compairIndex());
        Collections.sort(this.clusterDef.get(XLearningConstants.WORKER.toString()), new compairIndex());
        List workerList = new ArrayList<String>();
        List psList = new ArrayList<String>();
        for (int i = 0; i < this.clusterDef.get(XLearningConstants.WORKER.toString()).size(); i++) {
          workerList.add(this.clusterDef.get(XLearningConstants.WORKER.toString()).get(i).getHost());
        }
        for (int i = 0; i < this.clusterDef.get(XLearningConstants.PS.toString()).size(); i++) {
          psList.add(this.clusterDef.get(XLearningConstants.PS.toString()).get(i).getHost());
        }
        Map<String, List<String>> clusterMessage = new HashMap<>();
        clusterMessage.put(XLearningConstants.WORKER, workerList);
        clusterMessage.put(XLearningConstants.PS, psList);
        LOG.info("Sending cluster def \"" + new Gson().toJson(clusterMessage) + "\"to container");
        this.clusterDefStr = new Gson().toJson(clusterMessage);
      }
    }
    return this.clusterDefStr;
  }

  @Override
  public HeartbeatResponse heartbeat(XLearningContainerId containerId, HeartbeatRequest heartbeatRequest) {
    LastTime lastTime = runningContainers.get(containerId);
    if (lastTime != null) {
      lastTime.setLastTime(clock.getTime());
    }

    XLearningContainerStatus currentContainerStatus = heartbeatRequest.getXLearningContainerStatus();
    LOG.debug("Received heartbeat from container " + containerId.toString() + ", status is " + currentContainerStatus.toString());
    if (containerId2Status.get(containerId) == null || containerId2Status.get(containerId) != currentContainerStatus) {
      try {
        LOG.info("Update container " + containerId.toString() + " status to " + currentContainerStatus);
        containerId2Status.put(containerId, currentContainerStatus);
        if (currentContainerStatus.equals(XLearningContainerStatus.SUCCEEDED) || currentContainerStatus.equals(XLearningContainerStatus.FAILED)) {
          LOG.info("container " + containerId.toString() + " is " + currentContainerStatus + ", now remove from running containers");
          runningContainers.remove(containerId);
          if (containerId2InnerModel.containsKey(containerId)) {
            containerId2InnerModel.remove(containerId);
          }
        }
      } catch (Exception e) {
        LOG.error("Update container " + containerId.toString() + " status failed, ", e);
      }
    }

    if (containerId2Role.get(containerId).equals(XLearningConstants.WORKER.toString())) {
      String localProgressLog = heartbeatRequest.getProgressLog();
      if (!localProgressLog.equals("")) {
        this.reporterProgress.put(containerId, localProgressLog);
        try {
          LOG.debug("container " + containerId + " " + localProgressLog);
        } catch (Exception e) {
          LOG.error("log progress error:" + e);
        }
      }
      String localContainersStartTime = heartbeatRequest.getContainersStartTime();
      if (!localContainersStartTime.equals("")) {
        this.containersAppStartTimeMap.put(containerId, localContainersStartTime);
      }
      String localContainersFinishTime = heartbeatRequest.getContainersFinishTime();
      if (!localContainersFinishTime.equals("")) {
        this.containersAppFinishTimeMap.put(containerId, localContainersFinishTime);
      }
      if (this.isSaveInnerModel) {
        if (containerId2InnerModel.containsKey(containerId)) {
          if (!containerId2InnerModel.get(containerId).getInnerModelTimeStamp().equals(this.interResultTimeStamp)) {
            try {
              LOG.info("Update container " + containerId.toString() + " interResult to " + this.interResultTimeStamp + ", waiting ...");
              containerId2InnerModel.put(containerId, new InnerModelSavedPair(this.interResultTimeStamp, false));
            } catch (Exception e) {
              LOG.error("Update container " + containerId.toString() + " interResult failed, ", e);
            }
          } else {
            if (heartbeatRequest.getInnerModelSavedStatus()) {
              if (!containerId2InnerModel.get(containerId).getModelSavedStatus()) {
                LOG.info("container " + containerId.toString() + "saves the interResult " + this.interResultTimeStamp + " finished.");
                containerId2InnerModel.put(containerId, new InnerModelSavedPair(this.interResultTimeStamp, true));
              }
            }
          }
        }
      }
      return new HeartbeatResponse(isXLearningTrainFinished, this.interResultTimeStamp);
    } else {
      String localContainersStartTime = heartbeatRequest.getContainersStartTime();
      if (!localContainersStartTime.equals("")) {
        this.containersAppStartTimeMap.put(containerId, localContainersStartTime);
      }
      String localContainersFinishTime = heartbeatRequest.getContainersFinishTime();
      if (!localContainersFinishTime.equals("")) {
        this.containersAppFinishTimeMap.put(containerId, localContainersFinishTime);
      }
      return new HeartbeatResponse(isXLearningTrainFinished, Long.MIN_VALUE);
    }
  }

  @Override
  public InputInfo[] getInputSplit(XLearningContainerId containerId) {
    int inputInfoSize = applicationContext.getInputs(containerId).size();
    return applicationContext.getInputs(containerId).toArray(new InputInfo[inputInfoSize]);
  }

  @Override
  public InputSplit[] getStreamInputSplit(XLearningContainerId containerId) {
    int inputSplitSize = applicationContext.getStreamInputs(containerId).size();
    return applicationContext.getStreamInputs(containerId).toArray(new InputSplit[inputSplitSize]);
  }

  @Override
  public OutputInfo[] getOutputLocation() {
    return applicationContext.getOutputs().toArray(new OutputInfo[0]);
  }

  @Override
  public long getProtocolVersion(String protocol, long clientVersion) throws IOException {
    return ApplicationContainerProtocol.versionID;
  }

  @Override
  public ProtocolSignature getProtocolSignature(
      String protocol, long clientVersion, int clientMethodsHash) throws IOException {
    return ProtocolSignature.getProtocolSignature(this, protocol,
        clientVersion, clientMethodsHash);
  }

  private static class LastTime {
    private long lastTime;

    public LastTime(long time) {
      setLastTime(time);
    }

    public synchronized void setLastTime(long time) {
      lastTime = time;
    }

    public synchronized long getLastTime() {
      return lastTime;
    }

  }

  private class TimeoutMonitor implements Runnable {

    @Override
    public void run() {
      while (!Thread.currentThread().isInterrupted()) {
        long currentTime = clock.getTime();
        Set<Entry<XLearningContainerId, LastTime>> entrySet = runningContainers.entrySet();
        for (Entry<XLearningContainerId, LastTime> entry : entrySet) {
          if(containerId2Status.get(entry.getKey()).equals(XLearningContainerStatus.UNDEFINED)) {
            if (currentTime > (entry.getValue().getLastTime() + localResourceTimeOut)) {
              LOG.info("Container " + entry.getKey().toString() + " local resource timed out after "
                  + localResourceTimeOut / 1000 + " seconds");
              HeartbeatRequest heartbeatRequest = new HeartbeatRequest();
              heartbeatRequest.setXLearningContainerStatus(XLearningContainerStatus.FAILED);
              heartbeat(entry.getKey(), heartbeatRequest);
            }
          }  else {
            if (currentTime > (entry.getValue().getLastTime() + containerTimeOut)) {
              LOG.info("Container " + entry.getKey().toString() + " timed out after "
                  + containerTimeOut / 1000 + " seconds");
              HeartbeatRequest heartbeatRequest = new HeartbeatRequest();
              heartbeatRequest.setXLearningContainerStatus(XLearningContainerStatus.FAILED);
              heartbeat(entry.getKey(), heartbeatRequest);
            }
          }
        }
        try {
          Thread.sleep(monitorInterval);
        } catch (InterruptedException e) {
          LOG.warn("ContainerHeartbeatHandler thread interrupted");
          break;
        }
      }
    }
  }

  private class ContainerHostPair {
    private int index;
    private String host;

    public ContainerHostPair(int index, String host) {
      setId(index);
      setHost(host);
    }

    public void setId(int index) {
      this.index = index;
    }

    public void setHost(String host) {
      this.host = host;
    }

    public int getId() {
      return this.index;
    }

    public String getHost() {
      return this.host;
    }
  }

  private class compairIndex implements Comparator<ContainerHostPair> {
    @Override
    public int compare(ContainerHostPair a, ContainerHostPair b) {
      Integer p1 = a.getId();
      Integer p2 = b.getId();
      if (p1 < p2)
        return -1;
      else if (p1 == p2)
        return 0;
      else
        return 1;
    }
  }

  private class InnerModelSavedPair {
    private Long interResultTimeStamp = Long.MIN_VALUE;
    private Boolean savedStatus = false;

    public InnerModelSavedPair() {
    }

    public InnerModelSavedPair(Long interResultTimeStamp, Boolean savedStatus) {
      this.interResultTimeStamp = interResultTimeStamp;
      this.savedStatus = savedStatus;
    }

    public Long getInnerModelTimeStamp() {
      return this.interResultTimeStamp;
    }

    public Boolean getModelSavedStatus() {
      return this.savedStatus;
    }

  }
}
