package net.qihoo.xlearning.container;

import com.google.gson.Gson;
import net.qihoo.xlearning.api.ApplicationContainerProtocol;
import net.qihoo.xlearning.util.Utilities;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.yarn.api.records.ContainerId;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.apache.hadoop.yarn.util.ResourceCalculatorProcessTree;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class ContainerReporter extends Thread {
  private static final Log LOG = LogFactory.getLog(ContainerReporter.class);

  private ApplicationContainerProtocol protocol;

  private Configuration conf;

  private XLearningContainerId containerId;

  private String xlearningCmdProcessId;

  private String containerProcessId;

  private Class<? extends ResourceCalculatorProcessTree> processTreeClass;

  private ConcurrentHashMap<String, List> cpuMetrics;


  public ContainerReporter(ApplicationContainerProtocol protocol, Configuration conf,
                           XLearningContainerId xlearningContainerId, String xlearningCmdProcessId) {
    this.protocol = protocol;
    this.conf = conf;
    this.containerId = xlearningContainerId;
    this.xlearningCmdProcessId = xlearningCmdProcessId;
    this.containerProcessId = null;
    this.processTreeClass = conf.getClass(YarnConfiguration.NM_CONTAINER_MON_PROCESS_TREE, null,
        ResourceCalculatorProcessTree.class);
    this.cpuMetrics = new ConcurrentHashMap<>();
  }

  public void run() {
    try {
      produceCpuMetrics(this.xlearningCmdProcessId);
    } catch (IOException e) {
      e.printStackTrace();
    }

    while (true) {
      Utilities.sleep(3000);
      try {
        protocol.reportCpuMetrics(containerId, new Gson().toJson(cpuMetrics));
      } catch (Exception e) {
        LOG.debug("report cpu metrics exception:" + e);
      }
    }
  }

  private static class ProcessTreeInfo {
    private ContainerId containerId;
    private String pid;
    private ResourceCalculatorProcessTree pTree;
    private long vmemLimit;
    private long pmemLimit;
    private int cpuVcores;

    public ProcessTreeInfo(ContainerId containerId, String pid,
                           ResourceCalculatorProcessTree pTree, long vmemLimit, long pmemLimit,
                           int cpuVcores) {
      this.containerId = containerId;
      this.pid = pid;
      this.pTree = pTree;
      this.vmemLimit = vmemLimit;
      this.pmemLimit = pmemLimit;
      this.cpuVcores = cpuVcores;
    }

    public ContainerId getContainerId() {
      return this.containerId;
    }

    public String getPID() {
      return this.pid;
    }

    public void setPid(String pid) {
      this.pid = pid;
    }

    public ResourceCalculatorProcessTree getProcessTree() {
      return this.pTree;
    }

    public void setProcessTree(ResourceCalculatorProcessTree pTree) {
      this.pTree = pTree;
    }

    public long getVmemLimit() {
      return this.vmemLimit;
    }

    /**
     * @return Physical memory limit for the process tree in bytes
     */
    public long getPmemLimit() {
      return this.pmemLimit;
    }

    /**
     * Return the number of cpu vcores assigned
     *
     * @return
     */
    public int getCpuVcores() {
      return this.cpuVcores;
    }

  }

  private void produceCpuMetrics(String xlearningCmdProcessId) throws IOException {
    String command = "cat /proc/" + xlearningCmdProcessId + "/stat";
    try {
      Process process = Runtime.getRuntime().exec(command);
      InputStream is = process.getInputStream();
      BufferedReader br = new BufferedReader(new InputStreamReader(is));
      String line;
      while ((line = br.readLine()) != null) {
        String[] strs = line.split(" ");
        this.containerProcessId = strs[3];
        break;
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    LOG.info("containerProcessId is:" + this.containerProcessId);
    ProcessTreeInfo processTreeInfo =
        new ProcessTreeInfo(this.containerId.getContainerId(),
            null, null, 0, 0, 0);
    ResourceCalculatorProcessTree pt =
        ResourceCalculatorProcessTree.getResourceCalculatorProcessTree(this.containerProcessId, this.processTreeClass, conf);
    processTreeInfo.setPid(this.containerProcessId);
    processTreeInfo.setProcessTree(pt);
    final ResourceCalculatorProcessTree pTree = processTreeInfo.getProcessTree();
    final DecimalFormat df = new DecimalFormat("#.00");
    df.setRoundingMode(RoundingMode.HALF_UP);
    LOG.info("Starting thread to read cpu metrics");
    Thread cpuMetricsThread = new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          while (true) {
            pTree.updateProcessTree();
            Long time = (new Date()).getTime();
            double currentPmemUsage = 0.0;
            try {
              Method getMemorySize = pTree.getClass().getMethod("getRssMemorySize");
              currentPmemUsage = Double.parseDouble(df.format(((long) getMemorySize.invoke(pTree)) / 1024.0 / 1024.0 / 1024.0));
            } catch (NoSuchMethodException e) {
              LOG.debug("current hadoop version don't have the method getRssMemorySize of Class " + pTree.getClass().toString() + ". For More Detail: " + e);
              currentPmemUsage = Double.parseDouble(df.format(pTree.getCumulativeRssmem() / 1024.0 / 1024.0 / 1024.0));
            }
            if (currentPmemUsage < 0.0) {
              currentPmemUsage = 0.0;
            }
            List memPoint = new ArrayList();
            memPoint.add(time);
            memPoint.add(currentPmemUsage);
            cpuMetrics.put("CPUMEM", memPoint);
            try {
              Method getCpuUsage = pTree.getClass().getMethod("getCpuUsagePercent");
              int cpuUsagePercentPerCore = (int) (float) getCpuUsage.invoke(pTree);
              if (cpuUsagePercentPerCore < 0) {
                cpuUsagePercentPerCore = 0;
              }
              List utilPoint = new ArrayList();
              utilPoint.add(time);
              utilPoint.add(cpuUsagePercentPerCore);
              cpuMetrics.put("CPUUTIL", utilPoint);
            } catch (NoSuchMethodException e) {
              LOG.debug("current hadoop version don't have the method getCpuUsagePercent of Class " + pTree.getClass().toString() + ". For More Detail: " + e);
            } catch (Exception e) {
              LOG.debug("getCpuUsagePercent Exception: " + e);
            }
            Utilities.sleep(1000);
          }
        } catch (Exception e) {
          LOG.warn("Exception in thread read cpu metrics");
          e.printStackTrace();
        }
      }
    });
    cpuMetricsThread.start();
  }

}
