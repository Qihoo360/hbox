package net.qihoo.xlearning.jobhistory;

import net.qihoo.xlearning.webapp.AMParams;
import org.apache.hadoop.mapreduce.v2.app.AppContext;
import org.apache.hadoop.yarn.webapp.hamlet.Hamlet;
import org.apache.hadoop.yarn.webapp.hamlet.Hamlet.TABLE;
import org.apache.hadoop.yarn.webapp.view.HtmlBlock;

import com.google.inject.Inject;

import java.text.SimpleDateFormat;
import java.util.Date;

public class HsSingleJobBlock extends HtmlBlock implements AMParams {

  final AppContext appContext;

  @Inject
  HsSingleJobBlock(AppContext appctx) {
    appContext = appctx;
  }

  @Override
  protected void render(Block html) {
    int numContainers = Integer.parseInt($(CONTAINER_NUMBER));
    int numWorkers = Integer.parseInt($(WORKER_NUMBER));
    if (numContainers > 0) {
      Hamlet.TBODY<Hamlet.TABLE<Hamlet>> tbody = html.
          h2("All Containers:").
          table("#Containers").
          thead("ui-widget-header").
          tr().
          th("ui-state-default", "Container ID").
          th("ui-state-default", "Container Host").
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

        if ($(CONTAINER_REPORTER_PROGRESS + i).equals("progress log format error")) {
          td._().
              td(containerMachine.split(":")[0]).
              td($(CONTAINER_STATUS + i)).
              td($(CONTAINER_START_TIME + i)).
              td($(CONTAINER_FINISH_TIME + i)).
              td($(CONTAINER_REPORTER_PROGRESS + i)).td()._()._();
        } else if ($(CONTAINER_REPORTER_PROGRESS + i).equals("0.00%")) {
          td._().
              td(containerMachine.split(":")[0]).
              td($(CONTAINER_STATUS + i)).
              td($(CONTAINER_START_TIME + i)).
              td($(CONTAINER_FINISH_TIME + i)).
              td("N/A").td()._()._();
        } else {
          td._().
              td(containerMachine.split(":")[0]).
              td($(CONTAINER_STATUS + i)).
              td($(CONTAINER_START_TIME + i)).
              td($(CONTAINER_FINISH_TIME + i)).td()
              .div().$class("ui-progressbar ui-widget ui-widget-content ui-corner-all").$title($(CONTAINER_REPORTER_PROGRESS + i))
              .div().$class("ui-progressbar-value ui-widget-header ui-corner-left").$style("width:" + $(CONTAINER_REPORTER_PROGRESS + i))
              ._()._()._()._();
        }
      }
      tbody._()._();

      int timestampSize = Integer.parseInt($(TIMESTAMP_TOTAL));
      int outputSize = Integer.parseInt($(OUTPUT_TOTAL));
      if (timestampSize > 0) {
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
      html.div().$style("margin:20px 2px;")._(" ")._();
      for (int i = 0; i < numWorkers; i++) {
        if (!$("cpuMemMetrics" + i).equals("") && $("cpuMemMetrics" + i) != null) {
          html.div().$style("margin:20px 2px;font-weight:bold;font-size:12px")._(String.format($(CONTAINER_ID + i)) + " metrics:")._();
        }

        html.script().$src("/static/xlWebApp/jquery-3.1.1.min.js")._();
        html.script().$src("/static/xlWebApp/highstock.js")._();
        html.script().$src("/static/xlWebApp/exporting.js")._();

        String containerCpuMemID = "containerCpuMem" + i;
        String containerCpuUtilID = "containerCpuUtil" + i;
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
    } else {
      html.div().$style("font-size:20px;")._("Job History Log getting error !")._();
    }
  }
}
