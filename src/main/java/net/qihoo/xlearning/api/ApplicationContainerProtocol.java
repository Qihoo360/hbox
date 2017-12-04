package net.qihoo.xlearning.api;

import net.qihoo.xlearning.common.*;
import net.qihoo.xlearning.container.XLearningContainerId;
import org.apache.hadoop.ipc.VersionedProtocol;
import org.apache.hadoop.mapred.InputSplit;

public interface ApplicationContainerProtocol extends VersionedProtocol {

  public static final long versionID = 1L;

  void reportReservedPort(String host, int port, String role, int index);

  String getClusterDef();

  HeartbeatResponse heartbeat(XLearningContainerId containerId, HeartbeatRequest heartbeatRequest);

  InputInfo[] getInputSplit(XLearningContainerId containerId);

  InputSplit[] getStreamInputSplit(XLearningContainerId containerId);

  OutputInfo[] getOutputLocation();

  void reportTensorBoardURL(String url);

  void reportMapedTaskID(XLearningContainerId containerId, String taskId);

  Long interResultTimeStamp();

}
