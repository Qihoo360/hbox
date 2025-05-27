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
package net.qihoo.hbox.webapp;

import static org.apache.hadoop.yarn.util.StringHelper.join;

import net.qihoo.hbox.common.AMParams;
import org.apache.hadoop.yarn.webapp.SubView;
import org.apache.hadoop.yarn.webapp.WebApp;
import org.apache.hadoop.yarn.webapp.WebApps;
import org.apache.hadoop.yarn.webapp.hamlet2.Hamlet.HTML;
import org.apache.hadoop.yarn.webapp.view.TwoColumnLayout;

public class InfoPage extends TwoColumnLayout implements AMParams {
    @Override
    protected void preHead(HTML<__> html) {
        super.preHead(html);
        setTitle(join($(APP_TYPE) + " Application ", $(APP_ID)));
    }

    @Override
    protected Class<? extends SubView> content() {
        if ($(APP_TYPE).equals("Tensorflow")
                || $(APP_TYPE).equals("Mxnet")
                || $(APP_TYPE).equals("Distlightlda")
                || $(APP_TYPE).equals("Xflow")
                || $(APP_TYPE).equals("Xdl")) {
            return InfoBlock.class;
        } else {
            return SingleInfoBlock.class;
        }
    }

    @Override
    protected Class<? extends SubView> nav() {
        return NavBlock.class;
    }

    @Override
    protected Class<? extends SubView> header() {
        try {
            if (WebApps.Builder.class.getMethod("build", WebApp.class) != null) {
                return HeaderBlock.class;
            }
        } catch (NoSuchMethodException e) {
            LOG.debug("current hadoop version don't have the method build of Class " + WebApps.class.toString()
                    + ". For More Detail: " + e);
            return org.apache.hadoop.yarn.webapp.view.HeaderBlock.class;
        }
        return null;
    }
}
