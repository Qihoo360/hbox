package net.qihoo.hbox.api;

import net.qihoo.hbox.common.InputInfo;
import net.qihoo.hbox.common.Message;
import net.qihoo.hbox.common.OutputInfo;
import net.qihoo.hbox.container.HboxContainerId;
import net.qihoo.hbox.common.HboxContainerStatus;
import org.apache.hadoop.yarn.api.records.ApplicationId;
import org.apache.hadoop.yarn.api.records.Container;
import org.apache.hadoop.mapred.InputSplit;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;

public interface ApplicationContext {

    ApplicationId getApplicationID();

    String getAppType();

    String getAppUser();

    int getWorkerNum();

    int getPsNum();

    int getWorkerGcores();

    int getPsGcores();

    int getWorkerMemory();

    int getPsMemory();

    int getWorkerVCores();

    int getPsVCores();

    int getChiefWorkerMemory();

    int getEvaluatorWorkerMemory();

    List<Container> getWorkerContainers();

    List<Container> getPsContainers();

    HboxContainerStatus getContainerStatus(HboxContainerId containerId);

    String getContainerGPUDevice(HboxContainerId containerId);

    List<InputInfo> getInputs(HboxContainerId containerId);

    Map<String, InputInfo> getWholeInputs();

    List<InputSplit> getStreamInputs(HboxContainerId containerId);

    List<OutputInfo> getOutputs();

    LinkedBlockingQueue<Message> getMessageQueue();

    String getTensorBoardUrl();

    Map<HboxContainerId, String> getVPCCommandAndPasswdMap();

    Map<HboxContainerId, String> getDigitsUrlMap();

    Map<HboxContainerId, String> getReporterProgress();

    Map<HboxContainerId, String> getContainersAppStartTime();

    Map<HboxContainerId, String> getContainersAppFinishTime();

    Map<HboxContainerId, String> getMapedTaskID();

    Map<HboxContainerId, ConcurrentHashMap<String, LinkedBlockingDeque<List<Long>>>> getContainersGpuMemMetrics();

    Map<HboxContainerId, ConcurrentHashMap<String, LinkedBlockingDeque<List<Long>>>> getContainersGpuUtilMetrics();

    Map<HboxContainerId, ConcurrentHashMap<String, LinkedBlockingDeque<Object>>> getContainersCpuMetrics();

    Map<HboxContainerId, ConcurrentHashMap<String, List<Double>>> getContainersGpuMemStatistics();

    Map<HboxContainerId, ConcurrentHashMap<String, List<Double>>> getContainersGpuUtilStatistics();

    Map<HboxContainerId, ConcurrentHashMap<String, List<Double>>> getContainersCpuStatistics();

    int getSavingModelStatus();

    int getSavingModelTotalNum();

    Boolean getStartSavingStatus();

    void startSavingModelStatus(Boolean flag);

    Boolean getLastSavingStatus();

    String getLastInterSavingPath();

    List<Long> getModelSavingList();

    Boolean getContainerStarted();

    String getTfEvaluatorId();

    String getChiefWorkerId();

    String getSchedulerId();

    Boolean getChiefWorker();

    String getAMContainerID();

    String getContainerStdOut(HboxContainerId cid);

    String getContainerStdErr(HboxContainerId cid);

    void sendSignal(int sid);

}
