package net.qihoo.xlearning.container;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.qihoo.xlearning.conf.XLearningConfiguration;
import net.qihoo.xlearning.util.Utilities;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.yarn.conf.YarnConfiguration;

public class DockerContainer implements IContainerLaunch {

  private static final Log LOG = LogFactory.getLog(DockerContainer.class);
  private XLearningContainerId containerId;
  private Process xlearningProcess;
  private XLearningConfiguration conf;
  private String runArgs;
  private boolean useHarbor = false;

  public DockerContainer(XLearningContainerId containerId, XLearningConfiguration conf) {
    this.containerId = containerId;
    this.conf = conf;
    this.runArgs = conf.get(XLearningConfiguration.XLEARNING_DOCKER_RUN_ARGS,
        "");
    this.useHarbor = conf.getBoolean(XLearningConfiguration.XLEARNING_DOCKER_USEHARBOR,
        XLearningConfiguration.DEFAULT_XLEARNING_DOCKER_USEHARBOR);

  }

  @Override
  public boolean isAlive() {
    if (xlearningProcess != null && xlearningProcess.isAlive()) {
      return true;
    } else if (Utilities.isDockerAlive(containerId.toString())) {
      return true;
    }
    return false;
  }

  @Override
  public Process exec(String command, String[] envp, Map<String, String> envs) throws IOException {
    LOG.info("docker command:" + command + ",envs:" + envs);
    Runtime rt = Runtime.getRuntime();
    String port = conf.get("RESERVED_PORT");
    String path = new File("").getAbsolutePath();
    StringBuilder envsParam = new StringBuilder();
    String[] skipEnvs = conf.getStrings(XLearningConfiguration.XLEARNING_DOCKER_SKIP_ENV,
        XLearningConfiguration.DEFAULT_XLEARNING_DOCKER_SKIP_ENV);
    Set<String> skipEnvSet = new HashSet<>();
    for (String per : skipEnvs) {
      skipEnvSet.add(per);
    }
    for (String keyValue : envp) {
      try {
        if (skipEnvSet.contains(keyValue.split("=")[0])) {
          continue;
        }
      } catch (Exception e) {
      }
      envsParam.append(" --env " + keyValue + "");
    }
    if (port.equals("-1")) {
      port = "";
    } else {
      port = " -p " + port;
    }
    if (command.contains("DOCKER_RESERVED_PORT")) {
      command = command.replace("DOCKER_RESERVED_PORT", port);
    }
    if (command.contains("XLEARNING_CONTAINER_NAME")) {
      command = command.replace("XLEARNING_CONTAINER_NAME", containerId.toString());
    }
    String containerMemory = envs.get("DOCKER_CONTAINER_MEMORY");
    String containerCpu = envs.get("DOCKER_CONTAINER_CPU");
    String userName = conf.get("hadoop.job.ugi");
    String[] userNameArr = userName.split(",");
    if (userNameArr.length > 1) {
      userName = userNameArr[0];
    }
    LOG.info("Container launch userName:" + userName);
    String appId = conf.get(XLearningConfiguration.XLEARNING_APP_ID);
    String homePath = envs.get("HADOOP_HDFS_HOME");
    String mount = " -v " + path + ":" + "/work";
    mount += " -v " + homePath + ":" + homePath;
    mount += " -v " + "/home/yarn/software/hadoop:/home/yarn/software/hadoop";
    mount += " -v " + "/etc/passwd:/etc/passwd";
    String[] localDirs = conf.getStrings(YarnConfiguration.NM_LOCAL_DIRS);
    if (localDirs.length > 0) {
      for (String perPath : localDirs) {
        String basePath = perPath + "/usercache/" + userName + "/appcache/" + appId;
        mount = mount + " -v " + basePath + ":" + basePath;
      }
    }
    String[] logsDirs = conf.getStrings(YarnConfiguration.NM_LOG_DIRS);
    if (localDirs.length > 0) {
      for (String perPath : logsDirs) {
        String basePath = perPath + "/" + appId;
        mount = mount + " -v " + basePath + ":" + basePath;
      }
    }
    String dockerUserCommand = "id -u";
    String user = dockerCommandExe(dockerUserCommand, rt, envp);
    if (useHarbor) {
      String dockerLoginCommand =
          "docker login --username " + conf.get(XLearningConfiguration.XLEARNING_DOCKER_USERNAME)
              + " --password " + conf
              .get(XLearningConfiguration.XLEARNING_DOCKER_PASSWORD) + " " + conf
              .get(XLearningConfiguration.XLEARNING_DOCKER_REGISTRY_HOST);
      dockerCommandExe(dockerLoginCommand, rt, envp);
    }
    String dockerImage = "";
    if (useHarbor) {
      dockerImage =
          envs.get("DOCKER_REGISTRY_HOST")
              + "/" + envs.get("DOCKER_REGISTRY_IMAGE");
    } else {
      dockerImage =
          envs.get("DOCKER_REGISTRY_HOST") + ":" + envs.get("DOCKER_REGISTRY_PORT")
              + "/" + envs.get("DOCKER_REGISTRY_IMAGE");
    }
    String dockerPullCommand = "docker pull " + dockerImage;
    LOG.info("Docker Pull command:" + dockerPullCommand);
    dockerCommandExe(dockerPullCommand, rt, envp);
    String dockerCommand =
        "docker run" +
            " --network host " +
            " -u " + user +
            " --rm " +
            " --cpus " + containerCpu +
            " -m " + containerMemory + "m " +
            port +
            mount +
            envsParam.toString() +
            " --name " + containerId.toString() + " " +
            runArgs + " " +
            dockerImage;
    dockerCommand += " " + command;
    LOG.info("Docker command:" + dockerCommand);
    xlearningProcess = rt.exec(dockerCommand, envp);
    return xlearningProcess;
  }
  private String dockerCommandExe(String dockerCommand, Runtime rt, String[] envp) {
    StringBuffer result = new StringBuffer();

    try {
      Process process = rt.exec(dockerCommand, envp);
      int i = process.waitFor();
      LOG.info("Docker Command:" + i);
      BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
      String line;
      while ((line = br.readLine()) != null) {
        LOG.info(line);
        result.append(line);
      }
    } catch (Exception e) {
      LOG.warn("Docker command " + dockerCommand + "Error:", e);
    }
    return result.toString();
  }

}
