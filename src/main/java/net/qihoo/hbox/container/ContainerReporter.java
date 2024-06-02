package net.qihoo.hbox.container;

import com.google.gson.Gson;
import net.qihoo.hbox.api.ApplicationContainerProtocol;
import net.qihoo.hbox.conf.HboxConfiguration;
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

    private HboxContainerId containerId;

    private String hboxCmdProcessId;

    private String containerProcessId;

    private Class<? extends ResourceCalculatorProcessTree> processTreeClass;

    private ConcurrentHashMap<String, List> cpuMetrics;

    private String containerType;


    public ContainerReporter(ApplicationContainerProtocol protocol, Configuration conf,
                             HboxContainerId hboxContainerId, String hboxCmdProcessId) {
        this.protocol = protocol;
        this.conf = conf;
        this.containerId = hboxContainerId;
        this.hboxCmdProcessId = hboxCmdProcessId;
        this.containerProcessId = null;
        this.processTreeClass = conf.getClass(YarnConfiguration.NM_CONTAINER_MON_PROCESS_TREE, null,
                ResourceCalculatorProcessTree.class);
        this.cpuMetrics = new ConcurrentHashMap<>();
        this.containerType = conf.get(HboxConfiguration.HBOX_CONTAINER_TYPE,
                HboxConfiguration.DEFAULT_HBOX_CONTAINER_TYPE);
    }

    public void run() {
        try {
//        produceCpuMetricsDocker(this.containerId.toString());
            produceCpuMetrics(this.hboxCmdProcessId);
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
                        null, null, 0, 0, 0);
        ResourceCalculatorProcessTree pt =
                ResourceCalculatorProcessTree.getResourceCalculatorProcessTree(this.containerProcessId, this.processTreeClass, conf);
        processTreeInfo.setPid(this.containerProcessId);
        processTreeInfo.setProcessTree(pt);
        final ResourceCalculatorProcessTree pTree = processTreeInfo.getProcessTree();
        final DecimalFormat df = new DecimalFormat("#.00");
        df.setRoundingMode(RoundingMode.HALF_UP);
        LOG.info("Starting thread to read cpu metrics");
        final String statsCommand = "docker stats --no-stream --format {{.CPUPerc}}{{.MemUsage}} " + this.containerId.toString();
        Thread cpuMetricsThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while (true) {
                        pTree.updateProcessTree();
                        List memPoint = new ArrayList();
                        List utilPoint = new ArrayList();
                        Long time = (new Date()).getTime();
                        double currentPmemUsage = 0.0;
                        float cpuUsagePercentPerCore = 0f;
                        try {
                            Method getMemorySize = pTree.getClass().getMethod("getRssMemorySize");
                            currentPmemUsage = Double.parseDouble(df.format(((long) getMemorySize.invoke(pTree)) / 1024.0 / 1024.0 / 1024.0));
                        } catch (NoSuchMethodException e) {
                            LOG.info("current hadoop version don't have the method getRssMemorySize of Class " + pTree.getClass().toString() + ". For More Detail: " + e);
                            currentPmemUsage = Double.parseDouble(df.format(pTree.getCumulativeRssmem() / 1024.0 / 1024.0 / 1024.0));
                        }

                        try {
                            Method getCpuUsage = pTree.getClass().getMethod("getCpuUsagePercent");
                            cpuUsagePercentPerCore = (float) getCpuUsage.invoke(pTree);
                        } catch (NoSuchMethodException e) {
                            LOG.debug("current hadoop version don't have the method getCpuUsagePercent of Class " + pTree.getClass().toString() + ". For More Detail: " + e);
                        } catch (Exception e) {
                            LOG.debug("getCpuUsagePercent Exception: " + e);
                        }

                        if (containerType.equalsIgnoreCase("DOCKER")) {
                            try {
                                Runtime rt = Runtime.getRuntime();
                                Process process = rt.exec(statsCommand);
                                int i = process.waitFor();
                                LOG.debug("Docker Stats Get:" + (i == 0 ? "Success" : "Failed"));
                                if (i == 0) {
                                    BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
                                    String line;
                                    while ((line = br.readLine()) != null) {
                                        if (StringUtils.split(line, '%').length < 2)
                                            continue;
                                        String[] statsInfo = StringUtils.split(line, '%');
                                        cpuUsagePercentPerCore += Float.parseFloat(statsInfo[0].trim());
                                        String currentPmemUsageStr = statsInfo[1].trim().split("/")[0].trim();
                                        if (currentPmemUsageStr.contains("M")) {
                                            currentPmemUsage += Double.parseDouble(currentPmemUsageStr.split("M")[0].trim()) / 1024.0;
                                        } else if (currentPmemUsageStr.contains("G")) {
                                            currentPmemUsage += Double.parseDouble(currentPmemUsageStr.split("G")[0].trim());
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                LOG.warn("Exception of getting the docker stats.");
                                e.printStackTrace();
                            }
                        }

                        if (currentPmemUsage < 0.0) {
                            currentPmemUsage = 0.0;
                        }
                        memPoint.add(time);
                        memPoint.add(currentPmemUsage);
                        cpuMetrics.put("CPUMEM", memPoint);

                        if (cpuUsagePercentPerCore < 0) {
                            cpuUsagePercentPerCore = 0;
                        }
                        utilPoint.add(time);
                        utilPoint.add(cpuUsagePercentPerCore);
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

}
