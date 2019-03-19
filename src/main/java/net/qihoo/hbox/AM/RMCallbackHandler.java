/**
 *
 */
package net.qihoo.hbox.AM;

import net.qihoo.hbox.conf.HboxConfiguration;
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

    public final List<Container> acquiredContainers;

    private String hboxAppType;

    private String containerType;

    private final Map<String, Container> hostToContainer;

    private HashMap<String, Integer> countMap = new HashMap<>();

    private int blackHostsLimit = Integer.MAX_VALUE;

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
        HboxConfiguration configuration = new HboxConfiguration();
        containerType = configuration.get(HboxConfiguration.CONTAINER_EXECUTOR_TYPE, HboxConfiguration.DEFAULT_CONTAINER_EXECUTOR_TYPE);
        //add blackHosts addition and limit number per host
        if(this.hboxAppType.equals("MPI") || this.hboxAppType.equals("HOROVOD")){
            blackHostsLimit = 1;
        }else if(containerType.equalsIgnoreCase("DOCKER")){
            blackHostsLimit = configuration.getInt(HboxConfiguration.HBOX_DOCKER_NUM_PER_WORKER, HboxConfiguration.DEDAULT_HBOX_DOCKER_NUM_PER_WORKER);
        }
        for (Container acquiredContainer : containers) {
            String host = acquiredContainer.getNodeId().getHost();
            LOG.info("Acquired container " + acquiredContainer.getId() + " on host " + host);
            if (!blackHosts.contains(host)) {
                //count and process blackHosts
                int countContainerNum = countMap.get(host) == null? 1 : countMap.get(host) + 1;
                countMap.put(host, countContainerNum);
                if(countMap.get(host) >= blackHostsLimit)
                    blackHosts.add(host);
                //add worker or ps
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
        //int totalNeededCount = neededPsContainersCount + neededWorkerContainersCount;
        //return totalNeededCount == 0 ?
        //0.0f : (acquiredWorkerContainersCount.get() + acquiredPsContainersCount.get()) / totalNeededCount;
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
        LOG.info("Error from RMCallback: ", e);
    }
}
