package net.qihoo.hbox.jobhistory;

import static org.apache.hadoop.yarn.webapp.YarnWebParams.CONTAINER_ID;
import static org.apache.hadoop.yarn.webapp.YarnWebParams.ENTITY_STRING;
import static org.apache.hadoop.yarn.webapp.view.JQueryUI.ACCORDION;
import static org.apache.hadoop.yarn.webapp.view.JQueryUI.ACCORDION_ID;
import static org.apache.hadoop.yarn.webapp.view.JQueryUI.initID;

import org.apache.hadoop.yarn.webapp.SubView;
import org.apache.hadoop.yarn.webapp.log.AggregatedLogsBlock;
import org.apache.hadoop.yarn.webapp.view.TwoColumnLayout;

public class HsLogsPage extends TwoColumnLayout {

    @Override
    protected void preHead(Page.HTML<_> html) {
        String logEntity = $(ENTITY_STRING);
        if (logEntity == null || logEntity.isEmpty()) {
            logEntity = $(CONTAINER_ID);
        }
        if (logEntity == null || logEntity.isEmpty()) {
            logEntity = "UNKNOWN";
        }
        set(ACCORDION_ID, "nav");
        set(initID(ACCORDION, "nav"), "{autoHeight:false, active:0}");
    }

    /**
     * The content of this page is the JobBlock
     *
     * @return HsJobBlock.class
     */
    @Override
    protected Class<? extends SubView> content() {
        return AggregatedLogsBlock.class;
    }
}
