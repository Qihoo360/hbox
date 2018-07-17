package net.qihoo.xlearning.api;

import net.qihoo.xlearning.common.InputInfo;
import net.qihoo.xlearning.common.Message;
import net.qihoo.xlearning.common.OutputInfo;
import net.qihoo.xlearning.container.XLearningContainerId;
import net.qihoo.xlearning.common.XLearningContainerStatus;
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

  int getPsMemory();

  int getWorkerVCores();

  int getPsVCores();

  List<Container> getWorkerContainers();

  List<Container> getPsContainers();

  XLearningContainerStatus getContainerStatus(XLearningContainerId containerId);

  List<InputInfo> getInputs(XLearningContainerId containerId);

  List<InputSplit> getStreamInputs(XLearningContainerId containerId);

  List<OutputInfo> getOutputs();

  LinkedBlockingQueue<Message> getMessageQueue();

  String getTensorBoardUrl();

  Map<XLearningContainerId, String> getReporterProgress();

  Map<XLearningContainerId, String> getContainersAppStartTime();

  Map<XLearningContainerId, String> getContainersAppFinishTime();

  Map<XLearningContainerId, String> getMapedTaskID();

  Map<XLearningContainerId, ConcurrentHashMap<String, LinkedBlockingDeque<Object>>> getContainersCpuMetrics();

  Map<XLearningContainerId, ConcurrentHashMap<String, List<Double>>> getContainersCpuStatistics();

  int getSavingModelStatus();

  int getSavingModelTotalNum();

  Boolean getStartSavingStatus();

  void startSavingModelStatus(Boolean flag);

  Boolean getLastSavingStatus();

  List<Long> getModelSavingList();

}
