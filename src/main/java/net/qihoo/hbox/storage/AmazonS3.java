package net.qihoo.hbox.storage;

import java.io.File;
import java.io.InputStream;
import java.util.List;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.S3ClientOptions;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import net.qihoo.hbox.api.Storage;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class AmazonS3 implements Storage {
    private static final Log LOG = LogFactory.getLog(AmazonS3.class);
    private AmazonS3Client s3;
    private String bucketName;

    public AmazonS3(String bucketName, String cluster) {
        AWSCredentialsProvider customProvider = new CustomCredentialProvider();
        S3ClientOptions clientOptions = S3ClientOptions.builder()
                .setPathStyleAccess(true)
                .disableChunkedEncoding()
                .build();
        this.s3 = new AmazonS3Client(customProvider);
        this.s3.setEndpoint(cluster);
        this.s3.setS3ClientOptions(clientOptions);
        this.bucketName = bucketName;
        if (!doesBucketExist()) {
            if (createBucket(bucketName))
                LOG.info("Bucket is not exist! Create new bucket: " + bucketName);
        }
    }

    @Override
    public boolean put(File file) {
        return putObject(file.getName(), file);
    }

    @Override
    public InputStream get(String fileName) {
        if (doesObjectExist(fileName)) {
            return getObject(fileName);
        } else
            return null;
    }

    private boolean createBucket(String bucketName) {
        boolean success = false;
        try {
            s3.createBucket(bucketName);
            success = true;
        } catch (AmazonServiceException ase) {
            LOG.info("Caught an AmazonServiceException!" + "Error Message:    " + ase.getMessage());
        } catch (AmazonClientException ace) {
            LOG.info("Caught an AmazonClientException!" + "Error Message: " + ace.getMessage());
        }
        return success;
    }

    private List<Bucket> listBuckets() {
        List<Bucket> list = null;
        try {
            list = s3.listBuckets();
        } catch (AmazonServiceException ase) {
            LOG.info("Caught an AmazonServiceException!" + "Error Message:    " + ase.getMessage());
        } catch (AmazonClientException ace) {
            LOG.info("Caught an AmazonClientException!" + "Error Message: " + ace.getMessage());
        }
        return list;
    }

    private boolean putObject(String key, File file) {
        boolean success = false;
        try {
            s3.putObject(new PutObjectRequest(bucketName, key, file));
            success = true;
        } catch (AmazonServiceException ase) {
            LOG.info("Caught an AmazonServiceException!" + "Error Message:    " + ase.getMessage());
        } catch (AmazonClientException ace) {
            LOG.info("Caught an AmazonClientException!" + "Error Message: " + ace.getMessage());
        }
        return success;
    }

    private InputStream getObject(String key) {
        InputStream is = null;
        try {
            is = s3.getObject(new GetObjectRequest(bucketName, key)).getObjectContent();
        } catch (AmazonServiceException ase) {
            LOG.info("Caught an AmazonServiceException!" + "Error Message:    " + ase.getMessage());
        } catch (AmazonClientException ace) {
            LOG.info("Caught an AmazonClientException!" + "Error Message: " + ace.getMessage());
        }
        return is;
    }

    private List<S3ObjectSummary> listObjects() {
        List<S3ObjectSummary> list = null;
        try {
            ObjectListing objectListing = s3.listObjects(new ListObjectsRequest().withBucketName(bucketName));
            list = objectListing.getObjectSummaries();
        } catch (AmazonServiceException ase) {
            LOG.info("Caught an AmazonServiceException!" + "Error Message:    " + ase.getMessage());
        } catch (AmazonClientException ace) {
            LOG.info("Caught an AmazonClientException!" + "Error Message: " + ace.getMessage());
        }
        return list;
    }

    private boolean deleteObject(String key) {
        boolean success = false;
        try {
            s3.deleteObject(bucketName, key);
            success = true;
        } catch (AmazonServiceException ase) {
            LOG.info("Caught an AmazonServiceException!" + "Error Message:    " + ase.getMessage());
        } catch (AmazonClientException ace) {
            LOG.info("Caught an AmazonClientException!" + "Error Message: " + ace.getMessage());
        }
        return success;
    }

    private boolean deleteBucket() {
        boolean success = false;
        try {
            s3.deleteBucket(bucketName);
            success = true;
        } catch (AmazonServiceException ase) {
            LOG.info("Caught an AmazonServiceException!" + "Error Message:    " + ase.getMessage());
        } catch (AmazonClientException ace) {
            LOG.info("Caught an AmazonClientException!" + "Error Message: " + ace.getMessage());
        }
        return success;
    }

    private boolean doesObjectExist(String key) {
        return s3.doesObjectExist(bucketName, key);
    }

    private boolean doesBucketExist() {
        return s3.doesBucketExist(bucketName);
    }

    public String getUrl(String key) {
        return s3.getUrl(bucketName, key).toString();
    }
}

class CustomCredentials implements AWSCredentials {
    @Override
    public String getAWSAccessKeyId() {
        return "S40a9fCsF1U0suRVJeVG";
    }

    @Override
    public String getAWSSecretKey() {
        return "AJrGuPwvs9bUcvbBFDTrYUyPfDqnvQekh76NRNBC";
    }
}

class CustomCredentialProvider implements AWSCredentialsProvider {

    @Override
    public AWSCredentials getCredentials() {
        return new CustomCredentials();
    }

    @Override
    public void refresh() {
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }
}