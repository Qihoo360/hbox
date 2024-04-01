package net.qihoo.hbox.common;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.io.BooleanWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Writable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class HeartbeatResponse implements Writable {

    private BooleanWritable isHboxTrainCompleted;
    private LongWritable interResultTimeStamp;

    private static final Log LOG = LogFactory.getLog(HeartbeatResponse.class);

    public HeartbeatResponse() {
        isHboxTrainCompleted = new BooleanWritable(false);
        interResultTimeStamp = new LongWritable(Long.MIN_VALUE);
    }

    public HeartbeatResponse(Boolean isHboxTrainCompleted, Long timeStamp) {
        this.isHboxTrainCompleted = new BooleanWritable(isHboxTrainCompleted);
        this.interResultTimeStamp = new LongWritable(timeStamp);
    }

    public Long getInnerModelTimeStamp() {
        return interResultTimeStamp.get();
    }

    public Boolean getIsHboxTrainCompleted() {
        return this.isHboxTrainCompleted.get();
    }

    @Override
    public void write(DataOutput dataOutput) {
        try {
            isHboxTrainCompleted.write(dataOutput);
            interResultTimeStamp.write(dataOutput);
        } catch (IOException e) {
            LOG.info("containerStatus write error: " + e);
        }
    }

    @Override
    public void readFields(DataInput dataInput) {
        try {
            isHboxTrainCompleted.readFields(dataInput);
            interResultTimeStamp.readFields(dataInput);
        } catch (IOException e) {
            LOG.info("containerStatus read error:" + e);
        }
    }
}
