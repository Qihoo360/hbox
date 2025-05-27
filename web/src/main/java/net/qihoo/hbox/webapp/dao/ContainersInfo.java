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

import java.util.ArrayList;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Created by jiarunying-it on 2018/8/30.
 */
@XmlRootElement(name = "containerInfo")
@XmlAccessorType(XmlAccessType.FIELD)
public class ContainersInfo {
    protected ArrayList<ContainerInfo> containerInfos = new ArrayList<>();

    public ContainersInfo() {}

    public void add(ContainerInfo containerInfo) {
        containerInfos.add(containerInfo);
    }
}
