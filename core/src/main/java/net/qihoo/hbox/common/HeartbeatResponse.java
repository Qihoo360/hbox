// Copyright 2017-2025 Qihoo Inc
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package net.qihoo.hbox.common;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.io.BooleanWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Writable;

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
