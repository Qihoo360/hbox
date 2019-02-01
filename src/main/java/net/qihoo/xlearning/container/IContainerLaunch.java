package net.qihoo.xlearning.container;

import java.io.IOException;
import java.util.Map;

public interface IContainerLaunch {

  Process exec(String command, String[] envp, Map<String, String> envs) throws IOException;

  boolean isAlive();
}
