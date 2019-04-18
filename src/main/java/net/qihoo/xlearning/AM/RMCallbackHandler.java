package net.qihoo.xlearning.AM;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.yarn.api.records.Container;
import org.apache.hadoop.yarn.api.records.ContainerStatus;
import org.apache.hadoop.yarn.api.records.NodeReport;
import org.apache.hadoop.yarn.client.api.async.AMRMClientAsync.CallbackHandler;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class RMCallbackHandler implements CallbackHandler {
  private static final Log LOG = LogFactory.getLog(RMCallbackHandler.class);

  private final List<Container> cancelContainers;

  public final List<Container> acquiredWorkerContainers;

  public final List<Container> acquiredPsContainers;

  public final List<Container> acquiredChiefWorkerContainers;

  public final List<Container> acquiredEvaluatorWorkerContainers;

  public final Set<String> blackHosts;

  private int neededWorkerContainersCount;

  private int neededPsContainersCount;

  private final AtomicInteger acquiredWorkerContainersCount;

  private final AtomicInteger acquiredPsContainersCount;

  private final AtomicBoolean workerContainersAllocating;

  private final AtomicBoolean chiefWorkerContainersAllocating;

  private final AtomicBoolean evaluatorWorkerContainersAllocating;

  private float progress;

  public RMCallbackHandler() {
    cancelContainers = Collections.synchronizedList(new ArrayList<Container>());
    acquiredWorkerContainers = Collections.synchronizedList(new ArrayList<Container>());
    acquiredPsContainers = Collections.synchronizedList(new ArrayList<Container>());
    acquiredChiefWorkerContainers = Collections.synchronizedList(new ArrayList<Container>());
    acquiredEvaluatorWorkerContainers = Collections.synchronizedList(new ArrayList<Container>());
    blackHosts = Collections.synchronizedSet(new HashSet<String>());
    acquiredWorkerContainersCount = new AtomicInteger(0);
    acquiredPsContainersCount = new AtomicInteger(0);
    workerContainersAllocating = new AtomicBoolean(false);
    chiefWorkerContainersAllocating = new AtomicBoolean(false);
    evaluatorWorkerContainersAllocating = new AtomicBoolean(false);
    progress = 0.0f;
  }

  public List<String> getBlackHosts() {
    List<String> blackHostList = new ArrayList<>(blackHosts.size());
    for (String host : blackHosts) {
      blackHostList.add(host);
    }
    return blackHostList;
  }

  public int getAllocatedWorkerContainerNumber() {
    return acquiredWorkerContainersCount.get();
  }

  public int getAllocatedPsContainerNumber() {
    return acquiredPsContainersCount.get();
  }

  public List<Container> getCancelContainer() {
    return cancelContainers;
  }

  public List<Container> getAcquiredWorkerContainer() {
    return new ArrayList<>(acquiredWorkerContainers);
  }

  public List<Container> getAcquiredPsContainer() {
    return new ArrayList<>(acquiredPsContainers);
  }

  public List<Container> getAcquiredChiefWorkerContainers() {
    return new ArrayList<>(acquiredChiefWorkerContainers);
  }

  public List<Container> getAcquiredEvaluatorWorkerContainers() {
    return new ArrayList<>(acquiredEvaluatorWorkerContainers);
  }

  public void setNeededWorkerContainersCount(int count) {
    neededWorkerContainersCount = count;
  }

  public void setNeededPsContainersCount(int count) {
    neededPsContainersCount = count;
  }

  public void setWorkerContainersAllocating() {
    workerContainersAllocating.set(true);
  }

  public void setChiefWorkerContainersAllocating(){
    chiefWorkerContainersAllocating.set(true);
  }

  public void setEvaluatorWorkerContainersAllocating(){
    evaluatorWorkerContainersAllocating.set(true);
  }

  @Override
  public void onContainersCompleted(List<ContainerStatus> containerStatuses) {
    for (ContainerStatus containerStatus : containerStatuses) {
      LOG.info("Container " + containerStatus.getContainerId() + " completed with status "
          + containerStatus.getState().toString());
    }
  }

  @Override
  public void onContainersAllocated(List<Container> containers) {
    for (Container acquiredContainer : containers) {
      LOG.info("Acquired container " + acquiredContainer.getId()
          + " on host " + acquiredContainer.getNodeId().getHost()
          + " , with the resource " + acquiredContainer.getResource().toString());
      String host = acquiredContainer.getNodeId().getHost();
      if (!blackHosts.contains(host)) {
        if (evaluatorWorkerContainersAllocating.get()) {
          acquiredEvaluatorWorkerContainers.add(acquiredContainer);
        } else if (chiefWorkerContainersAllocating.get()) {
          acquiredChiefWorkerContainers.add(acquiredContainer);
        } else if (workerContainersAllocating.get()) {
          acquiredWorkerContainers.add(acquiredContainer);
          acquiredWorkerContainersCount.incrementAndGet();
        } else {
          acquiredPsContainers.add(acquiredContainer);
          acquiredPsContainersCount.incrementAndGet();
        }
      } else {
        LOG.info("Add container " + acquiredContainer.getId() + " to cancel list");
        cancelContainers.add(acquiredContainer);
      }
    }
    LOG.info("Current acquired worker container " + acquiredWorkerContainersCount.get()
        + " / " + neededWorkerContainersCount + " ps container " + acquiredPsContainersCount.get()
        + " / " + neededPsContainersCount);
  }

  @Override
  public float getProgress() {
    return progress;
  }

  public void setProgress(float reportProgress) {
    this.progress = reportProgress;
  }

  @Override
  public void onShutdownRequest() {
  }

  @Override
  public void onNodesUpdated(List<NodeReport> updatedNodes) {
  }

  @Override
  public void onError(Throwable e) {
    LOG.error("Error from RMCallback: ", e);
  }
}
