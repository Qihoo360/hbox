package net.qihoo.xlearning.container;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;
import net.qihoo.xlearning.conf.XLearningConfiguration;
import net.qihoo.xlearning.util.Utilities;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class DockerContainer implements IContainerLaunch {

  private static final Log LOG = LogFactory.getLog(DockerContainer.class);
  private XLearningContainerId containerId;
  private Process xlearningProcess;
  private XLearningConfiguration conf;
  private String runArgs;

  public DockerContainer(XLearningContainerId containerId, XLearningConfiguration conf) {
    this.containerId = containerId;
    this.conf = conf;
    this.runArgs = conf.get(XLearningConfiguration.XLEARNING_DOCKER_RUN_ARGS,
        "");
  }

  @Override
  public boolean isAlive() {
    if (xlearningProcess != null && Utilities.isProcessAlive(xlearningProcess)) {
      return true;
    } else if (Utilities.isDockerAlive(containerId.toString())) {
      return true;
    }
    return false;
  }

  @Override
  public Process exec(String command, String[] envp, Map<String, String> envs, File dir) throws IOException {
    LOG.info("docker command:" + command + ",envs:" + envs);
    Runtime rt = Runtime.getRuntime();
    String port = conf.get("RESERVED_PORT");
    String workDir = "/" + conf.get(XLearningConfiguration.XLEARNING_DOCKER_WORK_DIR, XLearningConfiguration.DEFAULT_XLEARNING_DOCKER_WORK_DIR);
    String path = new File("").getAbsolutePath();
    StringBuilder envsParam = new StringBuilder();
    for (String keyValue : envp) {
      if (keyValue.startsWith("PATH") || keyValue.startsWith("CLASSPATH")) {
        continue;
      } else {
        envsParam.append(" --env " + keyValue + "");
      }
    }
    if (port.equals("-1")) {
      port = "";
    } else {
      port = " -p " + port;
    }
    String containerMemory = envs.get("DOCKER_CONTAINER_MEMORY");
    String containerCpu = envs.get("DOCKER_CONTAINER_CPU");
    String userName = conf.get("hadoop.job.ugi");
    String[] userNameArr = userName.split(",");
    if (userNameArr.length > 1) {
      userName = userNameArr[0];
    }
    LOG.info("Container launch userName:" + userName);
    String homePath = envs.get("HADOOP_HDFS_HOME");
    String mount = " -v " + path + ":" + workDir;
    mount += " -v " + homePath + ":" + homePath;
    String javaPath = envs.get("JAVA_HOME");
    mount += " -v " + javaPath + ":" + javaPath;
    String[] localDirs = envs.get("LOCAL_DIRS").split(File.pathSeparator);
    if (localDirs.length > 0) {
      for (String perPath : localDirs) {
        mount = mount + " -v " + perPath + ":" + perPath;
      }
    }
    String[] logsDirs = envs.get("LOG_DIRS").split(File.pathSeparator);
    if (localDirs.length > 0) {
      for (String perPath : logsDirs) {
        mount = mount + " -v " + perPath + ":" + perPath;
      }
    }

    String dockerHost = conf.get(XLearningConfiguration.XLEARNING_DOCKER_REGISTRY_HOST);
    String dockerPort = conf.get(XLearningConfiguration.XLEARNING_DOCKER_REGISTRY_PORT);
    String dockerImageName = conf.get(XLearningConfiguration.XLEARNING_DOCKER_IMAGE);
    if (dockerPort != null && dockerPort.length() > 0 && dockerHost != null && dockerPort != null) {
      dockerImageName = dockerHost + ":" + dockerPort + "/" + dockerImageName;
    }

    try {
      String dockerPullCommand = "docker pull " + dockerImageName;
      LOG.info("Docker Pull command:" + dockerPullCommand);
      Process process = rt.exec(dockerPullCommand, envp);
      int i = process.waitFor();
      LOG.info("Docker Pull Wait:" + (i == 0 ? "Success" : "Failed"));
      BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
      String line;
      while ((line = br.readLine()) != null) {
        LOG.info(line);
      }
    } catch (InterruptedException e) {
      LOG.warn("Docker pull Error:", e);
    }
    String dockerCommand =
        "docker run" +
            " --network host " +
            " --rm " +
            " --cpus " + containerCpu +
            " -m " + containerMemory + "m " +
            port +
            " -w " +workDir +
            mount +
            envsParam.toString() +
            " --name " + containerId.toString() + " " +
            runArgs + " " +
            dockerImageName;
    dockerCommand += " " + command;
    LOG.info("Docker command:" + dockerCommand);
    xlearningProcess = rt.exec(dockerCommand, envp);
//    Utilities.printProcessOutput(xlearningProcess);

    return xlearningProcess;
  }
}
