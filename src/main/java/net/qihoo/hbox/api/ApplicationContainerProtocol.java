package net.qihoo.hbox.api;

import net.qihoo.hbox.common.*;
import net.qihoo.hbox.container.HboxContainerId;
import org.apache.hadoop.ipc.VersionedProtocol;
import org.apache.hadoop.mapred.InputSplit;

import java.util.Map;


public interface ApplicationContainerProtocol extends VersionedProtocol {

  public static final long versionID = 1L;

  void reportReservedPort(String host, int port, String role, int index);

  void reportLightGbmIpPort(HboxContainerId containerId, String lightGbmIpPort);

  void reportLightLdaIpPort(HboxContainerId containerId, String lightLdaIpPort);

  String getClusterDef();

  String getLightGbmIpPortStr();

  String getLightLdaIpPortStr();

  void reportStatus(HboxContainerId containerId, HboxContainerStatus containerStatus);

  void reportGPUDevice(HboxContainerId containerId, String containerGPUDevice);

  HeartbeatResponse heartbeat(HboxContainerId containerId, HeartbeatRequest heartbeatRequest);

  boolean isHboxTrainCompleted();

  InputInfo[] getInputSplit(HboxContainerId containerId);

  InputInfo[] getInputWholeSplit();

  InputSplit[] getStreamInputSplit(HboxContainerId containerId);

  OutputInfo[] getOutputLocation();

  void reportTensorBoardURL(String url);

  void reportMapedTaskID(HboxContainerId containerId, String taskId);

  void reportVPCCommandAndPasswd(HboxContainerId containerId, String commandAndPasswd);

  void reportDigitsUrl(HboxContainerId containerId, String url);

  void reportGpuMemeoryUsed(HboxContainerId containerId, String gpuMemeoryUsed);

  void reportGpuUtilization(HboxContainerId containerId, String gpuUtilization);

  void reportCpuMetrics(HboxContainerId containerId, String cpuMetrics);

  Long interResultTimeStamp();

  boolean isApplicationCompleted();

  Long allContainerStartTime();

  int getSignal();

  void sendSignal(int sid);

}
