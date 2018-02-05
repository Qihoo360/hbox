package net.qihoo.xlearning.AM;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.yarn.api.records.ContainerId;
import org.apache.hadoop.yarn.api.records.ContainerStatus;
import org.apache.hadoop.yarn.api.records.Resource;
import org.apache.hadoop.yarn.client.api.async.NMClientAsync;

import java.nio.ByteBuffer;
import java.util.Map;

public class NMCallbackHandler extends NMClientAsync.AbstractCallbackHandler {
  private static final Log LOG = LogFactory.getLog(NMCallbackHandler.class);

  @Override
  public void onContainerStarted(ContainerId containerId,
                                 Map<String, ByteBuffer> allServiceResponse) {
    LOG.info("Container " + containerId.toString() + " started");
  }

  @Override
  public void onContainerStatusReceived(ContainerId containerId,
                                        ContainerStatus containerStatus) {
    LOG.info("Container " + containerId.toString() + " status " + containerStatus.toString() + " received");
  }

  @Override
  public void onContainerStopped(ContainerId containerId) {
    LOG.info("Container " + containerId.toString() + " stopped");
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

  @Override
  public void onIncreaseContainerResourceError(
      ContainerId containerId, Throwable t) {
    LOG.info("Container " + containerId.toString() + " failed to increase the resource ", t);
  }

  @Override
  public void onContainerResourceIncreased(
      ContainerId containerId, Resource resource) {
    LOG.info("Container " + containerId.toString() + " increase the resource: " + resource.toString());
  }

  @Override
  public void onUpdateContainerResourceError(
      ContainerId containerId, Throwable t) {
    LOG.info("Container " + containerId.toString() + " failed to update the resource ", t);
  }

  @Override
  public void onContainerResourceUpdated(ContainerId containerId,
                                         Resource resource) {
    LOG.info("Container " + containerId.toString() + " update the resource: " + resource.toString());
  }

}
