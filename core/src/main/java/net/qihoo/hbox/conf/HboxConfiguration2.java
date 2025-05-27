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
package net.qihoo.hbox.conf;

import org.apache.hadoop.mapred.InputFormat;
import org.apache.hadoop.mapred.OutputFormat;

// some configuration keys and default values only for core
public class HboxConfiguration2 {
    public static final String HBOX_INPUTF0RMAT_CLASS = "hbox.inputformat.class";
    public static final Class<? extends InputFormat> DEFAULT_HBOX_INPUTF0RMAT_CLASS =
            org.apache.hadoop.mapred.TextInputFormat.class;

    public static final String HBOX_OUTPUTFORMAT_CLASS = "hbox.outputformat.class";
    public static final Class<? extends OutputFormat> DEFAULT_HBOX_OUTPUTF0RMAT_CLASS =
            org.apache.hadoop.mapred.TextOutputFormat.class;
}
