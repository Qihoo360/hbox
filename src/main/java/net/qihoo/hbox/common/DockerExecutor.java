package net.qihoo.hbox.common;

import net.qihoo.hbox.conf.HboxConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;

public class DockerExecutor implements ILaunch {

    private static final Log LOG = LogFactory.getLog(DockerLaunch.class);
    private String containerId;
    private Process hboxProcess;
    private HboxConfiguration conf;
    private String runArgs;
    private String dockerPath;

    public DockerExecutor(String containerId, HboxConfiguration conf) {
        this.containerId = containerId;
        this.conf = conf;
        this.runArgs = conf.get(HboxConfiguration.HBOX_DOCKER_RUN_ARGS, "");
        LOG.info("docker run args:" + runArgs);
        this.dockerPath = "/bin/nvidia-docker";
    }

    @Override
    /**
     * @para envp 作业的相关环境变量，包括index，rolo，path作业的一些参数等
     * @pare envs 当前系统的环境变量，本机的本地环境
     */
    public Process exec(String command, String[] envp, Map<String, String> envs, File dir) throws IOException {
        LOG.info("docker command:" + command + ",envs:" + envs);
        Runtime rt = Runtime.getRuntime();
        String workDir = "/" + conf.get(HboxConfiguration.HBOX_DOCKER_WORK_DIR, HboxConfiguration.DEFAULT_HBOX_DOCKER_WORK_DIR);
        String path = new File("").getAbsolutePath();
        StringBuilder envsParam = new StringBuilder();
        //把container的环境变量添加到docker中去
        for (String keyValue : envp) {
            if (keyValue.startsWith("PATH") || keyValue.startsWith("CLASSPATH")) {
                continue;
            } else {
                envsParam.append(" --env " + keyValue + "");
            }
        }
        String mount = " -v " + path + ":" + workDir;
        String[] localDirs = envs.get("LOCAL_DIRS").split(",");
        Boolean publicFlag = conf.get(HboxConfiguration.HBOX_LOCAL_RESOURCE_VISIBILITY, HboxConfiguration.DEFAULT_HBOX_LOCAL_RESOURCE_VISIBILITY).equalsIgnoreCase("public");
        if (localDirs.length > 0) {
            for (String perPath : localDirs) {
                if (publicFlag) {
                    String[] localPath = perPath.split("usercache");
                    mount = mount + " -v " + localPath[0] + "filecache" + ":" + localPath[0] + "filecache";
                } else {
                    mount = mount + " -v " + perPath + ":" + perPath;
                }
            }
        }
        String[] logsDirs = envs.get("LOG_DIRS").split(",");
        if (localDirs.length > 0) {
            for (String perPath : logsDirs) {
                mount = mount + " -v " + perPath + ":" + perPath;
            }
        }

        String dockerImageName = conf.get(HboxConfiguration.HBOX_DOCKER_IMAGE_NAME);
        //从仓库拉取镜像文件
        try {
            String dockerPullCommand = this.dockerPath + " pull " + dockerImageName;
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

        StringBuilder commands = new StringBuilder();
        String dockerCommand = commands.append(dockerPath)
                .append(" ")
                .append("run")
                .append(" ")
                .append("--rm --net=host")
                .append(" --name " + containerId)
                .append(mount)
                .append(" -w " + workDir)
                .append(" ")
                .append(envsParam.toString())
                .append(" ")
                .append(runArgs)
                .append(" ")
                .append(dockerImageName)
                .toString();
        dockerCommand += " " + command;
        LOG.info("Docker command:" + dockerCommand);
        hboxProcess = rt.exec(dockerCommand, envp);

        return hboxProcess;
    }
}
