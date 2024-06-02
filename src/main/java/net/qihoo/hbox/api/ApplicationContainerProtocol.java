package net.qihoo.hbox.api;

import net.qihoo.hbox.common.*;
import net.qihoo.hbox.container.HboxContainerId;
import org.apache.hadoop.ipc.VersionedProtocol;
import org.apache.hadoop.mapred.InputSplit;

public interface ApplicationContainerProtocol extends VersionedProtocol {

    public static final long versionID = 1L;

    void reportReservedPort(String host, int port, String role, int index);

    void reportLightGbmIpPort(HboxContainerId containerId, String lightGbmIpPort);

    String getLightGbmIpPortStr();

    void reportLightLDAIpPort(HboxContainerId containerId, String lightLDAIpPort);

    String getLightLDAIpPortStr();

    String getClusterDef();

    HeartbeatResponse heartbeat(HboxContainerId containerId, HeartbeatRequest heartbeatRequest);

    InputInfo[] getInputSplit(HboxContainerId containerId);

    InputSplit[] getStreamInputSplit(HboxContainerId containerId);

    OutputInfo[] getOutputLocation();

    void reportTensorBoardURL(String url);

    void reportMapedTaskID(HboxContainerId containerId, String taskId);

    void reportCpuMetrics(HboxContainerId containerId, String cpuMetrics);

    Long interResultTimeStamp();

    int isApplicationCompleted();

}
