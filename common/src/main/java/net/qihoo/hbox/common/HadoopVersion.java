package net.qihoo.hbox.common;

import org.apache.hadoop.util.VersionInfo;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HadoopVersion {
  static private int HADOOP_VER_MAJOR;
  static private int HADOOP_VER_MINOR;
  static private int HADOOP_VER_PATCH;

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

  static public boolean hasHaddopVersion() {
    return isHaddopVersionAtLeast(0, 0, 0);
  }
  static public boolean isHaddopVersionAtLeast(final int major) {
    return isHaddopVersionAtLeast(major, 0, 0);
  }
  static public boolean isHaddopVersionAtLeast(final int major, final int minor) {
    return isHaddopVersionAtLeast(major, minor, 0);
  }
  static public boolean isHaddopVersionAtLeast(final int major, final int minor, final int patch) {
    if (HADOOP_VER_MAJOR != major) {
      return HADOOP_VER_MAJOR > major;
    } else if (HADOOP_VER_MINOR != minor) {
      return HADOOP_VER_MINOR > minor;
    } else {
      return HADOOP_VER_PATCH >= patch;
    }
  }
}
