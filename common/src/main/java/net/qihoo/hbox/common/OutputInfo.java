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

public class OutputInfo implements Writable {
    private String localLocation;

    private String dfsLocation;

    private String outputType;

    public OutputInfo() {}

    public String getOutputType() {
        return outputType;
    }

    public void setOutputType(String outputType) {
        this.outputType = outputType;
    }

    public String getLocalLocation() {
        return localLocation;
    }

    public void setLocalLocation(String localLocation) {
        this.localLocation = localLocation;
    }

    public String getDfsLocation() {
        return dfsLocation;
    }

    public void setDfsLocation(String dfsLocation) {
        this.dfsLocation = dfsLocation;
    }

    @Override
    public String toString() {
        return dfsLocation;
    }

    @Override
    public void write(DataOutput dataOutput) throws IOException {
        Text.writeString(dataOutput, localLocation);
        Text.writeString(dataOutput, dfsLocation);
        Text.writeString(dataOutput, outputType);
    }

    @Override
    public void readFields(DataInput dataInput) throws IOException {
        this.localLocation = Text.readString(dataInput);
        this.dfsLocation = Text.readString(dataInput);
        this.outputType = Text.readString(dataInput);
    }
}
