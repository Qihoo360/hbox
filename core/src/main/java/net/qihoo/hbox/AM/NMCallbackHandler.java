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
package net.qihoo.hbox.AM;

import java.nio.ByteBuffer;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.yarn.api.records.ContainerId;
import org.apache.hadoop.yarn.api.records.ContainerStatus;
import org.apache.hadoop.yarn.client.api.async.NMClientAsync.CallbackHandler;

public class NMCallbackHandler implements CallbackHandler {
    private static final Log LOG = LogFactory.getLog(NMCallbackHandler.class);

    @Override
    public void onContainerStarted(ContainerId containerId, Map<String, ByteBuffer> allServiceResponse) {
        LOG.info("Container " + containerId.toString() + " started");
    }

    @Override
    public void onContainerStatusReceived(ContainerId containerId, ContainerStatus containerStatus) {
        LOG.info("Container " + containerId.toString() + " status " + containerStatus.toString() + " received");
    }

    @Override
    public void onContainerStopped(ContainerId containerId) {
        LOG.info("Container " + containerId.toString() + " stoped");
    }

    @Override
    public void onStartContainerError(ContainerId containerId, Throwable t) {
        LOG.info("Container " + containerId.toString() + " failed to start ", t);
    }

    @Override
    public void onGetContainerStatusError(ContainerId containerId, Throwable t) {
        LOG.info("Container " + containerId.toString() + " get status error ", t);
    }

    @Override
    public void onStopContainerError(ContainerId containerId, Throwable t) {
        LOG.info("Container " + containerId.toString() + " failed to stop ", t);
    }
}
