package net.qihoo.xlearning.webapp;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.yarn.webapp.hamlet.Hamlet;
import org.apache.hadoop.yarn.webapp.hamlet.Hamlet.TABLE;
import org.apache.hadoop.yarn.webapp.hamlet.Hamlet.TBODY;
import org.apache.hadoop.yarn.webapp.hamlet.Hamlet.TR;
import org.apache.hadoop.yarn.webapp.hamlet.Hamlet.TD;
import org.apache.hadoop.yarn.webapp.view.HtmlBlock;

import java.text.SimpleDateFormat;
import java.util.Date;

public class InfoBlock extends HtmlBlock implements AMParams {
  private static final Log LOG = LogFactory.getLog(InfoBlock.class);

  @Override
  protected void render(Block html) {
    int numContainers = Integer.parseInt($(CONTAINER_NUMBER));
    if (numContainers > 0) {
      TBODY<TABLE<Hamlet>> tbody = html.
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
        TD<TR<TBODY<TABLE<Hamlet>>>> td = tbody.
            _().tbody("ui-widget-content").
            tr().
            $style("text-align:center;").td();
        td.span().$title(String.format($(CONTAINER_ID + i)))._().
            a(String.format("http://%s/node/containerlogs/%s/%s",
                $(CONTAINER_HTTP_ADDRESS + i),
                $(CONTAINER_ID + i),
                $(USER_NAME)),
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

      if (!$(BOARD_INFO).equals("no")) {
        if (!$(BOARD_INFO).contains("http")) {
          tbody._()._().div().$style("margin:20px 2px;")._(" ")._().
              h2("View TensorBoard:").
              table("#TensorBoard").
              thead("ui-widget-header").
              tr().
              th("ui-state-default", "Tensorboard Info").
              _()._().
              tbody("ui-widget-content").
              tr().
              $style("text-align:center;").
              td(String.format($(BOARD_INFO))).
              _()._()._();
        } else {
          tbody._()._().div().$style("margin:20px 2px;")._(" ")._().
              h2("View TensorBoard:").
              table("#TensorBoard").
              thead("ui-widget-header").
              tr().
              th("ui-state-default", "Tensorboard Info").
              _()._().
              tbody("ui-widget-content").
              tr().
              $style("text-align:center;").
              td().span().$title(String.format($(BOARD_INFO)))._().
              a(String.format($(BOARD_INFO)),
                  String.format($(BOARD_INFO))).
              _()._()._()._();
        }
      } else {
        tbody._()._();
      }

      html.div().$style("margin:20px 2px;")._(" ")._();
      int saveModelTotal = Integer.parseInt($(SAVE_MODEL_TOTAL));
      int saveModelSize = Integer.parseInt($(OUTPUT_TOTAL));
      if ((saveModelTotal > 0) && (saveModelSize > 0)) {
        if (!Boolean.valueOf($(SAVE_MODEL))) {
          html.div().button().$id("saveModel").$onclick("savedModel()").b("Save Model")._()._();
          StringBuilder script = new StringBuilder();
          script.append("function savedModel(){")
              .append("document.getElementById(\"saveModel\").disable=true;")
              .append("document.location.href='/proxy/").append($(APP_ID))
              .append("/proxy/savedmodel';")
              .append("}");
          html.script().$type("text/javascript")._(script.toString())._();
          if (!Boolean.valueOf($(LAST_SAVE_STATUS))) {
            html.div().$style("margin:20px 2px;")._(" ")._();
          } else {
            html.div().$style("margin:20px 2px;")._("saved the model completed!")._();
          }
        } else {
          html.div().button().$id("saveModel").$disabled().b("Save Model")._()._();
          if (!$(SAVE_MODEL_STATUS).equals($(SAVE_MODEL_TOTAL))) {
            html.div().$style("margin:20px 2px;")._(String.format("saving the model ... %s/%s",
                $(SAVE_MODEL_STATUS), $(SAVE_MODEL_TOTAL)))._();
          } else {
            StringBuilder script = new StringBuilder();
            script.append("location.href='/proxy/").append($(APP_ID))
                .append("';");
            html.script().$type("text/javascript")._(script.toString())._();
          }
        }
      } else if (saveModelSize == 0) {
        html.div().button().$id("saveModel").$disabled().b("Save Model")._()._();
        html.div().$style("margin:20px 2px;")._("don't have the local output dir")._();
      } else if (saveModelTotal == 0) {
        html.div().button().$id("saveModel").$disabled().b("Save Model")._()._();
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
            _()._().
            tbody();

        for (int i = 0; i < modelSaveTotal; i++) {
          String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(Long.parseLong($(TIMESTAMP_LIST + i))));
          TD<TR<TBODY<TABLE<Hamlet>>>> td = tbodySave.
              _().tbody("ui-widget-content").
              tr().
              $style("text-align:center;").
              td(timeStamp).
              td();

          String pathStr = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss").format(new Date(Long.parseLong($(TIMESTAMP_LIST + i))));
          for (int j = 0; j < saveModelSize; j++) {
            td.p()._($(OUTPUT_PATH + j) + pathStr)._();
          }
          td._()._();
        }
        tbodySave._()._();
      }
    } else {
      html.div().$style("font-size:20px;")._("Waiting for all containers allocated......")._();
    }
  }
}