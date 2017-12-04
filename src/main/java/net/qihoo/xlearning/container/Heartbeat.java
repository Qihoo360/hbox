package net.qihoo.xlearning.container;

import net.qihoo.xlearning.api.ApplicationContainerProtocol;
import net.qihoo.xlearning.common.HeartbeatResponse;
import net.qihoo.xlearning.common.HeartbeatRequest;
import net.qihoo.xlearning.common.OutputInfo;
import net.qihoo.xlearning.common.XLearningContainerStatus;
import net.qihoo.xlearning.conf.XLearningConfiguration;
import net.qihoo.xlearning.util.Utilities;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import java.text.SimpleDateFormat;
import java.util.Date;

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

  private Boolean IsXLearningTrainCompleted;

  public Heartbeat(ApplicationContainerProtocol protocol, Configuration conf,
                   XLearningContainerId xlearningContainerId) {
    this.protocol = protocol;
    this.conf = conf;
    this.containerId = xlearningContainerId;
    this.heartbeatRequest = new HeartbeatRequest();
    this.heartbeatResponse = new HeartbeatResponse();
    this.lastInnerModelTimeStamp = Long.MIN_VALUE;
    this.IsXLearningTrainCompleted = false;
    this.heartbeatInterval = this.conf.getInt(XLearningConfiguration.XLEARNING_CONTAINER_HEARTBEAT_INTERVAL, XLearningConfiguration.DEFAULT_XLEARNING_CONTAINER_HEARTBEAT_INTERVAL);
    this.heartbeatRetryMax = this.conf.getInt(XLearningConfiguration.XLEARNING_CONTAINER_HEARTBEAT_RETRY, XLearningConfiguration.DEFAULT_XLEARNING_CONTAINER_HEARTBEAT_RETRY);
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
        lastInnerModelTimeStamp = heartbeatResponse.getInnerModelTimeStamp();
        Thread interResultSavedThread = new Thread(new Runnable() {
          @Override
          public void run() {
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
                if (localFs.exists(localPath)) {
                  LOG.info("Start upload output " + localPath + " to remote path " + remotePath);
                  dfs.copyFromLocalFile(false, false, localPath, remotePath);
                  LOG.info("Upload output " + localPath + " to remote path " + remotePath + " finished.");
                }
                localFs.close();
              }
              LOG.info("container " + containerId + " currentStatus:" + heartbeatRequest.getXLearningContainerStatus() + " , savedModel completed");
            } catch (Exception e) {
              LOG.error("upload the interResult error:" + e);
            } finally {
              Long timeInterval = System.currentTimeMillis() - lastInnerModelTimeStamp;
              if (timeInterval <= conf.getInt(XLearningConfiguration.XLEARNING_INTERRESULT_UPLOAD_TIMEOUT, XLearningConfiguration.DEFAULT_XLEARNING_INTERRESULT_UPLOAD_TIMEOUT)) {
                setInnerModelSavedStatus(true);
              }
            }
          }
        });
        interResultSavedThread.start();
      } else if (!lastInnerModelTimeStamp.equals(Long.MIN_VALUE)) {
        Long timeInterval = System.currentTimeMillis() - lastInnerModelTimeStamp;
        if (timeInterval > conf.getInt(XLearningConfiguration.XLEARNING_INTERRESULT_UPLOAD_TIMEOUT, XLearningConfiguration.DEFAULT_XLEARNING_INTERRESULT_UPLOAD_TIMEOUT)) {
          setInnerModelSavedStatus(true);
        }
      }
    }
    this.IsXLearningTrainCompleted = heartbeatResponse.getIsXLearningTrainCompleted();
  }
}
