package net.qihoo.hbox.storage;

public class S3File {
    private String bucket;
    private String key;
    private String url;

    public S3File(String bucket, String key, String url){
        this.bucket = bucket;
        this.key = key;
        this.url = url;
    }

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

}
