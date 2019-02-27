package net.qihoo.xlearning.util;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import net.qihoo.xlearning.conf.XLearningConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.yarn.api.records.ApplicationId;
import org.apache.hadoop.yarn.api.records.LocalResource;
import org.apache.hadoop.yarn.api.records.LocalResourceType;
import org.apache.hadoop.yarn.api.records.LocalResourceVisibility;
import org.apache.hadoop.yarn.util.ConverterUtils;
import org.apache.hadoop.yarn.util.Records;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

public final class Utilities {
  private static Log LOG = LogFactory.getLog(Utilities.class);

  private Utilities() {
  }

  public static void sleep(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      LOG.warn("Sleeping are Interrupted ...", e);
    }
  }

  public static List<FileStatus> listStatusRecursively(Path path, FileSystem fs, List<FileStatus> fileStatuses)
      throws IOException {
    if (fileStatuses == null) {
      fileStatuses = new ArrayList<>(1000);
    }
    LOG.info("input path: " + path.toString());
    FileStatus[] fileStatus = fs.listStatus(path);
    if (fileStatus != null && fileStatus.length > 0) {
      for (FileStatus f : fileStatus) {
        if (fs.isDirectory(f.getPath())) {
          listStatusRecursively(f.getPath(), fs, fileStatuses);
        } else {
          fileStatuses.add(f);
        }
      }
    } else {
      LOG.info("input list size:" + fileStatus.length);
      if (fileStatus == null) {
        LOG.info("fileStatus is null");
      }
    }
    return fileStatuses;
  }

  public static List<Path> convertStatusToPath(List<FileStatus> fileStatuses) {
    List<Path> paths = new ArrayList<>();
    if (fileStatuses != null) {
      for (FileStatus fileStatus : fileStatuses) {
        paths.add(fileStatus.getPath());
      }
    }
    return paths;
  }

  public static Path getRemotePath(XLearningConfiguration conf, ApplicationId appId, String fileName) {
    String pathSuffix = appId.toString() + "/" + fileName;
    Path remotePath = new Path(conf.get(XLearningConfiguration.XLEARNING_STAGING_DIR, XLearningConfiguration.DEFAULT_XLEARNING_STAGING_DIR),
        pathSuffix);
    remotePath = new Path(conf.get("fs.defaultFS"), remotePath);
    LOG.debug("Got remote path of " + fileName + " is " + remotePath.toString());
    return remotePath;
  }

  public static void setPathExecutableRecursively(String path) {
    File file = new File(path);
    if (!file.exists()) {
      LOG.warn("Path " + path + " does not exist!");
      return;
    }
    if (!file.setExecutable(true)) {
      LOG.error("Failed to set executable for " + path);
    }

    if (file.isDirectory()) {
      File[] files = file.listFiles();
      if (null != files && files.length > 0) {
        setPathExecutableRecursively(file.getAbsolutePath());
      }
    }
  }

  public static boolean mkdirs(String path) {
    return mkdirs(path, false);
  }

  public static boolean mkdirs(String path, boolean needDelete) {
    File file = new File(path);
    if (file.exists()) {
      if (needDelete) {
        file.delete();
      } else {
        return true;
      }
    }
    return file.mkdirs();
  }

  public static boolean mkParentDirs(String outFile) {
    File dir = new File(outFile);
    dir = dir.getParentFile();
    return dir.exists() || dir.mkdirs();
  }

  public static LocalResource createApplicationResource(FileSystem fs, Path path, LocalResourceType type)
      throws IOException {
    LocalResource localResource = Records.newRecord(LocalResource.class);
    FileStatus fileStatus = fs.getFileStatus(path);
    localResource.setResource(ConverterUtils.getYarnUrlFromPath(path));
    localResource.setSize(fileStatus.getLen());
    localResource.setTimestamp(fileStatus.getModificationTime());
    localResource.setType(type);
    localResource.setVisibility(LocalResourceVisibility.APPLICATION);
    return localResource;
  }

  public static void addPathToEnvironment(Map<String, String> env, String userEnvKey, String userEnvValue) {
    if (env.containsKey(userEnvKey)) {
      env.put(userEnvKey, userEnvValue + System.getProperty("path.separator") + env.get(userEnvKey) + System.getProperty("path.separator") + System.getenv(userEnvKey));
    } else {
      env.put(userEnvKey, userEnvValue + System.getProperty("path.separator") + System.getenv(userEnvKey));
    }
  }

  public static void getReservePort(Socket socket, String localHost, int reservePortBegin, int reservePortEnd) throws IOException {
    int i = 0;
    Random random = new Random(System.currentTimeMillis());
    while (i < 1000) {
      int rand = random.nextInt(reservePortEnd - reservePortBegin);
      try {
        socket.bind(new InetSocketAddress(localHost, reservePortBegin + rand));
        return;
      } catch (IOException e) {
        try {
          Thread.sleep(1000);
        } catch (InterruptedException e2) {}
      }
    }
    throw new IOException("couldn't allocate a unused port");
  }

  public static boolean isDockerAlive(String containerName) {
    boolean isAlive = false;
    Runtime runtime = Runtime.getRuntime();
    Process process;
    try {
      process = runtime.exec(
          new String[]{"/bin/bash", "-c", "docker ps --filter name=" + containerName + " | wc -l"});
      InputStream is = process.getInputStream();
      InputStreamReader isr = new InputStreamReader(is);
      BufferedReader br = new BufferedReader(isr);
      String line = null;
      StringBuffer sb = new StringBuffer();
      while ((line = br.readLine()) != null) {
        sb.append(line);
      }
      int result = Integer.parseInt(sb.toString());
      LOG.info("Docker running count:" + sb.toString());
      is.close();
      isr.close();
      br.close();
      if (result >= 2) {
        isAlive = true;
      }
    } catch (IOException e) {
      LOG.error("Docker check error:", e);
    }
    return isAlive;
  }

  public static boolean isProcessAlive(Process processid){
    try {
      Method isAliveMethod = processid.getClass().getMethod("isAlive");
      Boolean isAlive = (Boolean) isAliveMethod.invoke(processid);
      return isAlive;
    } catch (NoSuchMethodException e){
      try {
        processid.exitValue();
        return false;
      } catch(IllegalThreadStateException e1) {
        return true;
      }
    } catch (Exception et){
      return true;
    }
  }

}
