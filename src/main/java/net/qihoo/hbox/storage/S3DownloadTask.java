package net.qihoo.hbox.storage;

import net.qihoo.hbox.common.HboxContainerStatus;
import net.qihoo.hbox.conf.HboxConfiguration;
import net.qihoo.hbox.container.Heartbeat;
import net.qihoo.hbox.util.Utilities;
import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import java.io.*;
import java.util.Date;

public class S3DownloadTask implements Runnable {

    private static final Log LOG = LogFactory.getLog(S3UploadTask.class);
    private AmazonS3 s3;
    private Configuration conf;
    private final int downloadRetry;
    private final String objectKey;
    private final String downloadDst;
    private Heartbeat heartbeatThread;
    private int heartbeatInterval;

    public S3DownloadTask(Heartbeat hb, Configuration conf, AmazonS3 s3, String objectKey, String downloadDst) {
        this.s3 = s3;
        this.downloadRetry = conf.getInt(HboxConfiguration.HBOX_DOWNLOAD_FILE_RETRY, HboxConfiguration.DEFAULT_HBOX_DOWNLOAD_FILE_RETRY);
        this.objectKey = objectKey;
        this.downloadDst = downloadDst;
        this.heartbeatThread = hb;
        this.heartbeatInterval = conf.getInt(HboxConfiguration.HBOX_CONTAINER_HEARTBEAT_INTERVAL, HboxConfiguration.DEFAULT_HBOX_CONTAINER_HEARTBEAT_INTERVAL);
    }

    @Override
    public void run() {
        LOG.info("Downloading input file " + this.objectKey + " from Bucket" + this.s3.getBucketName() + " to " + this.downloadDst);
        int retry = 0;
        while (true) {
            InputStream in = null;
            try {
                File exist = new File(downloadDst);
                if (exist.exists()) {
                    exist.delete();
                }
                in = this.s3.get(this.objectKey);
                FileUtils.copyInputStreamToFile(in, exist);
                LOG.info("Downloading input file " + this.objectKey + " successful.");
                break;
            } catch (Exception e) {
                if (retry < downloadRetry) {
                    LOG.warn("Download input file " + this.objectKey + " failed, retry in " + (++retry), e);
                } else {
                    LOG.error("Download input file " + this.objectKey + " failed after " + downloadRetry + " retry times!", e);
                    reportFailedAndExit();
                }
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {
                    }
                }
            }
        }
    }

    private void reportFailedAndExit() {
        Date now = new Date();
        heartbeatThread.setContainersFinishTime(now.toString());
        heartbeatThread.setContainerStatus(HboxContainerStatus.FAILED);
        Utilities.sleep(heartbeatInterval);
        System.exit(-1);
    }
}