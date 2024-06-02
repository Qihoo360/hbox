package net.qihoo.hbox.api;

import net.qihoo.hbox.common.InputInfo;
import net.qihoo.hbox.common.Message;
import net.qihoo.hbox.common.OutputInfo;
import net.qihoo.hbox.container.HboxContainerId;
import net.qihoo.hbox.common.HboxContainerStatus;
import org.apache.hadoop.yarn.api.records.ApplicationId;
import org.apache.hadoop.yarn.api.records.Container;
import org.apache.hadoop.mapred.InputSplit;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;

import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;

public interface ApplicationContext {

    ApplicationId getApplicationID();

    int getWorkerNum();

    int getPsNum();

    int getWorkerMemory();

    int getChiefWorkerMemory();

    int getEvaluatorWorkerMemory();

    int getPsMemory();

    int getWorkerVCores();

    int getPsVCores();

    List<Container> getWorkerContainers();

    List<Container> getPsContainers();

    HboxContainerStatus getContainerStatus(HboxContainerId containerId);

    List<InputInfo> getInputs(HboxContainerId containerId);

    List<InputSplit> getStreamInputs(HboxContainerId containerId);

    List<OutputInfo> getOutputs();

    LinkedBlockingQueue<Message> getMessageQueue();

    String getTensorBoardUrl();

    Map<HboxContainerId, String> getReporterProgress();

    Map<HboxContainerId, String> getContainersAppStartTime();

    Map<HboxContainerId, String> getContainersAppFinishTime();

    Map<HboxContainerId, String> getMapedTaskID();

    Map<HboxContainerId, ConcurrentHashMap<String, LinkedBlockingDeque<Object>>> getContainersCpuMetrics();

    Map<HboxContainerId, ConcurrentHashMap<String, List<Double>>> getContainersCpuStatistics();

    int getSavingModelStatus();

    int getSavingModelTotalNum();

    Boolean getStartSavingStatus();

    void startSavingModelStatus(Boolean flag);

    Boolean getLastSavingStatus();

    List<Long> getModelSavingList();

    String getTfEvaluatorId();

    String getChiefWorkerId();

    Boolean getChiefWorker();

}
