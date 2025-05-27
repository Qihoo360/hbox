// Copyright 2017-2025 Qihoo Inc
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
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
    /*
     * (non-Javadoc)
     * @see org.apache.hadoop.mapreduce.v2.hs.webapp.HsView#preHead(org.apache.hadoop.yarn.webapp.hamlet.Hamlet.HTML)
     */
    @Override
    protected void preHead(Page.HTML<__> html) {
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
