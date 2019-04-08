package net.qihoo.xlearning.container;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import net.qihoo.xlearning.api.ApplicationContainerProtocol;
import net.qihoo.xlearning.api.XLearningConstants;
import net.qihoo.xlearning.common.*;
import net.qihoo.xlearning.conf.XLearningConfiguration;
import net.qihoo.xlearning.util.Utilities;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Heartbeat extends Thread {

  private static final Log LOG = LogFactory.getLog(Heartbeat.class);

  private ApplicationContainerProtocol protocol;

  private Configuration conf;

  private XLearningContainerId containerId;

  private HeartbeatRequest heartbeatRequest;

  private HeartbeatResponse heartbeatResponse;

  private int heartbeatInterval;

  private int heartbeatRetryMax;

  private Long lastInnerModelTimeStamp;

  private Long previousInnerModelTimeStamp;

  private Boolean IsXLearningTrainCompleted;

  private int outputIndex;

  private int index;

  private String role;

  private int downloadRetry;

  private int uploadTimeOut;

  public Heartbeat(ApplicationContainerProtocol protocol, Configuration conf,
                   XLearningContainerId xlearningContainerId, int outputIndex, int index, String role) {
    this.protocol = protocol;
    this.conf = conf;
    this.containerId = xlearningContainerId;
    this.heartbeatRequest = new HeartbeatRequest();
    this.heartbeatResponse = new HeartbeatResponse();
    this.lastInnerModelTimeStamp = Long.MIN_VALUE;
    this.previousInnerModelTimeStamp = Long.MIN_VALUE;
    this.IsXLearningTrainCompleted = false;
    this.heartbeatInterval = this.conf.getInt(XLearningConfiguration.XLEARNING_CONTAINER_HEARTBEAT_INTERVAL, XLearningConfiguration.DEFAULT_XLEARNING_CONTAINER_HEARTBEAT_INTERVAL);
    this.heartbeatRetryMax = this.conf.getInt(XLearningConfiguration.XLEARNING_CONTAINER_HEARTBEAT_RETRY, XLearningConfiguration.DEFAULT_XLEARNING_CONTAINER_HEARTBEAT_RETRY);
    this.downloadRetry = this.conf.getInt(XLearningConfiguration.XLEARNING_DOWNLOAD_FILE_RETRY, XLearningConfiguration.DEFAULT_XLEARNING_DOWNLOAD_FILE_RETRY);
    this.uploadTimeOut = this.conf.getInt(XLearningConfiguration.XLEARNING_INTERRESULT_UPLOAD_TIMEOUT, XLearningConfiguration.DEFAULT_XLEARNING_INTERRESULT_UPLOAD_TIMEOUT);
    this.outputIndex = outputIndex;
    this.index = index;
    this.role = role;
  }

  @SuppressWarnings("static-access")
  public void run() {
    while (!Thread.currentThread().interrupted()) {
      heartbeatResponse = heartbeatWithRetry();
      heartbeatResponseHandle(heartbeatResponse);
      Utilities.sleep(heartbeatInterval);
    }
  }

  public void setContainerStatus(XLearningContainerStatus containerStatus) {
    this.heartbeatRequest.setXLearningContainerStatus(containerStatus);
  }

  public void setInnerModelSavedStatus(Boolean flag) {
    this.heartbeatRequest.setInnerModelSavedStatus(flag);
  }

  public void setProgressLog(String xlearningProgressLog) {
    this.heartbeatRequest.setProgressLog(xlearningProgressLog);
  }

  public void setContainersStartTime(String startTime) {
    this.heartbeatRequest.setContainersStartTime(startTime);
  }

  public void setContainersFinishTime(String finishTime) {
    this.heartbeatRequest.setContainersFinishTime(finishTime);
  }

  public Boolean isXLearningTrainCompleted() {
    return this.IsXLearningTrainCompleted;
  }

  public HeartbeatResponse heartbeatWithRetry() {
    int retry = 0;
    while (true) {
      try {
        heartbeatResponse = protocol.heartbeat(containerId, heartbeatRequest);
        LOG.debug("Send HeartBeat to ApplicationMaster");
        return heartbeatResponse;
      } catch (Exception e) {
        retry++;
        if (retry <= heartbeatRetryMax) {
          LOG.warn("Send heartbeat to ApplicationMaster failed in retry " + retry);
          Utilities.sleep(heartbeatInterval);
        } else {
          LOG.warn("Send heartbeat to ApplicationMaster failed in retry " + retry
              + ", container will suicide!", e);
          System.exit(1);
        }
      }
    }
  }

  public void heartbeatResponseHandle(HeartbeatResponse heartbeatResponse) {
    LOG.debug("Received the heartbeat response from the AM. CurrentJob finished " + heartbeatResponse.getIsXLearningTrainCompleted()
        + " , currentInnerModelSavedTimeStamp is " + heartbeatResponse.getInnerModelTimeStamp());
    if (!heartbeatResponse.getIsXLearningTrainCompleted()) {
      if (!heartbeatResponse.getInnerModelTimeStamp().equals(lastInnerModelTimeStamp)) {
        previousInnerModelTimeStamp = lastInnerModelTimeStamp;
        lastInnerModelTimeStamp = heartbeatResponse.getInnerModelTimeStamp();
        ExecutorService executor = Executors.newFixedThreadPool(
            conf.getInt(XLearningConfiguration.XLEARNING_UPLOAD_OUTPUT_THREAD_NUMS, XLearningConfiguration.DEFAULT_XLEARNING_DOWNLOAD_FILE_THREAD_NUMS),
            new ThreadFactoryBuilder()
                .setDaemon(true)
                .setNameFormat("Upload-InnerModel-Thread #%d")
                .build()
        );

        try {
          for (OutputInfo outputs : protocol.getOutputLocation()) {
            LOG.info("Output path: " + outputs.getLocalLocation() + "#" + outputs.getDfsLocation());
            FileSystem localFs = FileSystem.getLocal(conf);
            Path localPath = new Path(outputs.getLocalLocation());
            Path remotePath = new Path(outputs.getDfsLocation()
                + conf.get(XLearningConfiguration.XLEARNING_INTERREAULST_DIR, XLearningConfiguration.DEFAULT_XLEARNING_INTERRESULT_DIR)
                + new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss").format(new Date(lastInnerModelTimeStamp))
                + "/" + containerId.toString());
            LOG.info("InnerModel path:" + remotePath);
            FileSystem dfs = remotePath.getFileSystem(conf);
            if (dfs.exists(remotePath)) {
              LOG.info("Container remote output path " + remotePath + "exists, so we has to delete is first.");
              dfs.delete(remotePath);
            }
            dfs.close();
            if (localFs.exists(localPath)) {
              String splitDir = localPath.toString();
              if (!localPath.toString().endsWith("/")) {
                splitDir = localPath.toString() + "/";
              } else if (!localPath.toString().startsWith("/")) {
                splitDir = "/" + localPath.toString();
              }
              List<FileStatus> uploadFiles = Utilities.listStatusRecursively(localPath,
                  localFs, null, conf.getInt(XLearningConfiguration.XLEARNING_FILE_LIST_LEVEL, XLearningConfiguration.DEFAULT_XLEARNING_FILE_LIST_LEVEL));
              for (FileStatus uploadFile : uploadFiles) {
                if (conf.getBoolean(XLearningConfiguration.XLEARNING_INTERRESULT_SAVE_INC, XLearningConfiguration.DEFAULT_XLEARNING_INTERRESULT_SAVE_INC) && uploadFile.getModificationTime() <= previousInnerModelTimeStamp) {
                  LOG.debug("current file " + uploadFile.getPath().toString() + " not changed after last saved.");
                } else {
                  Path uploadPath = uploadFile.getPath();
                  LOG.debug("upload:" + uploadPath + " \tfrom\tlocalPath:" + localPath);
                  String[] fileName = StringUtils.splitByWholeSeparator(uploadPath.toString() + "/", splitDir, 2);
                  if (fileName.length == 2) {
                    Path uploadDstPath = new Path(remotePath.toString() + "/" + fileName[1]);
                    UploadTask uploadTask = new UploadTask(conf, uploadDstPath, uploadPath);
                    LOG.debug("upload from " + uploadPath + " to " + uploadDstPath);
                    executor.submit(uploadTask);
                  } else {
                    LOG.error("Get the local path error");
                  }
                }
              }
            }
            localFs.close();
          }
        } catch (IOException e) {
          LOG.error("container " + containerId + "upload the interResult error:" + e);
        }
        executor.shutdown();
        Boolean isUploadInnerFinish = false;
        try {
          isUploadInnerFinish = executor.awaitTermination(uploadTimeOut, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
          LOG.error("UploadInnerThread exception: " + e);
        }
        if (isUploadInnerFinish) {
          LOG.info("container " + containerId + " currentStatus:" + heartbeatRequest.getXLearningContainerStatus() + " , savedModel completed");
          setInnerModelSavedStatus(true);
        } else {
          List<Runnable> list = executor.shutdownNow();
          LOG.info("InnerModel Upload timeout, more than set :" + uploadTimeOut + " s. Already has the " + list.size() + " task not executed.");
          setInnerModelSavedStatus(true);
        }
      }
      LOG.debug("container " + containerId + " currentStatus:" + heartbeatRequest.getXLearningContainerStatus());
    }
    this.IsXLearningTrainCompleted = heartbeatResponse.getIsXLearningTrainCompleted();
  }
}
