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

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.hadoop.util.VersionInfo;

public class HadoopVersion {
    private static int HADOOP_VER_MAJOR;
    private static int HADOOP_VER_MINOR;
    private static int HADOOP_VER_PATCH;

    static {
        final String v = VersionInfo.getVersion();
        if (null == v) {
            HADOOP_VER_MAJOR = -1;
            HADOOP_VER_MINOR = -1;
            HADOOP_VER_PATCH = -1;
        } else {
            final Pattern r3 = Pattern.compile("^0*(0|[1-9]\\d*)[.]0*(0|[1-9]\\d*)[.]0*(0|[1-9]\\d*)(\\D|$)");
            final Pattern r2 = Pattern.compile("^0*(0|[1-9]\\d*)[.]0*(0|[1-9]\\d*)(\\D|$)");
            final Pattern r1 = Pattern.compile("^0*(0|[1-9]\\d*)(\\D|$)");
            final Matcher m3 = r3.matcher(v);
            final Matcher m2 = r2.matcher(v);
            final Matcher m1 = r1.matcher(v);
            if (m3.find()) {
                HADOOP_VER_MAJOR = Integer.parseInt(m3.group(1));
                HADOOP_VER_MINOR = Integer.parseInt(m3.group(2));
                HADOOP_VER_PATCH = Integer.parseInt(m3.group(3));
            } else if (m2.find()) {
                HADOOP_VER_MAJOR = Integer.parseInt(m2.group(1));
                HADOOP_VER_MINOR = Integer.parseInt(m2.group(2));
                HADOOP_VER_PATCH = 0;
            } else if (m1.find()) {
                HADOOP_VER_MAJOR = Integer.parseInt(m1.group(1));
                HADOOP_VER_MINOR = 0;
                HADOOP_VER_PATCH = 0;
            } else {
                HADOOP_VER_MAJOR = -1;
                HADOOP_VER_MINOR = -1;
                HADOOP_VER_PATCH = -1;
            }
        }
    }

    public static boolean hasHaddopVersion() {
        return isHaddopVersionAtLeast(0, 0, 0);
    }

    public static boolean isHaddopVersionAtLeast(final int major) {
        return isHaddopVersionAtLeast(major, 0, 0);
    }

    public static boolean isHaddopVersionAtLeast(final int major, final int minor) {
        return isHaddopVersionAtLeast(major, minor, 0);
    }

    public static boolean isHaddopVersionAtLeast(final int major, final int minor, final int patch) {
        if (HADOOP_VER_MAJOR != major) {
            return HADOOP_VER_MAJOR > major;
        } else if (HADOOP_VER_MINOR != minor) {
            return HADOOP_VER_MINOR > minor;
        } else {
            return HADOOP_VER_PATCH >= patch;
        }
    }

    // hadoop 3.1 supports schedule GPU resource natively
    // see https://hadoop.apache.org/docs/r3.1.0/hadoop-yarn/hadoop-yarn-site/UsingGpus.html
    public static boolean SUPPORTS_GPU = isHaddopVersionAtLeast(3, 1, 0);
}
