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
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.io.WritableUtils;

public class Message implements Writable {
    private LogType logType;
    private String message;

    public Message() {
        this.logType = LogType.STDERR;
        this.message = "";
    }

    public Message(LogType logType, String message) {
        this.logType = logType;
        this.message = message;
    }

    public LogType getLogType() {
        return logType;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public void write(DataOutput dataOutput) throws IOException {
        WritableUtils.writeEnum(dataOutput, this.logType);
        Text.writeString(dataOutput, message);
    }

    @Override
    public void readFields(DataInput dataInput) throws IOException {
        this.logType = WritableUtils.readEnum(dataInput, LogType.class);
        this.message = Text.readString(dataInput);
    }
}
