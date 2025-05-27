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

import static org.apache.hadoop.yarn.util.StringHelper.pajoin;
import static org.apache.hadoop.yarn.webapp.YarnWebParams.*;

import net.qihoo.hbox.common.AMParams;
import org.apache.hadoop.mapreduce.v2.app.AppContext;
import org.apache.hadoop.mapreduce.v2.hs.HistoryContext;
import org.apache.hadoop.mapreduce.v2.hs.webapp.HsWebServices;
import org.apache.hadoop.mapreduce.v2.hs.webapp.JAXBContextResolver;
import org.apache.hadoop.yarn.webapp.GenericExceptionHandler;
import org.apache.hadoop.yarn.webapp.WebApp;

public class HsWebApp extends WebApp implements AMParams {

    private HistoryContext history;

    public HsWebApp(HistoryContext history) {
        this.history = history;
    }

    @Override
    public void setup() {
        bind(HsWebServices.class);
        bind(JAXBContextResolver.class);
        bind(GenericExceptionHandler.class);
        bind(AppContext.class).toInstance(history);
        bind(HistoryContext.class).toInstance(history);
        route("/", HsController.class);
        route(pajoin("/job", APP_ID), HsController.class, "job");
        route(
                pajoin("/logs", NM_NODENAME, CONTAINER_ID, ENTITY_STRING, APP_OWNER, CONTAINER_LOG_TYPE),
                HsController.class,
                "logs");
    }
}
