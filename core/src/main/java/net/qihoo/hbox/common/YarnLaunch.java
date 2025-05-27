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
package net.qihoo.hbox.common;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class YarnLaunch implements ILaunch {

    private static final Log LOG = LogFactory.getLog(YarnLaunch.class);
    private String containerId;
    private Process hboxProcess;

    public YarnLaunch(String containerId) {
        this.containerId = containerId;
    }

    @Override
    public Process exec(String command, String[] envp, Map<String, String> envs, File dir) throws IOException {
        Runtime rt = Runtime.getRuntime();
        hboxProcess = rt.exec(command, envp, dir);
        return hboxProcess;
    }

    @Override
    public Process exec(String[] commandArgs, String[] envp, Map<String, String> envs, File dir) throws IOException {
        final Runtime rt = Runtime.getRuntime();
        hboxProcess = rt.exec(commandArgs, envp, dir);
        return hboxProcess;
    }
}
