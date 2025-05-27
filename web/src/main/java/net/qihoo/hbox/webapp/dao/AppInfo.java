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
package net.qihoo.hbox.webapp.dao;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import net.qihoo.hbox.api.ApplicationContext;
import net.qihoo.hbox.webapp.App;

/**
 * Created by jiarunying-it on 2018/8/29.
 */
@XmlRootElement(name = "info")
@XmlAccessorType(XmlAccessType.FIELD)
public class AppInfo {

    protected String appId;
    protected String appType;
    protected String appUser;
    protected Integer workerNum;
    protected String workerMem;
    protected Integer workerVCores;
    protected Integer workerGCores;
    protected Integer psNum;
    protected String psMem;
    protected Integer psVCores;
    protected Integer psGCores;

    public AppInfo() {}

    public AppInfo(App app, ApplicationContext context) {
        this.appId = context.getApplicationID().toString();
        this.appType = context.getAppType();
        this.appUser = context.getAppUser();
        this.workerNum = context.getWorkerNum();
        this.psNum = context.getPsNum();
        this.workerMem = String.format("%s GB", context.getWorkerMemory() / 1024.0);
        this.workerVCores = context.getWorkerVCores();
        this.workerGCores = context.getWorkerGcores();
        this.psMem = String.format("%s GB", context.getPsMemory() / 1024.0);
        this.psVCores = context.getPsVCores();
        this.psGCores = context.getPsGcores();
    }
}
