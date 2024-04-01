package net.qihoo.hbox.common;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import net.qihoo.hbox.container.HboxContainerId;
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
}
