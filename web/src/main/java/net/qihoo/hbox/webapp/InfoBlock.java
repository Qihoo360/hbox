package net.qihoo.hbox.webapp;

import net.qihoo.hbox.common.AMParams;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.util.StringUtils;
import org.apache.hadoop.yarn.webapp.hamlet2.Hamlet.TABLE;
import org.apache.hadoop.yarn.webapp.hamlet2.Hamlet.TBODY;
import org.apache.hadoop.yarn.webapp.hamlet2.Hamlet.TD;
import org.apache.hadoop.yarn.webapp.hamlet2.Hamlet.TR;
import org.apache.hadoop.yarn.webapp.hamlet2.Hamlet;
import org.apache.hadoop.yarn.webapp.view.HtmlBlock;

import java.text.SimpleDateFormat;
import java.util.Date;

public class InfoBlock extends HtmlBlock implements AMParams {
    private static final Log LOG = LogFactory.getLog(InfoBlock.class);

    @Override
    protected void render(Block html) {
        int numContainers = Integer.parseInt($(CONTAINER_NUMBER));
        int numWorkers = Integer.parseInt($(WORKER_NUMBER));
        int workerGcores = Integer.parseInt($("WORKER_GCORES"));
        int numPS = Integer.parseInt($(PS_NUMBER));
        int psGcores = Integer.parseInt($("PS_GCORES"));
        if (numContainers > 0) {
            TBODY<TABLE<Hamlet>> tbody = html.
                    h2("All Containers:").
                    table("#Containers").
                    thead("ui-widget-header").
                    tr().
                    th("ui-state-default", "Container ID").
                    th("ui-state-default", "Container Host").
                    th("ui-state-default", "GPU Device ID").
                    th("ui-state-default", "Container Role").
                    th("ui-state-default", "Container Status").
                    th("ui-state-default", "Start Time").
                    th("ui-state-default", "Finish Time").
                    th("ui-state-default", "Reporter Progress").
                    __().__().
                    tbody();

            for (int i = 0; i < numContainers; i++) {
                TD<TR<TBODY<TABLE<Hamlet>>>> td = tbody.
                        __().tbody("ui-widget-content").
                        tr().
                        $style("text-align:center;").td();
                td.span().$title(String.format($(CONTAINER_ID + i))).__().
                        a(String.format("http://%s/node/containerlogs/%s/%s",
                                $(CONTAINER_HTTP_ADDRESS + i),
                                $(CONTAINER_ID + i),
                                $(USER_NAME)),
                                String.format($(CONTAINER_ID + i)));
                String containerMachine = $(CONTAINER_HTTP_ADDRESS + i);

                if ($(CONTAINER_REPORTER_PROGRESS + i).equals("progress log format error")) {
                    td.__().
                            td(containerMachine.split(":")[0]).
                            td($(CONTAINER_GPU_DEVICE + i)).
                            td($(CONTAINER_ROLE + i)).
                            td($(CONTAINER_STATUS + i)).
                            td($(CONTAINER_START_TIME + i)).
                            td($(CONTAINER_FINISH_TIME + i)).
                            td($(CONTAINER_REPORTER_PROGRESS + i)).__();
                } else if ($(CONTAINER_REPORTER_PROGRESS + i).equals("0.00%")) {
                    td.__().
                            td(containerMachine.split(":")[0]).
                            td($(CONTAINER_GPU_DEVICE + i)).
                            td($(CONTAINER_ROLE + i)).
                            td($(CONTAINER_STATUS + i)).
                            td($(CONTAINER_START_TIME + i)).
                            td($(CONTAINER_FINISH_TIME + i)).
                            td("N/A").__();
                } else {
                    td.__().
                            td(containerMachine.split(":")[0]).
                            td($(CONTAINER_GPU_DEVICE + i)).
                            td($(CONTAINER_ROLE + i)).
                            td($(CONTAINER_STATUS + i)).
                            td($(CONTAINER_START_TIME + i)).
                            td($(CONTAINER_FINISH_TIME + i)).td()
                            .div().$class("ui-progressbar ui-widget ui-widget-content ui-corner-all").$title($(CONTAINER_REPORTER_PROGRESS + i))
                            .div().$class("ui-progressbar-value ui-widget-header ui-corner-left").$style("width:" + $(CONTAINER_REPORTER_PROGRESS + i))
                            .__().__().__().__();
                }

            }

            if (!$(BOARD_INFO).equals("no")) {
                if (!$(BOARD_INFO).contains("http")) {
                    tbody.__().__().div().$style("margin:20px 2px;").__(" ").__().
                            h2("View Board:").
                            table("#Board").
                            thead("ui-widget-header").
                            tr().
                            th("ui-state-default", "board Info").
                            __().__().
                            tbody("ui-widget-content").
                            tr().
                            $style("text-align:center;").
                            td(String.format($(BOARD_INFO))).
                            __().__().__();
                } else {
                    tbody.__().__().div().$style("margin:20px 2px;").__(" ").__().
                            h2("View Board:").
                            table("#Board").
                            thead("ui-widget-header").
                            tr().
                            th("ui-state-default", "board Info").
                            __().__().
                            tbody("ui-widget-content").
                            tr().
                            $style("text-align:center;").
                            td().span().$title(String.format($(BOARD_INFO))).__().
                            a(String.format($(BOARD_INFO)),
                                    String.format($(BOARD_INFO))).
                            __().__().__().__();
                }
            } else {
                tbody.__().__();
            }

            html.div().$style("margin:20px 2px;").__(" ").__();

            // resource applied info
            TBODY<TABLE<Hamlet>> resourceAppliedInfo = html.
                    h2("Resource Applied Info:").
                    table("#resourceAppliedInfo").
                    thead("ui-widget-header").
                    tr().
                    th("ui-state-default", "Role").
                    th("ui-state-default", "Number").
                    th("ui-state-default", "CPU Memory(GB)").
                    th("ui-state-default", "CPU Cores").
                    th("ui-state-default", "GPU Num").
                    __().__().
                    tbody();
            if (numWorkers > 0) {
                resourceAppliedInfo.
                        __().tbody("ui-widget-content").
                        tr().
                        $style("text-align:center;").
                        td("worker").
                        td(String.valueOf(numWorkers)).
                        td($(WORKER_MEMORY)).
                        td($(WORKER_VCORES)).
                        td(String.valueOf(workerGcores)).
                        __();
            }
            if ($("chiefWorkerMemory") != null && $("chiefWorkerMemory").trim() != "") {
                resourceAppliedInfo.
                        __().tbody("ui-widget-content").
                        tr().
                        $style("text-align:center;").
                        td("worker/chief").
                        td("-").
                        td($("chiefWorkerMemory")).
                        td("-").
                        td("-").
                        __();
            }
            if ($("evaluatorWorkerMemory") != null && $("evaluatorWorkerMemory").trim() != "") {
                resourceAppliedInfo.
                        __().tbody("ui-widget-content").
                        tr().
                        $style("text-align:center;").
                        td("worker/evaluator").
                        td("-").
                        td($("evaluatorWorkerMemory")).
                        td("-").
                        td("-").
                        __();
            }
            if (numPS > 0) {
                resourceAppliedInfo.
                        __().tbody("ui-widget-content").
                        tr().
                        $style("text-align:center;").
                        td("ps").
                        td(String.valueOf(numPS)).
                        td($(PS_MEMORY)).
                        td($(PS_VCORES)).
                        td(String.valueOf(psGcores)).
                        __();
            }
            resourceAppliedInfo.__().__();

            html.div().$style("margin:20px 2px;").__(" ").__();

            // worker/ps containers resource usage statistics info
            if (numWorkers > 0) {
                TBODY<TABLE<Hamlet>> workerCPUUsage = html.
                        h2("Worker Containers CPU Usage Info:").
                        table("#workerCPUUsageInfo").
                        thead("ui-widget-header").
                        tr().
                        th("ui-state-default", "ContainerID").
                        th("ui-state-default", "CPU memory average usages(GB)").
                        th("ui-state-default", "CPU memory max usages(GB)").
                        th("ui-state-default", "CPU utilization average usages(%)").
                        th("ui-state-default", "CPU utilization max usages(%)").
                        __().__().
                        tbody();

                for (int i = 0; i < numWorkers; i++) {
                    workerCPUUsage.
                            __().tbody("ui-widget-content").
                            tr().
                            $style("text-align:center;").
                            td($(CONTAINER_ID + i)).
                            td($(CPU_USAGE_TYPE + CONTAINER_MEM_USAGE_STATISTICS + USAGE_AVG + i)).
                            td($(CPU_USAGE_TYPE + CONTAINER_MEM_USAGE_STATISTICS + USAGE_MAX + i)).
                            td($(CPU_USAGE_TYPE + CONTAINER_UTIL_USAGE_STATISTICS + USAGE_AVG + i)).
                            td($(CPU_USAGE_TYPE + CONTAINER_UTIL_USAGE_STATISTICS + USAGE_MAX + i)).
                            __();
                }
                workerCPUUsage.__().__();

                if (workerGcores > 0) {
                    html.div().$style("margin:20px 2px;").__(" ").__();
                    TBODY<TABLE<Hamlet>> workerGPUUsage = html.
                            h2("Worker Containers GPU Usage Info:").
                            table("#workerGPUUsageInfo").
                            thead("ui-widget-header").
                            tr().
                            th("ui-state-default", "ContainerID").
                            th("ui-state-default", "GPU DEVICE ID").
                            th("ui-state-default", "GPU memory average usages(MB)").
                            th("ui-state-default", "GPU memory max usages(MB)").
                            th("ui-state-default", "GPU utilization average usages(%)").
                            th("ui-state-default", "GPU utilization max usages(%)").
                            __().__().
                            tbody();

                    for (int i = 0; i < numWorkers; i++) {
                        String gpustrs = $(CONTAINER_GPU_DEVICE + i);
                        String[] gpusIndex = StringUtils.split(gpustrs, ',');
                        for (int j = 0; j < gpusIndex.length; j++) {
                            workerGPUUsage.
                                    __().tbody("ui-widget-content").
                                    tr().
                                    $style("text-align:center;").
                                    td($(CONTAINER_ID + i)).
                                    td(gpusIndex[j]).
                                    td($(GPU_USAGE_TYPE + CONTAINER_MEM_USAGE_STATISTICS + USAGE_AVG + i + gpusIndex[j])).
                                    td($(GPU_USAGE_TYPE + CONTAINER_MEM_USAGE_STATISTICS + USAGE_MAX + i + gpusIndex[j])).
                                    td($(GPU_USAGE_TYPE + CONTAINER_UTIL_USAGE_STATISTICS + USAGE_AVG + i + gpusIndex[j])).
                                    td($(GPU_USAGE_TYPE + CONTAINER_UTIL_USAGE_STATISTICS + USAGE_MAX + i + gpusIndex[j])).
                                    __();
                        }
                    }
                    workerGPUUsage.__().__();
                }
            }

            if (numPS > 0) {
                html.div().$style("margin:20px 2px;").__(" ").__();
                TBODY<TABLE<Hamlet>> psCPUUsage = html.
                        h2("PS Containers CPU Usage Info:").
                        table("#psCPUUsageInfo").
                        thead("ui-widget-header").
                        tr().
                        th("ui-state-default", "ContainerID").
                        th("ui-state-default", "CPU memory average usages(GB)").
                        th("ui-state-default", "CPU memory max usages(GB)").
                        th("ui-state-default", "CPU utilization average usages(%)").
                        th("ui-state-default", "CPU utilization max usages(%)").
                        __().__().
                        tbody();

                for (int i = numWorkers; i < numContainers; i++) {
                    psCPUUsage.
                            __().tbody("ui-widget-content").
                            tr().
                            $style("text-align:center;").
                            td($(CONTAINER_ID + i)).
                            td($(CPU_USAGE_TYPE + CONTAINER_MEM_USAGE_STATISTICS + USAGE_AVG + i)).
                            td($(CPU_USAGE_TYPE + CONTAINER_MEM_USAGE_STATISTICS + USAGE_MAX + i)).
                            td($(CPU_USAGE_TYPE + CONTAINER_UTIL_USAGE_STATISTICS + USAGE_AVG + i)).
                            td($(CPU_USAGE_TYPE + CONTAINER_UTIL_USAGE_STATISTICS + USAGE_MAX + i)).
                            __();
                }
                psCPUUsage.__().__();

                if (psGcores > 0) {
                    html.div().$style("margin:20px 2px;").__(" ").__();
                    TBODY<TABLE<Hamlet>> psGPUUsage = html.
                            h2("PS Containers GPU Usage Info:").
                            table("#psGPUUsageInfo").
                            thead("ui-widget-header").
                            tr().
                            th("ui-state-default", "ContainerID").
                            th("ui-state-default", "GPU DEVICE ID").
                            th("ui-state-default", "GPU memory average usages(MB)").
                            th("ui-state-default", "GPU memory max usages(MB)").
                            th("ui-state-default", "GPU utilization average usages(%)").
                            th("ui-state-default", "GPU utilization max usages(%)").
                            __().__().
                            tbody();

                    for (int i = numWorkers; i < numContainers; i++) {
                        String gpustrs = $(CONTAINER_GPU_DEVICE + i);
                        String[] gpusIndex = StringUtils.split(gpustrs, ',');
                        for (int j = 0; j < gpusIndex.length; j++) {
                            psGPUUsage.
                                    __().tbody("ui-widget-content").
                                    tr().
                                    $style("text-align:center;").
                                    td($(CONTAINER_ID + i)).
                                    td(gpusIndex[j]).
                                    td($(GPU_USAGE_TYPE + CONTAINER_MEM_USAGE_STATISTICS + USAGE_AVG + i + gpusIndex[j])).
                                    td($(GPU_USAGE_TYPE + CONTAINER_MEM_USAGE_STATISTICS + USAGE_MAX + i + gpusIndex[j])).
                                    td($(GPU_USAGE_TYPE + CONTAINER_UTIL_USAGE_STATISTICS + USAGE_AVG + i + gpusIndex[j])).
                                    td($(GPU_USAGE_TYPE + CONTAINER_UTIL_USAGE_STATISTICS + USAGE_MAX + i + gpusIndex[j])).
                                    __();
                        }
                    }
                    psGPUUsage.__().__();
                }
            }

            html.div().$style("margin:20px 2px;").__(" ").__();
            int saveModelTotal = Integer.parseInt($(SAVE_MODEL_TOTAL));
            int saveModelSize = Integer.parseInt($(OUTPUT_TOTAL));
            if ((saveModelTotal > 0) && (saveModelSize > 0)) {
                if (!Boolean.valueOf($(SAVE_MODEL))) {
                    html.div().button().$id("saveModel").$onclick("savedModel()").b("Save Model").__().__();
                    StringBuilder script = new StringBuilder();
                    script.append("function savedModel(){")
                            .append("document.getElementById(\"saveModel\").disable=true;")
                            .append("document.location.href='/proxy/").append($(APP_ID))
                            .append("/proxy/savedmodel';")
                            .append("}");
                    html.script().$type("text/javascript").__(script.toString()).__();
                    if (!Boolean.valueOf($(LAST_SAVE_STATUS))) {
                        html.div().$style("margin:20px 2px;").__(" ").__();
                    } else {
                        html.div().$style("margin:20px 2px;").__("saved the model completed!").__();
                    }
                } else {
                    html.div().button().$id("saveModel").$disabled().b("Save Model").__().__();
                    if (!$(SAVE_MODEL_STATUS).equals($(SAVE_MODEL_TOTAL))) {
                        html.div().$style("margin:20px 2px;").__(String.format("saving the model ... %s/%s",
                                $(SAVE_MODEL_STATUS), $(SAVE_MODEL_TOTAL))).__();
                    } else {
                        StringBuilder script = new StringBuilder();
                        script.append("location.href='/proxy/").append($(APP_ID))
                                .append("';");
                        html.script().$type("text/javascript").__(script.toString()).__();
                    }
                }
            } else if (saveModelSize == 0) {
                html.div().button().$id("saveModel").$disabled().b("Save Model").__().__();
                html.div().$style("margin:20px 2px;").__("don't have the output").__();
            } else if (saveModelTotal == 0) {
                html.div().button().$id("saveModel").$disabled().b("Save Model").__().__();
            }

            int modelSaveTotal = Integer.parseInt($(TIMESTAMP_TOTAL));
            if (modelSaveTotal > 0) {
                TBODY<TABLE<Hamlet>> tbodySave = html.
                        h2("").
                        table("#savedmodel").
                        thead("ui-widget-header").
                        tr().
                        th("ui-state-default", "Saved timeStamp").
                        th("ui-state-default", "Saved path").
                        __().__().
                        tbody();

                for (int i = 0; i < modelSaveTotal; i++) {
                    String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(Long.parseLong($(TIMESTAMP_LIST + i))));
                    TD<TR<TBODY<TABLE<Hamlet>>>> td = tbodySave.
                            __().tbody("ui-widget-content").
                            tr().
                            $style("text-align:center;").
                            td(timeStamp).
                            td();

                    String pathStr = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss").format(new Date(Long.parseLong($(TIMESTAMP_LIST + i))));
                    for (int j = 0; j < saveModelSize; j++) {
                        td.p().__($(OUTPUT_PATH + j) + pathStr).__();
                    }
                    td.__().__();
                }
                tbodySave.__().__();
            }

            html.div().$style("margin:20px 2px;").__(" ").__();
            int i = 0;
            if (workerGcores > 0) {
                for (i = 0; i < numWorkers; i++) {
                    if (!$("cpuMemMetrics" + i).equals("") && $("cpuMemMetrics" + i) != null) {
                        html.div().$style("margin:20px 2px;font-weight:bold;font-size:12px").__(String.format($(CONTAINER_ID + i)) + " Metrics:").__();
                        html.script().$src("/proxy/" + $(APP_ID) + "/static/hboxWebApp/jquery-3.1.1.min.js").__();
                        html.script().$src("/proxy/" + $(APP_ID) + "/static/hboxWebApp/highstock.js").__();
                        html.script().$src("/proxy/" + $(APP_ID) + "/static/hboxWebApp/exporting.js").__();

                        String containerGpuMemID = "containerGpuMem" + i;
                        String containerGpuUtilID = "containerGpuUtil" + i;
                        String containerCpuMemID = "containerCpuMem" + i;
                        String containerCpuUtilID = "containercpuUtil" + i;
                        String containerClass = "container" + i;
                        String gpustrs = $(CONTAINER_GPU_DEVICE + i);
                        String[] gpusIndex = StringUtils.split(gpustrs, ',');
                        String[] dataGpuMem = new String[gpusIndex.length];
                        String[] dataGpuUtil = new String[gpusIndex.length];
                        String[] seriesGpuMemOptions = new String[gpusIndex.length];
                        String[] seriesGpuUtilOptions = new String[gpusIndex.length];
                        for (int j = 0; j < gpusIndex.length; j++) {
                            dataGpuMem[j] = $("gpuMemMetrics" + i + gpusIndex[j]);
                            dataGpuUtil[j] = $("gpuUtilMetrics" + i + gpusIndex[j]);
                            gpusIndex[j] = "gpu" + gpusIndex[j];
                            seriesGpuMemOptions[j] = "{\n" +
                                    "            name: '" + gpusIndex[j] + "',\n" +
                                    "            data: " + dataGpuMem[j] + "\n" +
                                    "        }";
                            seriesGpuUtilOptions[j] = "{\n" +
                                    "            name: '" + gpusIndex[j] + "',\n" +
                                    "            data: " + dataGpuUtil[j] + "\n" +
                                    "        }";
                        }
                        String seriesGpuMemOptionsData = StringUtils.join(",", seriesGpuMemOptions);
                        seriesGpuMemOptionsData = "[" + seriesGpuMemOptionsData + "]";
                        String seriesGpuUtilOptionsData = StringUtils.join(",", seriesGpuUtilOptions);
                        seriesGpuUtilOptionsData = "[" + seriesGpuUtilOptionsData + "]";
                        String seriesCpuMemOptions = "[{\n" +
                                "            name: 'cpu mem used',\n" +
                                "            data: " + $("cpuMemMetrics" + i) + "\n" +
                                "        }]";
                        String seriesCpuUtilOptions = "[{\n" +
                                "            name: 'cpu util',\n" +
                                "            data: " + $("cpuUtilMetrics" + i) + "\n" +
                                "        }]";
                        html.div()
                                .div().$id(containerGpuMemID).$class(containerClass).$style("height: 400px; min-width: 310px; diplay:inline-block").__()
                                .div().$id(containerGpuUtilID).$class(containerClass).$style("height: 400px; min-width: 310px; diplay:inline-block").__()
                                .div().$id(containerCpuMemID).$class(containerClass).$style("height: 400px; min-width: 310px; diplay:inline-block").__()
                                .div().$id(containerCpuUtilID).$class(containerClass).$style("height: 400px; min-width: 310px; diplay:inline-block").__()
                                .__();
                        String css = "." + containerClass + "{\n" +
                                "    display:inline-block;\n" +
                                "}";
                        html.style().$type("text/css").__(css).__();
                        String striptHead = "Highcharts.setOptions({\n" +
                                "    global: {\n" +
                                "        useUTC: false\n" +
                                "    }\n" +
                                "});\n" +
                                "// Create the chart\n";
                        String striptBody = "Highcharts.stockChart(" + containerGpuMemID + ", {\n" +
                                "    chart: {\n" +
                                "        width: 600\n" +
                                "    },\n" +
                                "\n" +
                                "    rangeSelector: {\n" +
                                "        buttons: [{\n" +
                                "            count: 1,\n" +
                                "            type: 'minute',\n" +
                                "            text: '1M'\n" +
                                "        }, {\n" +
                                "            count: 5,\n" +
                                "            type: 'minute',\n" +
                                "            text: '5M'\n" +
                                "        }, {\n" +
                                "            type: 'all',\n" +
                                "            text: 'All'\n" +
                                "        }],\n" +
                                "        inputEnabled: false,\n" +
                                "        selected: 0\n" +
                                "    },\n" +
                                "\n" +
                                "    title: {\n" +
                                "        text: 'gpu memory used( MB )'\n" +
                                "    },\n" +
                                "\n" +
                                "    credits: {\n" +
                                "        enabled: false\n" +
                                "    },\n" +
                                "\n" +
                                "    exporting: {\n" +
                                "        enabled: false\n" +
                                "    },\n" +
                                "\n" +
                                "    series: " + seriesGpuMemOptionsData + "\n" +
                                "});\n" +
                                "Highcharts.stockChart(" + containerGpuUtilID + ", {\n" +
                                "    chart: {\n" +
                                "        width: 600\n" +
                                "    },\n" +
                                "\n" +
                                "    rangeSelector: {\n" +
                                "        buttons: [{\n" +
                                "            count: 1,\n" +
                                "            type: 'minute',\n" +
                                "            text: '1M'\n" +
                                "        }, {\n" +
                                "            count: 5,\n" +
                                "            type: 'minute',\n" +
                                "            text: '5M'\n" +
                                "        }, {\n" +
                                "            type: 'all',\n" +
                                "            text: 'All'\n" +
                                "        }],\n" +
                                "        inputEnabled: false,\n" +
                                "        selected: 0\n" +
                                "    },\n" +
                                "\n" +
                                "    title: {\n" +
                                "        text: 'gpu utilization( % )'\n" +
                                "    },\n" +
                                "\n" +
                                "    credits: {\n" +
                                "        enabled: false\n" +
                                "    },\n" +
                                "\n" +
                                "    exporting: {\n" +
                                "        enabled: false\n" +
                                "    },\n" +
                                "\n" +
                                "    series: " + seriesGpuUtilOptionsData + "\n" +
                                "});\n" +
                                "Highcharts.stockChart(" + containerCpuMemID + ", {\n" +
                                "    chart: {\n" +
                                "        width: 600\n" +
                                "    },\n" +
                                "\n" +
                                "    rangeSelector: {\n" +
                                "        buttons: [{\n" +
                                "            count: 1,\n" +
                                "            type: 'minute',\n" +
                                "            text: '1M'\n" +
                                "        }, {\n" +
                                "            count: 5,\n" +
                                "            type: 'minute',\n" +
                                "            text: '5M'\n" +
                                "        }, {\n" +
                                "            type: 'all',\n" +
                                "            text: 'All'\n" +
                                "        }],\n" +
                                "        inputEnabled: false,\n" +
                                "        selected: 0\n" +
                                "    },\n" +
                                "\n" +
                                "    title: {\n" +
                                "        text: 'cpu memory used( GB )'\n" +
                                "    },\n" +
                                "\n" +
                                "    credits: {\n" +
                                "        enabled: false\n" +
                                "    },\n" +
                                "\n" +
                                "    exporting: {\n" +
                                "        enabled: false\n" +
                                "    },\n" +
                                "\n" +
                                "    series: " + seriesCpuMemOptions + "\n" +
                                "});\n" +
                                "Highcharts.stockChart(" + containerCpuUtilID + ", {\n" +
                                "    chart: {\n" +
                                "        width: 600\n" +
                                "    },\n" +
                                "\n" +
                                "    rangeSelector: {\n" +
                                "        buttons: [{\n" +
                                "            count: 1,\n" +
                                "            type: 'minute',\n" +
                                "            text: '1M'\n" +
                                "        }, {\n" +
                                "            count: 5,\n" +
                                "            type: 'minute',\n" +
                                "            text: '5M'\n" +
                                "        }, {\n" +
                                "            type: 'all',\n" +
                                "            text: 'All'\n" +
                                "        }],\n" +
                                "        inputEnabled: false,\n" +
                                "        selected: 0\n" +
                                "    },\n" +
                                "\n" +
                                "    title: {\n" +
                                "        text: 'cpu utilization( % )'\n" +
                                "    },\n" +
                                "\n" +
                                "    credits: {\n" +
                                "        enabled: false\n" +
                                "    },\n" +
                                "\n" +
                                "    exporting: {\n" +
                                "        enabled: false\n" +
                                "    },\n" +
                                "\n" +
                                "    series: " + seriesCpuUtilOptions + "\n" +
                                "});\n";

                        html.script().$type("text/javascript").__(striptHead + striptBody).__();
                    }
                }
            } else {
                for (i = 0; i < numWorkers; i++) {
                    if (!$("cpuMemMetrics" + i).equals("") && $("cpuMemMetrics" + i) != null) {
                        html.div().$style("margin:20px 2px;font-weight:bold;font-size:12px").__(String.format($(CONTAINER_ID + i)) + " metrics:").__();
                        html.script().$src("/proxy/" + $(APP_ID) + "/static/hboxWebApp/jquery-3.1.1.min.js").__();
                        html.script().$src("/proxy/" + $(APP_ID) + "/static/hboxWebApp/highstock.js").__();
                        html.script().$src("/proxy/" + $(APP_ID) + "/static/hboxWebApp/exporting.js").__();

                        String containerCpuMemID = "containerCpuMem" + i;
                        String containerCpuUtilID = "containercpuUtil" + i;
                        String containerClass = "container" + i;
                        String seriesCpuMemOptions = "[{\n" +
                                "            name: 'cpu mem used',\n" +
                                "            data: " + $("cpuMemMetrics" + i) + "\n" +
                                "        }]";
                        String seriesCpuUtilOptions = "[{\n" +
                                "            name: 'cpu util',\n" +
                                "            data: " + $("cpuUtilMetrics" + i) + "\n" +
                                "        }]";
                        html.div()
                                .div().$id(containerCpuMemID).$class(containerClass).$style("height: 400px; min-width: 310px; diplay:inline-block").__()
                                .div().$id(containerCpuUtilID).$class(containerClass).$style("height: 400px; min-width: 310px; diplay:inline-block").__()
                                .__();
                        String css = "." + containerClass + "{\n" +
                                "    display:inline-block;\n" +
                                "}";
                        html.style().$type("text/css").__(css).__();
                        String striptHead = "Highcharts.setOptions({\n" +
                                "    global: {\n" +
                                "        useUTC: false\n" +
                                "    }\n" +
                                "});\n" +
                                "// Create the chart\n";
                        String striptBody = "Highcharts.stockChart(" + containerCpuMemID + ", {\n" +
                                "    chart: {\n" +
                                "        width: 600\n" +
                                "    },\n" +
                                "\n" +
                                "    rangeSelector: {\n" +
                                "        buttons: [{\n" +
                                "            count: 1,\n" +
                                "            type: 'minute',\n" +
                                "            text: '1M'\n" +
                                "        }, {\n" +
                                "            count: 5,\n" +
                                "            type: 'minute',\n" +
                                "            text: '5M'\n" +
                                "        }, {\n" +
                                "            type: 'all',\n" +
                                "            text: 'All'\n" +
                                "        }],\n" +
                                "        inputEnabled: false,\n" +
                                "        selected: 0\n" +
                                "    },\n" +
                                "\n" +
                                "    title: {\n" +
                                "        text: 'cpu memory used( GB )'\n" +
                                "    },\n" +
                                "\n" +
                                "    credits: {\n" +
                                "        enabled: false\n" +
                                "    },\n" +
                                "\n" +
                                "    exporting: {\n" +
                                "        enabled: false\n" +
                                "    },\n" +
                                "\n" +
                                "    series: " + seriesCpuMemOptions + "\n" +
                                "});\n" +
                                "Highcharts.stockChart(" + containerCpuUtilID + ", {\n" +
                                "    chart: {\n" +
                                "        width: 600\n" +
                                "    },\n" +
                                "\n" +
                                "    rangeSelector: {\n" +
                                "        buttons: [{\n" +
                                "            count: 1,\n" +
                                "            type: 'minute',\n" +
                                "            text: '1M'\n" +
                                "        }, {\n" +
                                "            count: 5,\n" +
                                "            type: 'minute',\n" +
                                "            text: '5M'\n" +
                                "        }, {\n" +
                                "            type: 'all',\n" +
                                "            text: 'All'\n" +
                                "        }],\n" +
                                "        inputEnabled: false,\n" +
                                "        selected: 0\n" +
                                "    },\n" +
                                "\n" +
                                "    title: {\n" +
                                "        text: 'cpu utilization( % )'\n" +
                                "    },\n" +
                                "\n" +
                                "    credits: {\n" +
                                "        enabled: false\n" +
                                "    },\n" +
                                "\n" +
                                "    exporting: {\n" +
                                "        enabled: false\n" +
                                "    },\n" +
                                "\n" +
                                "    series: " + seriesCpuUtilOptions + "\n" +
                                "});\n";

                        html.script().$type("text/javascript").__(striptHead + striptBody).__();
                    }
                }
            }

            if (psGcores > 0) {
                for (int c = 0; c < numPS; i++, c++) {
                    if (!$("cpuMemMetrics" + i).equals("") && $("cpuMemMetrics" + i) != null) {
                        html.div().$style("margin:20px 2px;font-weight:bold;font-size:12px").__(String.format($(CONTAINER_ID + i)) + " Metrics:").__();
                        html.script().$src("/proxy/" + $(APP_ID) + "/static/hboxWebApp/jquery-3.1.1.min.js").__();
                        html.script().$src("/proxy/" + $(APP_ID) + "/static/hboxWebApp/highstock.js").__();
                        html.script().$src("/proxy/" + $(APP_ID) + "/static/hboxWebApp/exporting.js").__();

                        String containerGpuMemID = "containerGpuMem" + i;
                        String containerGpuUtilID = "containerGpuUtil" + i;
                        String containerCpuMemID = "containerCpuMem" + i;
                        String containerCpuUtilID = "containercpuUtil" + i;
                        String containerClass = "container" + i;
                        String gpustrs = $(CONTAINER_GPU_DEVICE + i);
                        String[] gpusIndex = StringUtils.split(gpustrs, ',');
                        String[] dataGpuMem = new String[gpusIndex.length];
                        String[] dataGpuUtil = new String[gpusIndex.length];
                        String[] seriesGpuMemOptions = new String[gpusIndex.length];
                        String[] seriesGpuUtilOptions = new String[gpusIndex.length];
                        for (int j = 0; j < gpusIndex.length; j++) {
                            dataGpuMem[j] = $("gpuMemMetrics" + i + gpusIndex[j]);
                            dataGpuUtil[j] = $("gpuUtilMetrics" + i + gpusIndex[j]);
                            gpusIndex[j] = "gpu" + gpusIndex[j];
                            seriesGpuMemOptions[j] = "{\n" +
                                    "            name: '" + gpusIndex[j] + "',\n" +
                                    "            data: " + dataGpuMem[j] + "\n" +
                                    "        }";
                            seriesGpuUtilOptions[j] = "{\n" +
                                    "            name: '" + gpusIndex[j] + "',\n" +
                                    "            data: " + dataGpuUtil[j] + "\n" +
                                    "        }";
                        }
                        String seriesGpuMemOptionsData = StringUtils.join(",", seriesGpuMemOptions);
                        seriesGpuMemOptionsData = "[" + seriesGpuMemOptionsData + "]";
                        String seriesGpuUtilOptionsData = StringUtils.join(",", seriesGpuUtilOptions);
                        seriesGpuUtilOptionsData = "[" + seriesGpuUtilOptionsData + "]";
                        String seriesCpuMemOptions = "[{\n" +
                                "            name: 'cpu mem used',\n" +
                                "            data: " + $("cpuMemMetrics" + i) + "\n" +
                                "        }]";
                        String seriesCpuUtilOptions = "[{\n" +
                                "            name: 'cpu util',\n" +
                                "            data: " + $("cpuUtilMetrics" + i) + "\n" +
                                "        }]";
                        html.div()
                                .div().$id(containerGpuMemID).$class(containerClass).$style("height: 400px; min-width: 310px; diplay:inline-block").__()
                                .div().$id(containerGpuUtilID).$class(containerClass).$style("height: 400px; min-width: 310px; diplay:inline-block").__()
                                .div().$id(containerCpuMemID).$class(containerClass).$style("height: 400px; min-width: 310px; diplay:inline-block").__()
                                .div().$id(containerCpuUtilID).$class(containerClass).$style("height: 400px; min-width: 310px; diplay:inline-block").__()
                                .__();
                        String css = "." + containerClass + "{\n" +
                                "    display:inline-block;\n" +
                                "}";
                        html.style().$type("text/css").__(css).__();
                        String striptHead = "Highcharts.setOptions({\n" +
                                "    global: {\n" +
                                "        useUTC: false\n" +
                                "    }\n" +
                                "});\n" +
                                "// Create the chart\n";
                        String striptBody = "Highcharts.stockChart(" + containerGpuMemID + ", {\n" +
                                "    chart: {\n" +
                                "        width: 600\n" +
                                "    },\n" +
                                "\n" +
                                "    rangeSelector: {\n" +
                                "        buttons: [{\n" +
                                "            count: 1,\n" +
                                "            type: 'minute',\n" +
                                "            text: '1M'\n" +
                                "        }, {\n" +
                                "            count: 5,\n" +
                                "            type: 'minute',\n" +
                                "            text: '5M'\n" +
                                "        }, {\n" +
                                "            type: 'all',\n" +
                                "            text: 'All'\n" +
                                "        }],\n" +
                                "        inputEnabled: false,\n" +
                                "        selected: 0\n" +
                                "    },\n" +
                                "\n" +
                                "    title: {\n" +
                                "        text: 'gpu memory used( MB )'\n" +
                                "    },\n" +
                                "\n" +
                                "    credits: {\n" +
                                "        enabled: false\n" +
                                "    },\n" +
                                "\n" +
                                "    exporting: {\n" +
                                "        enabled: false\n" +
                                "    },\n" +
                                "\n" +
                                "    series: " + seriesGpuMemOptionsData + "\n" +
                                "});\n" +
                                "Highcharts.stockChart(" + containerGpuUtilID + ", {\n" +
                                "    chart: {\n" +
                                "        width: 600\n" +
                                "    },\n" +
                                "\n" +
                                "    rangeSelector: {\n" +
                                "        buttons: [{\n" +
                                "            count: 1,\n" +
                                "            type: 'minute',\n" +
                                "            text: '1M'\n" +
                                "        }, {\n" +
                                "            count: 5,\n" +
                                "            type: 'minute',\n" +
                                "            text: '5M'\n" +
                                "        }, {\n" +
                                "            type: 'all',\n" +
                                "            text: 'All'\n" +
                                "        }],\n" +
                                "        inputEnabled: false,\n" +
                                "        selected: 0\n" +
                                "    },\n" +
                                "\n" +
                                "    title: {\n" +
                                "        text: 'gpu utilization( % )'\n" +
                                "    },\n" +
                                "\n" +
                                "    credits: {\n" +
                                "        enabled: false\n" +
                                "    },\n" +
                                "\n" +
                                "    exporting: {\n" +
                                "        enabled: false\n" +
                                "    },\n" +
                                "\n" +
                                "    series: " + seriesGpuUtilOptionsData + "\n" +
                                "});\n" +
                                "Highcharts.stockChart(" + containerCpuMemID + ", {\n" +
                                "    chart: {\n" +
                                "        width: 600\n" +
                                "    },\n" +
                                "\n" +
                                "    rangeSelector: {\n" +
                                "        buttons: [{\n" +
                                "            count: 1,\n" +
                                "            type: 'minute',\n" +
                                "            text: '1M'\n" +
                                "        }, {\n" +
                                "            count: 5,\n" +
                                "            type: 'minute',\n" +
                                "            text: '5M'\n" +
                                "        }, {\n" +
                                "            type: 'all',\n" +
                                "            text: 'All'\n" +
                                "        }],\n" +
                                "        inputEnabled: false,\n" +
                                "        selected: 0\n" +
                                "    },\n" +
                                "\n" +
                                "    title: {\n" +
                                "        text: 'cpu memory used( GB )'\n" +
                                "    },\n" +
                                "\n" +
                                "    credits: {\n" +
                                "        enabled: false\n" +
                                "    },\n" +
                                "\n" +
                                "    exporting: {\n" +
                                "        enabled: false\n" +
                                "    },\n" +
                                "\n" +
                                "    series: " + seriesCpuMemOptions + "\n" +
                                "});\n" +
                                "Highcharts.stockChart(" + containerCpuUtilID + ", {\n" +
                                "    chart: {\n" +
                                "        width: 600\n" +
                                "    },\n" +
                                "\n" +
                                "    rangeSelector: {\n" +
                                "        buttons: [{\n" +
                                "            count: 1,\n" +
                                "            type: 'minute',\n" +
                                "            text: '1M'\n" +
                                "        }, {\n" +
                                "            count: 5,\n" +
                                "            type: 'minute',\n" +
                                "            text: '5M'\n" +
                                "        }, {\n" +
                                "            type: 'all',\n" +
                                "            text: 'All'\n" +
                                "        }],\n" +
                                "        inputEnabled: false,\n" +
                                "        selected: 0\n" +
                                "    },\n" +
                                "\n" +
                                "    title: {\n" +
                                "        text: 'cpu utilization( % )'\n" +
                                "    },\n" +
                                "\n" +
                                "    credits: {\n" +
                                "        enabled: false\n" +
                                "    },\n" +
                                "\n" +
                                "    exporting: {\n" +
                                "        enabled: false\n" +
                                "    },\n" +
                                "\n" +
                                "    series: " + seriesCpuUtilOptions + "\n" +
                                "});\n";

                        html.script().$type("text/javascript").__(striptHead + striptBody).__();
                    }
                }
            } else {
                for (int c = 0; c < numPS; i++, c++) {
                    if (!$("cpuMemMetrics" + i).equals("") && $("cpuMemMetrics" + i) != null) {
                        html.div().$style("margin:20px 2px;font-weight:bold;font-size:12px").__(String.format($(CONTAINER_ID + i)) + " metrics:").__();
                        html.script().$src("/proxy/" + $(APP_ID) + "/static/hboxWebApp/jquery-3.1.1.min.js").__();
                        html.script().$src("/proxy/" + $(APP_ID) + "/static/hboxWebApp/highstock.js").__();
                        html.script().$src("/proxy/" + $(APP_ID) + "/static/hboxWebApp/exporting.js").__();

                        String containerCpuMemID = "containerCpuMem" + i;
                        String containerCpuUtilID = "containercpuUtil" + i;
                        String containerClass = "container" + i;
                        String seriesCpuMemOptions = "[{\n" +
                                "            name: 'cpu mem used',\n" +
                                "            data: " + $("cpuMemMetrics" + i) + "\n" +
                                "        }]";
                        String seriesCpuUtilOptions = "[{\n" +
                                "            name: 'cpu util',\n" +
                                "            data: " + $("cpuUtilMetrics" + i) + "\n" +
                                "        }]";
                        html.div()
                                .div().$id(containerCpuMemID).$class(containerClass).$style("height: 400px; min-width: 310px; diplay:inline-block").__()
                                .div().$id(containerCpuUtilID).$class(containerClass).$style("height: 400px; min-width: 310px; diplay:inline-block").__()
                                .__();
                        String css = "." + containerClass + "{\n" +
                                "    display:inline-block;\n" +
                                "}";
                        html.style().$type("text/css").__(css).__();
                        String striptHead = "Highcharts.setOptions({\n" +
                                "    global: {\n" +
                                "        useUTC: false\n" +
                                "    }\n" +
                                "});\n" +
                                "// Create the chart\n";
                        String striptBody = "Highcharts.stockChart(" + containerCpuMemID + ", {\n" +
                                "    chart: {\n" +
                                "        width: 600\n" +
                                "    },\n" +
                                "\n" +
                                "    rangeSelector: {\n" +
                                "        buttons: [{\n" +
                                "            count: 1,\n" +
                                "            type: 'minute',\n" +
                                "            text: '1M'\n" +
                                "        }, {\n" +
                                "            count: 5,\n" +
                                "            type: 'minute',\n" +
                                "            text: '5M'\n" +
                                "        }, {\n" +
                                "            type: 'all',\n" +
                                "            text: 'All'\n" +
                                "        }],\n" +
                                "        inputEnabled: false,\n" +
                                "        selected: 0\n" +
                                "    },\n" +
                                "\n" +
                                "    title: {\n" +
                                "        text: 'cpu memory used( GB )'\n" +
                                "    },\n" +
                                "\n" +
                                "    credits: {\n" +
                                "        enabled: false\n" +
                                "    },\n" +
                                "\n" +
                                "    exporting: {\n" +
                                "        enabled: false\n" +
                                "    },\n" +
                                "\n" +
                                "    series: " + seriesCpuMemOptions + "\n" +
                                "});\n" +
                                "Highcharts.stockChart(" + containerCpuUtilID + ", {\n" +
                                "    chart: {\n" +
                                "        width: 600\n" +
                                "    },\n" +
                                "\n" +
                                "    rangeSelector: {\n" +
                                "        buttons: [{\n" +
                                "            count: 1,\n" +
                                "            type: 'minute',\n" +
                                "            text: '1M'\n" +
                                "        }, {\n" +
                                "            count: 5,\n" +
                                "            type: 'minute',\n" +
                                "            text: '5M'\n" +
                                "        }, {\n" +
                                "            type: 'all',\n" +
                                "            text: 'All'\n" +
                                "        }],\n" +
                                "        inputEnabled: false,\n" +
                                "        selected: 0\n" +
                                "    },\n" +
                                "\n" +
                                "    title: {\n" +
                                "        text: 'cpu utilization( % )'\n" +
                                "    },\n" +
                                "\n" +
                                "    credits: {\n" +
                                "        enabled: false\n" +
                                "    },\n" +
                                "\n" +
                                "    exporting: {\n" +
                                "        enabled: false\n" +
                                "    },\n" +
                                "\n" +
                                "    series: " + seriesCpuUtilOptions + "\n" +
                                "});\n";

                        html.script().$type("text/javascript").__(striptHead + striptBody).__();
                    }
                }
            }

        } else {
            html.div().$style("font-size:20px;").__("Waiting for all containers allocated......").__();
        }
    }
}
