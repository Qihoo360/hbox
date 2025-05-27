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

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import net.qihoo.hbox.common.HboxContainerStatus;
import net.qihoo.hbox.common.InputInfo;
import net.qihoo.hbox.common.Message;
import net.qihoo.hbox.common.OutputInfo;
import net.qihoo.hbox.container.HboxContainerId;
import org.apache.hadoop.yarn.api.records.ApplicationId;
import org.apache.hadoop.yarn.api.records.Container;

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
