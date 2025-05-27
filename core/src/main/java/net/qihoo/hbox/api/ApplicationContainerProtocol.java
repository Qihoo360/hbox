// Copyright 2017-2025 Qihoo Inc
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package net.qihoo.hbox.api;

import net.qihoo.hbox.common.*;
import net.qihoo.hbox.container.HboxContainerId;
import org.apache.hadoop.ipc.VersionedProtocol;
import org.apache.hadoop.mapred.InputSplit;

public interface ApplicationContainerProtocol extends VersionedProtocol {

    public static final long versionID = 1L;

    void reportReservedPort(String host, int port, String role, int index);

    void reportLightGbmIpPort(HboxContainerId containerId, String lightGbmIpPort);

    void reportLightLdaIpPort(HboxContainerId containerId, String lightLdaIpPort);

    void reportTorchRank0IP(String ip);

    String getTorchRank0IP();

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
