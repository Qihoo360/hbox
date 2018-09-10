/**
 *
 */
package net.qihoo.hbox.AM;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.yarn.api.records.Container;
import org.apache.hadoop.yarn.api.records.ContainerStatus;
import org.apache.hadoop.yarn.api.records.NodeReport;
import org.apache.hadoop.yarn.client.api.async.AMRMClientAsync.CallbackHandler;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class RMCallbackHandler implements CallbackHandler {
  private static final Log LOG = LogFactory.getLog(RMCallbackHandler.class);

  private final List<Container> cancelContainers;

  public final List<Container> acquiredWorkerContainers;

  public final List<Container> acquiredPsContainers;

  public final Set<String> blackHosts;

  private int neededWorkerContainersCount;

  private int neededPsContainersCount;

  private final AtomicInteger acquiredWorkerContainersCount;

  private final AtomicInteger acquiredPsContainersCount;

  private final AtomicBoolean workerContainersAllocating;

  private float progress;

  public final List<Container> acquiredContainers;

  private String hboxAppType;

  private final Map<String, Container> hostToContainer;

  public RMCallbackHandler() {
    cancelContainers = Collections.synchronizedList(new ArrayList<Container>());
    acquiredWorkerContainers = Collections.synchronizedList(new ArrayList<Container>());
    acquiredPsContainers = Collections.synchronizedList(new ArrayList<Container>());
    blackHosts = Collections.synchronizedSet(new HashSet<String>());
    acquiredWorkerContainersCount = new AtomicInteger(0);
    acquiredPsContainersCount = new AtomicInteger(0);
    workerContainersAllocating = new AtomicBoolean(false);
    progress = 0.0f;
    acquiredContainers = Collections.synchronizedList(new ArrayList<Container>());
    hboxAppType = "";
    hostToContainer = new ConcurrentHashMap<>();
  }

  public List<String> getBlackHosts() {
    List<String> blackHostList = new ArrayList<>(blackHosts.size());
    for (String host : blackHosts) {
      blackHostList.add(host);
    }
    return blackHostList;
  }

  public void setHboxAppType(String appType) {
    this.hboxAppType = appType;
  }

  public void addBlackHost(String hostname) {
    blackHosts.add(hostname);
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

  public void setNeededWorkerContainersCount(int count) {
    neededWorkerContainersCount = count;
  }

  public void setNeededPsContainersCount(int count) {
    neededPsContainersCount = count;
  }

  public void setWorkerContainersAllocating() {
    workerContainersAllocating.set(true);
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
    int count = containers.size();
    if(this.hboxAppType.equals("MPI") || this.hboxAppType.equals("HOROVOD")) {
      for (Container acquiredContainer : containers) {
        LOG.info("Acquired container " + acquiredContainer.getId()
                + " on host " + acquiredContainer.getNodeId().getHost());
        acquiredContainers.add(acquiredContainer);
        String host = acquiredContainer.getNodeId().getHost();
        if (!hostToContainer.containsKey(host)) {
          hostToContainer.put(host, acquiredContainer);
          acquiredWorkerContainers.add(acquiredContainer);
          blackHosts.add(host);
        } else {
          count --;
          LOG.info("Add container " + acquiredContainer.getId() + " to cancel list");
          cancelContainers.add(acquiredContainer);
        }
      }
      acquiredWorkerContainersCount.addAndGet(count);
      LOG.info("Current acquired container " + acquiredWorkerContainersCount.get()
              + ", total needed " + neededWorkerContainersCount);
    } else {
      for (Container acquiredContainer : containers) {
        LOG.info("Acquired container " + acquiredContainer.getId()
                + " on host " + acquiredContainer.getNodeId().getHost());
        String host = acquiredContainer.getNodeId().getHost();
        if (!blackHosts.contains(host)) {
          if (workerContainersAllocating.get()) {
            acquiredWorkerContainers.add(acquiredContainer);
            acquiredWorkerContainersCount.incrementAndGet();
          } else {
            acquiredPsContainers.add(acquiredContainer);
            acquiredPsContainersCount.incrementAndGet();
          }
        } else {
          count --;
          LOG.info("Add container " + acquiredContainer.getId() + " to cancel list");
          cancelContainers.add(acquiredContainer);
        }
      }
      LOG.info("Current acquired worker container " + acquiredWorkerContainersCount.get()
              + " / " + neededWorkerContainersCount + " ps container " + acquiredPsContainersCount.get()
              + " / " + neededPsContainersCount);
    }
  }

  @Override
  public float getProgress() {
    //int totalNeededCount = neededPsContainersCount + neededWorkerContainersCount;
    //return totalNeededCount == 0 ?
        //0.0f : (acquiredWorkerContainersCount.get() + acquiredPsContainersCount.get()) / totalNeededCount;
    return  progress;
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
    LOG.info("Error from RMCallback: ", e);
  }
}
