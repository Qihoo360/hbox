package net.qihoo.xlearning.jobhistory;

import net.qihoo.xlearning.webapp.AMParams;
import org.apache.hadoop.mapreduce.v2.app.AppContext;
import org.apache.hadoop.yarn.webapp.hamlet.Hamlet;
import org.apache.hadoop.yarn.webapp.hamlet.Hamlet.TABLE;
import org.apache.hadoop.yarn.webapp.view.HtmlBlock;

import com.google.inject.Inject;

import java.text.SimpleDateFormat;
import java.util.Date;

public class HsJobBlock extends HtmlBlock implements AMParams {

  final AppContext appContext;

  @Inject
  HsJobBlock(AppContext appctx) {
    appContext = appctx;
  }

  @Override
  protected void render(Block html) {
    int numContainers = Integer.parseInt($(CONTAINER_NUMBER));
    if (numContainers > 0) {
      Hamlet.TBODY<Hamlet.TABLE<Hamlet>> tbody = html.
          h2("All Containers:").
          table("#Containers").
          thead("ui-widget-header").
          tr().
          th("ui-state-default", "Container ID").
          th("ui-state-default", "Container Host").
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

        if ($(CONTAINER_REPORTER_PROGRESS + i).equals("progress log format error")) {
          td._().
              td(containerMachine.split(":")[0]).
              td($(CONTAINER_ROLE + i)).
              td($(CONTAINER_STATUS + i)).
              td($(CONTAINER_START_TIME + i)).
              td($(CONTAINER_FINISH_TIME + i)).
              td($(CONTAINER_REPORTER_PROGRESS + i)).td()._()._();
        } else if ($(CONTAINER_REPORTER_PROGRESS + i).equals("0.00%")) {
          td._().
              td(containerMachine.split(":")[0]).
              td($(CONTAINER_ROLE + i)).
              td($(CONTAINER_STATUS + i)).
              td($(CONTAINER_START_TIME + i)).
              td($(CONTAINER_FINISH_TIME + i)).
              td("N/A").td()._()._();
        } else {
          td._().
              td(containerMachine.split(":")[0]).
              td($(CONTAINER_ROLE + i)).
              td($(CONTAINER_STATUS + i)).
              td($(CONTAINER_START_TIME + i)).
              td($(CONTAINER_FINISH_TIME + i)).td()
              .div().$class("ui-progressbar ui-widget ui-widget-content ui-corner-all").$title($(CONTAINER_REPORTER_PROGRESS + i))
              .div().$class("ui-progressbar-value ui-widget-header ui-corner-left").$style("width:" + $(CONTAINER_REPORTER_PROGRESS + i))
              ._()._()._()._();
        }
      }

      if ($(BOARD_INFO_FLAG).equals("true")) {
        tbody._()._().div().$style("margin:40px 2px;")._(" ")._().
            h2("View TensorBoard:").
            table("#TensorBoard").
            thead("ui-widget-header").
            tr().
            th("ui-state-default", "Tensorboard Info").
            _()._().
            tbody("ui-widget-content").
            tr().
            $style("text-align:center;").
            td($(BOARD_INFO)).
            _()._()._();
      } else {
        tbody._()._();
      }


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
    } else {
      html.div().$style("font-size:20px;")._("Job History Log getting error !")._();
    }
  }
}
