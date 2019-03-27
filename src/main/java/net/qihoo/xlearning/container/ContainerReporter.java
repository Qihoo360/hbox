package net.qihoo.xlearning.container;

import com.google.gson.Gson;
import java.io.File;
import java.io.PrintWriter;
import jnr.unixsocket.UnixSocket;
import jnr.unixsocket.UnixSocketAddress;
import jnr.unixsocket.UnixSocketChannel;
import net.qihoo.xlearning.api.ApplicationContainerProtocol;
import net.qihoo.xlearning.conf.XLearningConfiguration;
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
import org.json.JSONObject;

public class ContainerReporter extends Thread {
  private static final Log LOG = LogFactory.getLog(ContainerReporter.class);

  private ApplicationContainerProtocol protocol;

  private Configuration conf;

  private XLearningContainerId containerId;

  private String xlearningCmdProcessId;

  private String containerProcessId;

  private Class<? extends ResourceCalculatorProcessTree> processTreeClass;

  private ConcurrentHashMap<String, List> cpuMetrics;

  private String containerType;

  private JSONObject dockerMetric;

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
    this.containerType = conf.get(XLearningConfiguration.XLEARNING_CONTAINER_TYPE,
        XLearningConfiguration.DEFAULT_XLEARNING_CONTAINER_TYPE);

  }

  public void run() {
    try {
      if (containerType.equalsIgnoreCase("docker")) {
        dockerProduceMetrics(this.containerId.toString(), this.xlearningCmdProcessId);
      } else {
        produceMetrics(this.xlearningCmdProcessId);
      }
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

  private void dockerProduceMetrics(final String containerId,final String xlearningCmdProcessId) {
    final DecimalFormat df = new DecimalFormat("#.00");
    df.setRoundingMode(RoundingMode.HALF_UP);
    LOG.info("Starting thread to read cpu metrics");
    Thread cpuMetricsThread = new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          while (true) {
            getDockerMetric(containerId);
            Long time = (new Date()).getTime();
            double currentPmemUsage = 0.0;
            currentPmemUsage = Double.parseDouble(df.format(getDockerMemory()  / 1024.0 / 1024.0 / 1024.0));
            if (currentPmemUsage < 0.0) {
              currentPmemUsage = 0.0;
            }
            List memPoint = new ArrayList();
            memPoint.add(time);
            memPoint.add(currentPmemUsage);
            cpuMetrics.put("CPUMEM", memPoint);
            try {
              double cpuUsagePercentPerCore = Double.parseDouble(df.format(getDockerCpu()));
              if (cpuUsagePercentPerCore < 0) {
                cpuUsagePercentPerCore = 0;
              }
              List utilPoint = new ArrayList();
              utilPoint.add(time);
              utilPoint.add(cpuUsagePercentPerCore);
              cpuMetrics.put("CPUUTIL", utilPoint);
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

  private void getDockerMetric(String containerId) {
    try {
      File sockFile = new File("/var/run/docker.sock");
      UnixSocketAddress address = new UnixSocketAddress(sockFile);
      UnixSocketChannel channel = UnixSocketChannel.open(address);
      UnixSocket unixSocket = new UnixSocket(channel);
      PrintWriter w = new PrintWriter(unixSocket.getOutputStream());
      w.println("GET /v1.27/containers/" + containerId + "/stats?stream=False HTTP/1.1");
      w.println("Host: http");
      w.println("Accept: */*");
      w.println("");
      w.flush();
      BufferedReader br = new BufferedReader(new InputStreamReader(unixSocket.getInputStream()));
      String line;
      while ((line = br.readLine()) != null) {
        if (line.trim().equals("")) {
          br.readLine();
          unixSocket.shutdownOutput();
          line = br.readLine();
          br.readLine();
          break;
        }
      }
      unixSocket.close();
      if (line != null && !line.trim().equals("")) {
        dockerMetric = new JSONObject(line);
      }
    } catch (Exception e) {
      LOG.error("Get docker metric error:", e);
    }
  }

  private long getDockerMemory() {
    long result = 0;
    try {
      result = dockerMetric.getJSONObject("memory_stats").getLong("usage");
    } catch (Exception e) {
      LOG.warn("Docker Metric Error:", e);
    }
    return result;
  }

  private double getDockerCpu() {
    double result = 0;
    try {
      int cpuNum = dockerMetric.getJSONObject("cpu_stats").getInt("online_cpus");
      long cpuUsage = dockerMetric.getJSONObject("cpu_stats").getJSONObject("cpu_usage")
          .getLong("total_usage");
      long systemCpuUsage = dockerMetric.getJSONObject("cpu_stats").getLong("system_cpu_usage");
      long perCpuUsage = dockerMetric.getJSONObject("precpu_stats").getJSONObject("cpu_usage")
          .getLong("total_usage");
      long perSystemCpuUsage = dockerMetric.getJSONObject("precpu_stats")
          .getLong("system_cpu_usage");
      result = ((double) (cpuUsage - perCpuUsage) / (double) (systemCpuUsage - perSystemCpuUsage))
          * cpuNum * 100.0;
    } catch (Exception e) {
      LOG.warn("Docker Metric Error:", e);
    }
    return result;
  }


  private void produceMetrics(String xlearningCmdProcessId) throws IOException {
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
