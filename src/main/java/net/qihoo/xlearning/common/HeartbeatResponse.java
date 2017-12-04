package net.qihoo.xlearning.common;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.io.BooleanWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Writable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class HeartbeatResponse implements Writable {

  private BooleanWritable isXLearningTrainCompleted;
  private LongWritable interResultTimeStamp;

  private static final Log LOG = LogFactory.getLog(HeartbeatResponse.class);

  public HeartbeatResponse() {
    isXLearningTrainCompleted = new BooleanWritable(false);
    interResultTimeStamp = new LongWritable(Long.MIN_VALUE);
  }

  public HeartbeatResponse(Boolean isXLearningTrainCompleted, Long timeStamp) {
    this.isXLearningTrainCompleted = new BooleanWritable(isXLearningTrainCompleted);
    this.interResultTimeStamp = new LongWritable(timeStamp);
  }

  public Long getInnerModelTimeStamp() {
    return interResultTimeStamp.get();
  }

  public Boolean getIsXLearningTrainCompleted() {
    return this.isXLearningTrainCompleted.get();
  }

  @Override
  public void write(DataOutput dataOutput) {
    try {
      isXLearningTrainCompleted.write(dataOutput);
      interResultTimeStamp.write(dataOutput);
    } catch (IOException e) {
      LOG.error("containerStatus write error: " + e);
    }
  }

  @Override
  public void readFields(DataInput dataInput) {
    try {
      isXLearningTrainCompleted.readFields(dataInput);
      interResultTimeStamp.readFields(dataInput);
    } catch (IOException e) {
      LOG.error("containerStatus read error:" + e);
    }
  }
}
