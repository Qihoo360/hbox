package net.qihoo.hbox.common;

import org.apache.hadoop.io.BooleanWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.io.WritableUtils;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class HeartbeatRequest implements Writable {
    private HboxContainerStatus hboxContainerStatus;
    private BooleanWritable interResultSavedStatus;
    private String progressLog;
    private String containersStartTime;
    private String containersFinishTime;
    private StringBuilder containerStdOut;
    private StringBuilder containerStdErr;

    public HeartbeatRequest() {
        hboxContainerStatus = HboxContainerStatus.UNDEFINED;
        interResultSavedStatus = new BooleanWritable(false);
        progressLog = "";
        containersStartTime = "";
        containersFinishTime = "";
        containerStdOut = new StringBuilder("");
        containerStdErr = new StringBuilder("");
    }

    public void setHboxContainerStatus(HboxContainerStatus hboxContainerStatus) {
        this.hboxContainerStatus = hboxContainerStatus;
    }

    public HboxContainerStatus getHboxContainerStatus() {
        return this.hboxContainerStatus;
    }

    public void setInnerModelSavedStatus(Boolean savedStatus) {
        this.interResultSavedStatus.set(savedStatus);
    }

    public Boolean getInnerModelSavedStatus() {
        return this.interResultSavedStatus.get();
    }

    public void setProgressLog(String hboxProgress) {
        this.progressLog = hboxProgress;
    }

    public String getProgressLog() {
        return this.progressLog;
    }

    public void setContainersStartTime(String startTime) {
        this.containersStartTime = startTime;
    }

    public String getContainersStartTime() {
        return this.containersStartTime;
    }

    public void setContainersFinishTime(String finishTime) {
        this.containersFinishTime = finishTime;
    }

    public String getContainersFinishTime() {
        return this.containersFinishTime;
    }

    public void appendContainerStdOut(String strOut) {
        this.containerStdOut.append(strOut).append(System.getProperty("line.separator"));
    }

    public String getContainerStdOut() {
        return this.containerStdOut.toString();
    }

    public void clearContainerStdOut() {
        this.containerStdOut.setLength(0);
    }

    public void appendContainerStdErr(String strErr) {
        this.containerStdErr.append(strErr).append(System.getProperty("line.separator"));
    }

    public String getContainerStdErr() {
        return this.containerStdErr.toString();
    }

    public void clearContainerStdErr() {
        this.containerStdErr.setLength(0);
    }

    @Override
    public void write(DataOutput dataOutput) throws IOException {
        WritableUtils.writeEnum(dataOutput, this.hboxContainerStatus);
        interResultSavedStatus.write(dataOutput);
        Text.writeString(dataOutput, this.progressLog);
        Text.writeString(dataOutput, this.containersStartTime);
        Text.writeString(dataOutput, this.containersFinishTime);
        Text.writeString(dataOutput, this.containerStdOut.toString());
        Text.writeString(dataOutput, this.containerStdErr.toString());
    }

    @Override
    public void readFields(DataInput dataInput) throws IOException {
        this.hboxContainerStatus = WritableUtils.readEnum(dataInput, HboxContainerStatus.class);
        interResultSavedStatus.readFields(dataInput);
        this.progressLog = Text.readString(dataInput);
        this.containersStartTime = Text.readString(dataInput);
        this.containersFinishTime = Text.readString(dataInput);
        this.containerStdOut.append(Text.readString(dataInput));
        this.containerStdErr.append(Text.readString(dataInput));
    }

}
