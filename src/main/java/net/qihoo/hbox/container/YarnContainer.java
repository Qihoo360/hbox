package net.qihoo.hbox.container;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import net.qihoo.hbox.util.Utilities;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class YarnContainer implements IContainerLaunch {

    private static final Log LOG = LogFactory.getLog(YarnContainer.class);
    private HboxContainerId containerId;
    private Process hboxProcess;

    public YarnContainer(HboxContainerId containerId) {
        this.containerId = containerId;
    }

    @Override
    public Process exec(String command, String[] envp, Map<String, String> envs, File dir) throws IOException {
        Runtime rt = Runtime.getRuntime();
        hboxProcess = rt.exec(command, envp, dir);
        return hboxProcess;
    }

    @Override
    public boolean isAlive() {
        if (hboxProcess != null) {
            Utilities.isProcessAlive(hboxProcess);
        }
        return false;
    }
}
