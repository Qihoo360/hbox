package net.qihoo.hbox.storage;

import net.qihoo.hbox.conf.HboxConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import java.io.File;

public class S3UploadTask implements Runnable {
    private static final Log LOG = LogFactory.getLog(S3UploadTask.class);
    private AmazonS3 s3;
    private final int downloadRetry;
    private final String objectKey;
    private final String uploadSrc;

    public S3UploadTask(Configuration conf, AmazonS3 s3, String objectKey, String src){
        this.s3 = s3;
        this.downloadRetry = conf.getInt(HboxConfiguration.HBOX_DOWNLOAD_FILE_RETRY, HboxConfiguration.DEFAULT_HBOX_DOWNLOAD_FILE_RETRY);
        this.objectKey = objectKey;
        if(src.startsWith("file:"))
            src= src.replaceFirst("file:", "");
        this.uploadSrc = src;
    }

    @Override
    public void run() {
        LOG.info("Upload output file " + this.objectKey + " to Amazon S3 bucket " + this.s3.getBucketName());
        int retry = 0;
        while (true) {
            try {
                File uploadFile = new File(uploadSrc);
                if(this.s3.put(this.objectKey, uploadFile)){
                    LOG.info("S3URL for upload file [" + uploadFile.getName() + "] is : " + s3.getUrl(objectKey));
                    break;
                }else
                    throw new RuntimeException();
            } catch (Exception e) {
                if (retry < downloadRetry) {
                    LOG.warn("Upload output file " + this.objectKey + " to HBox S3 failed, retry in " + (++retry), e);
                } else {
                    LOG.error("Upload output file " + this.objectKey + " to HBox S3 failed after " + downloadRetry + " retry times!", e);
                    break;
                }
            }
        }
    }
}
