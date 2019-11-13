package net.qihoo.hbox.container;

import com.google.gson.Gson;
import net.qihoo.hbox.api.ApplicationContainerProtocol;
import net.qihoo.hbox.util.Utilities;
import org.apache.commons.lang.StringUtils;
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
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by ouyangwen-it on 2017/11/30.
 */


public class ContainerReporter extends Thread {
    private static final Log LOG = LogFactory.getLog(ContainerReporter.class);

    private ApplicationContainerProtocol protocol;

    private Configuration conf;

    private HboxContainerId containerId;

    private String gpuStr;

    private String hboxCmdProcessId;

    private String containerProcessId;

    private Boolean dockerFlag;

    private Class<? extends ResourceCalculatorProcessTree> processTreeClass;

    private ConcurrentHashMap<String, List<Long>> gpuMemoryUsed;

    private ConcurrentHashMap<String, List<Long>> gpuUtilization;

    private ConcurrentHashMap<String, List> cpuMetrics;


    public ContainerReporter(ApplicationContainerProtocol protocol, Configuration conf,
                             HboxContainerId hboxContainerId, String gpuStr, String hboxCmdProcessId, Boolean dockerFlag) {
        this.protocol = protocol;
        this.conf = conf;
        this.containerId = hboxContainerId;
        this.gpuStr = gpuStr;
        this.hboxCmdProcessId = hboxCmdProcessId;
        this.dockerFlag = dockerFlag;
        this.containerProcessId = null;
        this.processTreeClass = conf.getClass(YarnConfiguration.NM_CONTAINER_MON_PROCESS_TREE, null,
                ResourceCalculatorProcessTree.class);
        this.gpuMemoryUsed = new ConcurrentHashMap<>();
        this.gpuUtilization = new ConcurrentHashMap<>();
        this.cpuMetrics = new ConcurrentHashMap<>();
    }

    public void run() {
        if (!this.gpuStr.equals("")) {
            try {
                produceGpuMetrics(this.gpuStr, this.dockerFlag);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            if (this.dockerFlag) {
                produceVPCCpuMetrics();
            } else {
                produceCpuMetrics(this.hboxCmdProcessId);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        while (true) {
            Utilities.sleep(3000);
            if (!this.gpuStr.equals("")) {
                try {
                    protocol.reportGpuMemeoryUsed(containerId, new Gson().toJson(gpuMemoryUsed));
                    protocol.reportGpuUtilization(containerId, new Gson().toJson(gpuUtilization));
                } catch (Exception e) {
                    LOG.debug("report gpu metrics exception:" + e);
                }
            }
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
        private int gpuCores;

        public ProcessTreeInfo(ContainerId containerId, String pid,
                               ResourceCalculatorProcessTree pTree, long vmemLimit, long pmemLimit,
                               int cpuVcores, int gpuCores) {
            this.containerId = containerId;
            this.pid = pid;
            this.pTree = pTree;
            this.vmemLimit = vmemLimit;
            this.pmemLimit = pmemLimit;
            this.cpuVcores = cpuVcores;
            this.gpuCores = gpuCores;
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

        public int getGpuCores() {
            return this.gpuCores;
        }
    }

    private void produceGpuMetrics(String gpuStr, final Boolean dockerFlag) throws IOException {
        String command = "nvidia-smi --format=csv,noheader,nounits --query-gpu=index,memory.used,utilization.gpu -l 1";
        final String[] gpuList = StringUtils.split(gpuStr, ',');
        final Process finalProcess = Runtime.getRuntime().exec(command);
        LOG.info("Starting thread to redirect stdout of nvidia-smi process");
        Thread stdoutRedirectThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    BufferedReader reader;
                    reader = new BufferedReader(new InputStreamReader(finalProcess.getInputStream()));
                    String line;
                    int i;
                    Boolean flag;
                    while ((line = reader.readLine()) != null) {
                        Long time = (new Date()).getTime();
                        String[] gpusIndex = StringUtils.split(line, ',');
                        for (i = 0; i < gpuList.length; i++) {
                            if ((dockerFlag && i == Integer.parseInt(gpusIndex[0])) || (!dockerFlag && gpuList[i].equals(gpusIndex[0])))
                                flag = true;
                            else
                                flag = false;
                            if (flag) {
                                List<Long> memPoint = new ArrayList<>();
                                memPoint.add(time);
                                memPoint.add(Long.parseLong(gpusIndex[1].trim()));
                                List<Long> utilPoint = new ArrayList<>();
                                utilPoint.add(time);
                                utilPoint.add(Long.parseLong(gpusIndex[2].trim()));
                                gpuMemoryUsed.put(gpuList[i], memPoint);
                                gpuUtilization.put(gpuList[i], utilPoint);
                                break;
                            }
                        }
                    }
                } catch (Exception e) {
                    LOG.warn("Exception in thread nvidia-smi stdoutRedirectThread");
                    e.printStackTrace();
                }
            }
        });
        stdoutRedirectThread.start();
    }

    private void produceCpuMetrics(String hboxCmdProcessId) throws IOException {
        String command = "cat /proc/" + hboxCmdProcessId + "/stat";
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
                        null, null, 0, 0, 0, 0);
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
                        double currentPmemUsage = Double.parseDouble(df.format(pTree.getRssMemorySize() / 1024.0 / 1024.0 / 1024.0));
                        int cpuUsagePercentPerCore = (int) pTree.getCpuUsagePercent();
                        if (cpuUsagePercentPerCore < 0) {
                            cpuUsagePercentPerCore = 0;
                        }
                        if (currentPmemUsage < 0.0) {
                            currentPmemUsage = 0.0;
                        }
                        List memPoint = new ArrayList();
                        List utilPoint = new ArrayList();
                        memPoint.add(time);
                        memPoint.add(currentPmemUsage);
                        utilPoint.add(time);
                        utilPoint.add(cpuUsagePercentPerCore);
                        cpuMetrics.put("CPUMEM", memPoint);
                        cpuMetrics.put("CPUUTIL", utilPoint);
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

    private void produceVPCCpuMetrics() throws IOException {
        final String command = "top -bcn 1";
        LOG.info("Starting thread to redirect stdout of top process");
        Thread stdoutRedirectThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while (true) {
                        final Process finalProcess = Runtime.getRuntime().exec(command);
                        BufferedReader reader;
                        reader = new BufferedReader(new InputStreamReader(finalProcess.getInputStream()));
                        String line;
                        Long time = (new Date()).getTime();
                        double currentPmemUsage = 0.0;
                        int cpuUsagePercentPerCore = 0;
                        while ((line = reader.readLine()) != null) {
                            String[] topInfo = StringUtils.split(line.trim(), " ");
                            if (topInfo.length > 0 && topInfo[1].toLowerCase().equals("root")) {
                                if (topInfo[5].trim().contains("g")) {
                                    currentPmemUsage += Double.parseDouble(topInfo[5].trim().split("g")[0].trim());
                                } else if (topInfo[5].trim().contains("m")) {
                                    currentPmemUsage += Double.parseDouble(topInfo[5].trim().split("g")[0].trim()) / 1024.0;
                                } else {
                                    currentPmemUsage += Double.parseDouble(topInfo[5].trim()) / 1024.0 / 1024.0;
                                }
                                cpuUsagePercentPerCore += (int) Double.parseDouble(topInfo[8].trim());
                            }
                        }
                        if (cpuUsagePercentPerCore < 0) {
                            cpuUsagePercentPerCore = 0;
                        }
                        if (currentPmemUsage < 0.0) {
                            currentPmemUsage = 0.0;
                        }
                        List memPoint = new ArrayList();
                        List utilPoint = new ArrayList();
                        memPoint.add(time);
                        memPoint.add(currentPmemUsage);
                        utilPoint.add(time);
                        utilPoint.add(cpuUsagePercentPerCore);
                        cpuMetrics.put("CPUMEM", memPoint);
                        cpuMetrics.put("CPUUTIL", utilPoint);
                        Utilities.sleep(1000);
                    }
                } catch (Exception e) {
                    LOG.warn("Exception in thread docker stats stdoutRedirectThread");
                    e.printStackTrace();
                }
            }
        });
        stdoutRedirectThread.start();
    }


}
