package net.qihoo.xlearning.container;

import java.io.IOException;
import java.util.Map;

import net.qihoo.xlearning.util.Utilities;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class YarnContainer implements IContainerLaunch {

  private static final Log LOG = LogFactory.getLog(YarnContainer.class);
  private XLearningContainerId containerId;
  private Process xlearningProcess;

  public YarnContainer(XLearningContainerId containerId) {
    this.containerId = containerId;
  }

  @Override
  public Process exec(String command, String[] envp, Map<String, String> envs) throws IOException {
    Runtime rt = Runtime.getRuntime();
    xlearningProcess = rt.exec(command, envp);
    return xlearningProcess;
  }

  @Override
  public boolean isAlive() {
    if (xlearningProcess != null) {
      Utilities.isProcessAlive(xlearningProcess);
    }
    return false;
  }
}
