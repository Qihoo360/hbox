/**
* Licensed to the Apache Software Foundation (ASF) under one
* or more contributor license agreements.  See the NOTICE file
* distributed with this work for additional information
* regarding copyright ownership.  The ASF licenses this file
* to you under the Apache License, Version 2.0 (the
* "License"); you may not use this file except in compliance
* with the License.  You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package net.qihoo.hbox.jobhistory;

import net.qihoo.hbox.webapp.AMParams;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.mapreduce.v2.app.AppContext;
import org.apache.hadoop.yarn.webapp.hamlet.Hamlet;
import org.apache.hadoop.yarn.webapp.hamlet.Hamlet.TABLE;
import org.apache.hadoop.yarn.webapp.view.HtmlBlock;

import com.google.inject.Inject;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Render a block of HTML for a give job.
 */
public class HsJobBlock extends HtmlBlock implements AMParams {

  final AppContext appContext;
  @Inject HsJobBlock(AppContext appctx) {
    appContext = appctx;
  }

  @Override protected void render(Block html) {
    int numContainers = Integer.parseInt($(CONTAINER_NUMBER));
    String version = $("VERSION");
    if (numContainers > 0) {
      Hamlet.TBODY<Hamlet.TABLE<Hamlet>> tbody = html.
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
          _()._().
          tbody();

      for (int i = 0; i < numContainers; i++) {
        Hamlet.TD<Hamlet.TR<Hamlet.TBODY<TABLE<Hamlet>>>> td = tbody.
            _().tbody("ui-widget-content").
            tr().
            $style("text-align:center;").td();
        td.span().$title(String.format($(CONTAINER_ID + i)))._().
            a($(CONTAINER_LOG_ADDRESS + i),
                String.format($(CONTAINER_ID + i)));
        String containerMachine = $(CONTAINER_HTTP_ADDRESS + i);

        if($(CONTAINER_REPORTER_PROGRESS + i).equals("progress log format error")) {
          td._().
             td(containerMachine.split(":")[0]).
             td($(CONTAINER_GPU_DEVICE + i)).
             td($(CONTAINER_ROLE + i)).
             td($(CONTAINER_STATUS + i)).
             td($(CONTAINER_START_TIME + i)).
             td($(CONTAINER_FINISH_TIME + i)).
             td($(CONTAINER_REPORTER_PROGRESS + i))._();
        } else if($(CONTAINER_REPORTER_PROGRESS + i).equals("0.00%")) {
          td._().
             td(containerMachine.split(":")[0]).
             td($(CONTAINER_GPU_DEVICE + i)).
             td($(CONTAINER_ROLE + i)).
             td($(CONTAINER_STATUS + i)).
             td($(CONTAINER_START_TIME + i)).
             td($(CONTAINER_FINISH_TIME + i)).
             td("N/A")._();
        } else {
          td._().
             td(containerMachine.split(":")[0]).
             td($(CONTAINER_GPU_DEVICE + i)).
             td($(CONTAINER_ROLE + i)).
             td($(CONTAINER_STATUS + i)).
             td($(CONTAINER_START_TIME + i)).
             td($(CONTAINER_FINISH_TIME + i)).td()
             .div().$class("ui-progressbar ui-widget ui-widget-content ui-corner-all").$title($(CONTAINER_REPORTER_PROGRESS + i))
             .div().$class("ui-progressbar-value ui-widget-header ui-corner-left").$style("width:" + $(CONTAINER_REPORTER_PROGRESS + i))
             ._()._()._()._();
        }
      }

      if($(BOARD_INFO_FLAG).equals("true")) {
        tbody._()._().div().$style("margin:40px 2px;")._(" ")._().
                h2("View Board:").
                table("#Board").
                thead("ui-widget-header").
                tr().
                th("ui-state-default", "board Info").
                _()._().
                tbody("ui-widget-content").
                tr().
                $style("text-align:center;").
                td($(BOARD_INFO)).
                _()._()._();
      } else {
        tbody._()._();
      }

      if (Boolean.parseBoolean($("USAGED_INFO"))) {
        int numWorkers = Integer.parseInt($("WORKER_NUM"));
        int workerGcores = Integer.parseInt($("WORKER_GCORES"));
        int numPS = Integer.parseInt($("PS_NUM"));
        int PSGcores = Integer.parseInt($("PS_GCORES"));
        html.div().$style("margin:20px 2px;")._(" ")._();

        // resource applied info
        Hamlet.TBODY<TABLE<Hamlet>> resourceAppliedInfo = html.
            h2("Resource Applied Info:").
            table("#resourceAppliedInfo").
            thead("ui-widget-header").
            tr().
            th("ui-state-default", "Role").
            th("ui-state-default", "Number").
            th("ui-state-default", "CPU Memory(GB)").
            th("ui-state-default", "CPU Cores").
            th("ui-state-default", "GPU Num").
            _()._().
            tbody();
        if(numWorkers > 0){
          resourceAppliedInfo.
              _().tbody("ui-widget-content").
              tr().
              $style("text-align:center;").
              td("worker").
              td(String.valueOf(numWorkers)).
              td($(WORKER_MEMORY)).
              td($(WORKER_VCORES)).
              td(String.valueOf(workerGcores)).
              _();
        }
        if (numPS > 0){
          resourceAppliedInfo.
              _().tbody("ui-widget-content").
              tr().
              $style("text-align:center;").
              td("ps").
              td(String.valueOf(numPS)).
              td($(PS_MEMORY)).
              td($(PS_VCORES)).
              td(String.valueOf(PSGcores)).
              _();
        }
        resourceAppliedInfo._()._();

        html.div().$style("margin:20px 2px;")._(" ")._();
        // worker/ps containers resource usage statistics info
        if (numWorkers > 0) {
          Hamlet.TBODY<TABLE<Hamlet>> workerCPUUsage = html.
              h2("Worker Containers CPU Usage Info:").
              table("#workerCPUUsageInfo").
              thead("ui-widget-header").
              tr().
              th("ui-state-default", "ContainerID").
              th("ui-state-default", "CPU memory average usages(GB)").
              th("ui-state-default", "CPU memory max usages(GB)").
              th("ui-state-default", "CPU utilization average usages(%)").
              th("ui-state-default", "CPU utilization max usages(%)").
              _()._().
              tbody();

          for (int i = 0; i < numWorkers; i++) {
            Hamlet.TD<Hamlet.TR<Hamlet.TBODY<TABLE<Hamlet>>>> td = workerCPUUsage.
                _().tbody("ui-widget-content").
                tr().
                $style("text-align:center;").
                td($("WORKER_CONTAINER_ID" + i)).
                td($("worker" + CPU_USAGE_TYPE + CONTAINER_MEM_USAGE_STATISTICS + USAGE_AVG + i)).td();
            String memWarn = $("worker" + CONTAINER_MEM_USAGE_WARN + i);
            if (memWarn != null && memWarn != "" && Boolean.valueOf(memWarn)) {
              td.$style("color:red").b(String.format("%s\t( Current cpu memory used is much less than applied. Please adjust !! )", $("worker" + CPU_USAGE_TYPE + CONTAINER_MEM_USAGE_STATISTICS + USAGE_MAX + i)));
            } else {
              td._($("worker" + CPU_USAGE_TYPE + CONTAINER_MEM_USAGE_STATISTICS + USAGE_MAX + i));
            }
            td._().td($("worker" + CPU_USAGE_TYPE + CONTAINER_UTIL_USAGE_STATISTICS + USAGE_AVG + i)).
                td($("worker" + CPU_USAGE_TYPE + CONTAINER_UTIL_USAGE_STATISTICS + USAGE_MAX + i)).
                _();
          }
          workerCPUUsage._()._();

          if (workerGcores > 0) {
            html.div().$style("margin:20px 2px;")._(" ")._();
            Hamlet.TBODY<TABLE<Hamlet>> workerGPUUsage = html.
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
                _()._().
                tbody();

            for (int i = 0; i < numWorkers; i++) {
              String gpustrs = $("WORKER_GPU_DEVICE" + i);
              String[] gpusIndex = StringUtils.split(gpustrs, ',');
              for (int j = 0; j < gpusIndex.length; j++) {
                workerGPUUsage.
                    _().tbody("ui-widget-content").
                    tr().
                    $style("text-align:center;").
                    td($("WORKER_CONTAINER_ID" + i)).
                    td(gpusIndex[j]).
                    td($("worker" + GPU_USAGE_TYPE + CONTAINER_MEM_USAGE_STATISTICS + USAGE_AVG + i + gpusIndex[j])).
                    td($("worker" + GPU_USAGE_TYPE + CONTAINER_MEM_USAGE_STATISTICS + USAGE_MAX + i + gpusIndex[j])).
                    td($("worker" + GPU_USAGE_TYPE + CONTAINER_UTIL_USAGE_STATISTICS + USAGE_AVG + i + gpusIndex[j])).
                    td($("worker" + GPU_USAGE_TYPE + CONTAINER_UTIL_USAGE_STATISTICS + USAGE_MAX + i + gpusIndex[j])).
                    _();
              }
            }
            workerGPUUsage._()._();
          }
        }

        if (numPS > 0) {
          html.div().$style("margin:20px 2px;")._(" ")._();
          Hamlet.TBODY<TABLE<Hamlet>> psCPUUsage = html.
              h2("PS Containers CPU Usage Info:").
              table("#psCPUUsageInfo").
              thead("ui-widget-header").
              tr().
              th("ui-state-default", "ContainerID").
              th("ui-state-default", "CPU memory average usages(GB)").
              th("ui-state-default", "CPU memory max usages(GB)").
              th("ui-state-default", "CPU utilization average usages(%)").
              th("ui-state-default", "CPU utilization max usages(%)").
              _()._().
              tbody();

          for (int i = 0; i < numPS; i++) {
            Hamlet.TD<Hamlet.TR<Hamlet.TBODY<TABLE<Hamlet>>>> td = psCPUUsage.
                _().tbody("ui-widget-content").
                tr().
                $style("text-align:center;").
                td($("PS_CONTAINER_ID" + i)).
                td($("ps" + CPU_USAGE_TYPE + CONTAINER_MEM_USAGE_STATISTICS + USAGE_AVG + i)).td();
            String memWarn = $("ps" + CONTAINER_MEM_USAGE_WARN + i);
            if (memWarn != null && memWarn != "" && Boolean.valueOf(memWarn)) {
              td.$style("color:red").b(String.format("%s\t( Current cpu memory used is much less than applied. Please adjust !! )",$("ps" + CPU_USAGE_TYPE + CONTAINER_MEM_USAGE_STATISTICS + USAGE_MAX + i)));
            } else {
              td._($("ps" + CPU_USAGE_TYPE + CONTAINER_MEM_USAGE_STATISTICS + USAGE_MAX + i));
            }
            td._().td($("ps" + CPU_USAGE_TYPE + CONTAINER_UTIL_USAGE_STATISTICS + USAGE_AVG + i)).
                td($("ps" + CPU_USAGE_TYPE + CONTAINER_UTIL_USAGE_STATISTICS + USAGE_MAX + i)).
                _();
          }
          psCPUUsage._()._();

          if (PSGcores > 0) {
            html.div().$style("margin:20px 2px;")._(" ")._();
            Hamlet.TBODY<TABLE<Hamlet>> psGPUUsage = html.
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
                _()._().
                tbody();

            for (int i = 0; i < numPS; i++) {
              String gpustrs = $("PS_GPU_DEVICE" + i);
              String[] gpusIndex = StringUtils.split(gpustrs, ',');
              for (int j = 0; j < gpusIndex.length; j++) {
                psGPUUsage.
                    _().tbody("ui-widget-content").
                    tr().
                    $style("text-align:center;").
                    td($("PS_CONTAINER_ID" + i)).
                    td(gpusIndex[j]).
                    td($("ps" + GPU_USAGE_TYPE + CONTAINER_MEM_USAGE_STATISTICS + USAGE_AVG + i + gpusIndex[j])).
                    td($("ps" + GPU_USAGE_TYPE + CONTAINER_MEM_USAGE_STATISTICS + USAGE_MAX + i + gpusIndex[j])).
                    td($("ps" + GPU_USAGE_TYPE + CONTAINER_UTIL_USAGE_STATISTICS + USAGE_AVG + i + gpusIndex[j])).
                    td($("ps" + GPU_USAGE_TYPE + CONTAINER_UTIL_USAGE_STATISTICS + USAGE_MAX + i + gpusIndex[j])).
                    _();
              }
            }
            psGPUUsage._()._();
          }
        }
      }

      html.div().$style("margin:20px 2px;")._(" ")._();

      int timestampSize = Integer.parseInt($(TIMESTAMP_TOTAL));
      int outputSize = Integer.parseInt($(OUTPUT_TOTAL));
      if(timestampSize > 0){
        html.div().$style("margin:20px 2px;")._(" ")._();
        Hamlet.TBODY<TABLE<Hamlet>> tbodySave = html.
            h2("Saved Model").
            table("#savedmodel").
            thead("ui-widget-header").
            tr().
            th("ui-state-default", "Saved timeStamp").
            th("ui-state-default", "Saved path").
            _()._().
            tbody();

        for (int i = 0; i < timestampSize; i++) {
          String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(Long.parseLong($(TIMESTAMP_LIST + i))));
          Hamlet.TD<Hamlet.TR<Hamlet.TBODY<TABLE<Hamlet>>>> td = tbodySave.
              _().tbody("ui-widget-content").
              tr().
              $style("text-align:center;").
              td(timeStamp).
              td();

          String pathStr = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss").format(new Date(Long.parseLong($(TIMESTAMP_LIST + i))));
          for (int j = 0; j < outputSize; j++) {
            td.p()._($(OUTPUT_PATH + j) + pathStr)._();
          }
          td._()._();
        }
        tbodySave._()._();
      }

      if(version !=null && !version.equals("")) {
        int numWorkers = Integer.parseInt($("WORKER_NUM"));
        int workerGcores = Integer.parseInt($("WORKER_GCORES"));
        int numPS = Integer.parseInt($("PS_NUM"));
        int PSGcores = Integer.parseInt($("PS_GCORES"));
        html.div().$style("margin:20px 2px;")._(" ")._();
        if(PSGcores > 0) {
          for(int i=0; i<numPS; i++) {
            if(!$("pscpuMemMetrics" + i).equals("") && $("pscpuMemMetrics" + i) != null) {
              html.div().$style("margin:20px 2px;font-weight:bold;font-size:12px")._(String.format($("PS_CONTAINER_ID" + i)) + " metrics:")._();
            }
            html.script().$src("/appResource/jquery-3.1.1.min.js")._();
            html.script().$src("/appResource/highstock.js")._();
            html.script().$src("/appResource/exporting.js")._();

            String containerGpuMemID = "pscontainerGpuMem" + i;
            String containerGpuUtilID = "pscontainerGpuUtil" + i;
            String containerCpuMemID = "pscontainerCpuMem" + i;
            String containerCpuUtilID = "pscontainercpuUtil" + i;
            String containerClass = "pscontainer" + i;
            String gpustrs = $("PS_GPU_DEVICE" + i);
            String[] gpusIndex = StringUtils.split(gpustrs, ',');
            String[] dataGpuMem = new String[gpusIndex.length];
            String[] dataGpuUtil = new String[gpusIndex.length];
            String[] seriesGpuMemOptions = new String[gpusIndex.length];
            String[] seriesGpuUtilOptions = new String[gpusIndex.length];
            for(int j=0; j<gpusIndex.length; j++) {
              dataGpuMem[j] = $("psgpuMemMetrics" + i + gpusIndex[j]);
              dataGpuUtil[j] = $("psgpuUtilMetrics" + i + gpusIndex[j]);
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
            String seriesGpuMemOptionsData = StringUtils.join(seriesGpuMemOptions, ",");
            seriesGpuMemOptionsData = "[" + seriesGpuMemOptionsData + "]";
            String seriesGpuUtilOptionsData = StringUtils.join(seriesGpuUtilOptions, ",");
            seriesGpuUtilOptionsData = "[" + seriesGpuUtilOptionsData + "]";
            String seriesCpuMemOptions = "[{\n" +
                "            name: 'cpu mem used',\n" +
                "            data: " + $("pscpuMemMetrics" + i) + "\n" +
                "        }]";
            String seriesCpuUtilOptions = "[{\n" +
                "            name: 'cpu util',\n" +
                "            data: " + $("pscpuUtilMetrics" + i) + "\n" +
                "        }]";
            html.div()
                .div().$id(containerGpuMemID).$class(containerClass).$style("height: 400px; min-width: 310px; diplay:inline-block")._()
                .div().$id(containerGpuUtilID).$class(containerClass).$style("height: 400px; min-width: 310px; diplay:inline-block")._()
                .div().$id(containerCpuMemID).$class(containerClass).$style("height: 400px; min-width: 310px; diplay:inline-block")._()
                .div().$id(containerCpuUtilID).$class(containerClass).$style("height: 400px; min-width: 310px; diplay:inline-block")._()
                ._();
            String css = "." + containerClass + "{\n" +
                "    display:inline-block;\n" +
                "}";
            html.style().$type("text/css")._(css)._();
            String striptHead = "Highcharts.setOptions({\n" +
                "    global: {\n" +
                "        useUTC: false\n" +
                "    }\n" +
                "});\n" +
                "// Create the chart\n";
            String striptBody = "Highcharts.stockChart(" + containerGpuMemID + ", {\n" +
                "    chart: {\n" +
                "        width: 550\n" +
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
                "        width: 550\n" +
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
                "        width: 550\n" +
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
                "        width: 550\n" +
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

            html.script().$type("text/javascript")._(striptHead + striptBody)._();
          }
        } else {
          for(int i=0; i<numPS; i++) {
            if(!$("pscpuMemMetrics" + i).equals("") && $("pscpuMemMetrics" + i) != null) {
              html.div().$style("margin:20px 2px;font-weight:bold;font-size:12px")._(String.format($("PS_CONTAINER_ID" + i)) + " metrics:")._();
            }

            html.script().$src("/appResource/jquery-3.1.1.min.js")._();
            html.script().$src("/appResource/highstock.js")._();
            html.script().$src("/appResource/exporting.js")._();

            String containerCpuMemID = "pscontainerCpuMem" + i;
            String containerCpuUtilID = "pscontainercpuUtil" + i;
            String containerClass = "pscontainer" + i;
            String seriesCpuMemOptions = "[{\n" +
                "            name: 'cpu mem used',\n" +
                "            data: " + $("pscpuMemMetrics" + i) + "\n" +
                "        }]";
            String seriesCpuUtilOptions = "[{\n" +
                "            name: 'cpu util',\n" +
                "            data: " + $("pscpuUtilMetrics" + i) + "\n" +
                "        }]";
            html.div()
                .div().$id(containerCpuMemID).$class(containerClass).$style("height: 400px; min-width: 310px; diplay:inline-block")._()
                .div().$id(containerCpuUtilID).$class(containerClass).$style("height: 400px; min-width: 310px; diplay:inline-block")._()
                ._();
            String css = "." + containerClass + "{\n" +
                "    display:inline-block;\n" +
                "}";
            html.style().$type("text/css")._(css)._();
            String striptHead = "Highcharts.setOptions({\n" +
                "    global: {\n" +
                "        useUTC: false\n" +
                "    }\n" +
                "});\n" +
                "// Create the chart\n";
            String striptBody = "Highcharts.stockChart(" + containerCpuMemID + ", {\n" +
                "    chart: {\n" +
                "        width: 550\n" +
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
                "        width: 550\n" +
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

            html.script().$type("text/javascript")._(striptHead + striptBody)._();
          }
        }

        if(workerGcores > 0) {
          for(int i=0; i<numWorkers; i++) {
            if(!$("workercpuMemMetrics" + i).equals("") && $("workercpuMemMetrics" + i) != null) {
              html.div().$style("margin:20px 2px;font-weight:bold;font-size:12px")._(String.format($("WORKER_CONTAINER_ID" + i)) + " metrics:")._();
            }
              html.script().$src("/appResource/jquery-3.1.1.min.js")._();
              html.script().$src("/appResource/highstock.js")._();
              html.script().$src("/appResource/exporting.js")._();

            String containerGpuMemID = "workercontainerGpuMem" + i;
            String containerGpuUtilID = "workercontainerGpuUtil" + i;
            String containerCpuMemID = "workercontainerCpuMem" + i;
            String containerCpuUtilID = "workercontainercpuUtil" + i;
            String containerClass = "workercontainer" + i;
            String gpustrs = $("WORKER_GPU_DEVICE" + i);
            String[] gpusIndex = StringUtils.split(gpustrs, ',');
            String[] dataGpuMem = new String[gpusIndex.length];
            String[] dataGpuUtil = new String[gpusIndex.length];
            String[] seriesGpuMemOptions = new String[gpusIndex.length];
            String[] seriesGpuUtilOptions = new String[gpusIndex.length];
            for(int j=0; j<gpusIndex.length; j++) {
              dataGpuMem[j] = $("workergpuMemMetrics" + i + gpusIndex[j]);
              dataGpuUtil[j] = $("workergpuUtilMetrics" + i + gpusIndex[j]);
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
            String seriesGpuMemOptionsData = StringUtils.join(seriesGpuMemOptions, ",");
            seriesGpuMemOptionsData = "[" + seriesGpuMemOptionsData + "]";
            String seriesGpuUtilOptionsData = StringUtils.join(seriesGpuUtilOptions, ",");
            seriesGpuUtilOptionsData = "[" + seriesGpuUtilOptionsData + "]";
            String seriesCpuMemOptions = "[{\n" +
                    "            name: 'cpu mem used',\n" +
                    "            data: " + $("workercpuMemMetrics" + i) + "\n" +
                    "        }]";
            String seriesCpuUtilOptions = "[{\n" +
                    "            name: 'cpu util',\n" +
                    "            data: " + $("workercpuUtilMetrics" + i) + "\n" +
                    "        }]";
            html.div()
                    .div().$id(containerGpuMemID).$class(containerClass).$style("height: 400px; min-width: 310px; diplay:inline-block")._()
                    .div().$id(containerGpuUtilID).$class(containerClass).$style("height: 400px; min-width: 310px; diplay:inline-block")._()
                    .div().$id(containerCpuMemID).$class(containerClass).$style("height: 400px; min-width: 310px; diplay:inline-block")._()
                    .div().$id(containerCpuUtilID).$class(containerClass).$style("height: 400px; min-width: 310px; diplay:inline-block")._()
                    ._();
            String css = "." + containerClass + "{\n" +
                    "    display:inline-block;\n" +
                    "}";
            html.style().$type("text/css")._(css)._();
            String striptHead = "Highcharts.setOptions({\n" +
                    "    global: {\n" +
                    "        useUTC: false\n" +
                    "    }\n" +
                    "});\n" +
                    "// Create the chart\n";
            String striptBody = "Highcharts.stockChart(" + containerGpuMemID + ", {\n" +
                    "    chart: {\n" +
                    "        width: 550\n" +
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
                    "        width: 550\n" +
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
                    "        width: 550\n" +
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
                    "        width: 550\n" +
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

            html.script().$type("text/javascript")._(striptHead + striptBody)._();
          }
        } else {
          for(int i=0; i<numWorkers; i++) {
            if(!$("workercpuMemMetrics" + i).equals("") && $("workercpuMemMetrics" + i) != null) {
              html.div().$style("margin:20px 2px;font-weight:bold;font-size:12px")._(String.format($("WORKER_CONTAINER_ID" + i)) + " metrics:")._();
            }

          html.script().$src("/appResource/jquery-3.1.1.min.js")._();
          html.script().$src("/appResource/highstock.js")._();
          html.script().$src("/appResource/exporting.js")._();

            String containerCpuMemID = "workercontainerCpuMem" + i;
            String containerCpuUtilID = "workercontainercpuUtil" + i;
            String containerClass = "workercontainer" + i;
            String seriesCpuMemOptions = "[{\n" +
                    "            name: 'cpu mem used',\n" +
                    "            data: " + $("workercpuMemMetrics" + i) + "\n" +
                    "        }]";
            String seriesCpuUtilOptions = "[{\n" +
                    "            name: 'cpu util',\n" +
                    "            data: " + $("workercpuUtilMetrics" + i) + "\n" +
                    "        }]";
            html.div()
                    .div().$id(containerCpuMemID).$class(containerClass).$style("height: 400px; min-width: 310px; diplay:inline-block")._()
                    .div().$id(containerCpuUtilID).$class(containerClass).$style("height: 400px; min-width: 310px; diplay:inline-block")._()
                    ._();
            String css = "." + containerClass + "{\n" +
                    "    display:inline-block;\n" +
                    "}";
            html.style().$type("text/css")._(css)._();
            String striptHead = "Highcharts.setOptions({\n" +
                    "    global: {\n" +
                    "        useUTC: false\n" +
                    "    }\n" +
                    "});\n" +
                    "// Create the chart\n";
            String striptBody = "Highcharts.stockChart(" + containerCpuMemID + ", {\n" +
                    "    chart: {\n" +
                    "        width: 550\n" +
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
                    "        width: 550\n" +
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

            html.script().$type("text/javascript")._(striptHead + striptBody)._();
          }
        }
      }


    } else {
      html.div().$style("font-size:20px;")._("Not get the container information, please check all containers started successfully !")._();
    }
  }
}
