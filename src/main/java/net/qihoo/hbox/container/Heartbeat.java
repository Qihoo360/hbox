package net.qihoo.hbox.container;

import net.qihoo.hbox.api.ApplicationContainerProtocol;
import net.qihoo.hbox.common.HeartbeatResponse;
import net.qihoo.hbox.common.HeartbeatRequest;
import net.qihoo.hbox.common.OutputInfo;
import net.qihoo.hbox.common.HboxContainerStatus;
import net.qihoo.hbox.conf.HboxConfiguration;
import net.qihoo.hbox.util.Utilities;
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

  private HboxContainerId containerId;

  private HeartbeatRequest heartbeatRequest;

  private HeartbeatResponse heartbeatResponse;

  private int heartbeatInterval;

  private int heartbeatRetryMax;

  private Long lastInnerModelTimeStamp;

  private Boolean IsHboxTrainCompleted;

  private String containerStdOut;

  private String containerStdErr;

  public Heartbeat(ApplicationContainerProtocol protocol, Configuration conf,
                   HboxContainerId hboxContainerId) {
    this.protocol = protocol;
    this.conf = conf;
    this.containerId = hboxContainerId;
    this.heartbeatRequest = new HeartbeatRequest();
    this.heartbeatResponse = new HeartbeatResponse();
    this.lastInnerModelTimeStamp = Long.MIN_VALUE;
    this.IsHboxTrainCompleted = false;
    this.heartbeatInterval = this.conf.getInt(HboxConfiguration.HBOX_CONTAINER_HEARTBEAT_INTERVAL, HboxConfiguration.DEFAULT_HBOX_CONTAINER_HEARTBEAT_INTERVAL);
    this.heartbeatRetryMax = this.conf.getInt(HboxConfiguration.HBOX_CONTAINER_HEARTBEAT_RETRY, HboxConfiguration.DEFAULT_HBOX_CONTAINER_HEARTBEAT_RETRY);
    this.containerStdOut = "";
    this.containerStdErr = "";
  }

  @SuppressWarnings("static-access")
  public void run() {
    while (!Thread.currentThread().interrupted()) {
      heartbeatResponse = heartbeatWithRetry();
      heartbeatResponseHandle(heartbeatResponse);
      Utilities.sleep(heartbeatInterval);
    }
  }

  public void setContainerStatus(HboxContainerStatus containerStatus) {
    this.heartbeatRequest.setHboxContainerStatus(containerStatus);
  }

  public void setInnerModelSavedStatus(Boolean flag) {
    this.heartbeatRequest.setInnerModelSavedStatus(flag);
  }

  public void setProgressLog(String hboxProgressLog) {
    this.heartbeatRequest.setProgressLog(hboxProgressLog);
  }

  public void setContainersStartTime(String startTime) {
    this.heartbeatRequest.setContainersStartTime(startTime);
  }

  public void setContainersFinishTime(String finishTime) {
    this.heartbeatRequest.setContainersFinishTime(finishTime);
  }

  public void appendContainerStdOut(String stdOut) {
    this.heartbeatRequest.appendContainerStdOut(stdOut);
  }

  public void appendContainerStdErr(String stdErr) {
    this.heartbeatRequest.appendContainerStdErr(stdErr);
  }

  public Boolean isHboxTrainCompleted() {
    return this.IsHboxTrainCompleted;
  }

  public HeartbeatResponse heartbeatWithRetry() {
    int retry = 0;
    while (true) {
      try {
        heartbeatResponse = protocol.heartbeat(containerId, heartbeatRequest);
        LOG.debug("Send HeartBeat to ApplicationMaster");
        if (conf.getBoolean(HboxConfiguration.HBOX_CONTAINER_RUNNING_LOG_ENABLE, HboxConfiguration.DEFAULT_HBOX_CONTAINER_RUNNING_LOG_ENABLE)) {
          heartbeatRequest.clearContainerStdOut();
          heartbeatRequest.clearContainerStdErr();
        }
        return heartbeatResponse;
      } catch (Exception e) {
        retry++;
        if (retry <= heartbeatRetryMax) {
          LOG.info("Send heartbeat to ApplicationMaster failed in retry " + retry);
          Utilities.sleep(heartbeatInterval);
        } else {
          LOG.info("Send heartbeat to ApplicationMaster failed in retry " + retry
              + ", container will suicide!", e);
          System.exit(1);
        }
      }
    }
  }

  public void heartbeatResponseHandle(HeartbeatResponse heartbeatResponse) {
    LOG.debug("Received the heartbeat response from the AM. CurrentJob finished " + heartbeatResponse.getIsHboxTrainCompleted()
        + " , currentInnerModelSavedTimeStamp is " + heartbeatResponse.getInnerModelTimeStamp());
    if (!heartbeatResponse.getIsHboxTrainCompleted()) {
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
                    + conf.get(HboxConfiguration.HBOX_INTERREAULST_DIR, HboxConfiguration.DEFAULT_HBOX_INTERRESULT_DIR)
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
              }
              LOG.info("container " + containerId + " currentStatus:" + heartbeatRequest.getHboxContainerStatus() + " , savedModel completed");
            } catch (Exception e) {
              LOG.error("container " + containerId + "upload the interResult error:" + e);
            }
            Long timeInterval = System.currentTimeMillis() - lastInnerModelTimeStamp;
            if (timeInterval <= conf.getInt(HboxConfiguration.HBOX_INTERRESULT_UPLOAD_TIMEOUT, HboxConfiguration.DEFAULT_HBOX_INTERRESULT_UPLOAD_TIMEOUT)) {
              setInnerModelSavedStatus(true);
            }
          }
        });
        interResultSavedThread.start();
      } else if (!lastInnerModelTimeStamp.equals(Long.MIN_VALUE)) {
        Long timeInterval = System.currentTimeMillis() - lastInnerModelTimeStamp;
        if (timeInterval > conf.getInt(HboxConfiguration.HBOX_INTERRESULT_UPLOAD_TIMEOUT, HboxConfiguration.DEFAULT_HBOX_INTERRESULT_UPLOAD_TIMEOUT)) {
          setInnerModelSavedStatus(true);
        }
      }
    }
    this.IsHboxTrainCompleted = heartbeatResponse.getIsHboxTrainCompleted();
  }
}
