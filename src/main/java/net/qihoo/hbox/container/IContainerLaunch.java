package net.qihoo.hbox.container;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public interface IContainerLaunch {

    Process exec(String command, String[] envp, Map<String, String> envs, File dir) throws IOException;

    boolean isAlive();
}
