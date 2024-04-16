package net.qihoo.hbox.common;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import org.junit.jupiter.api.Test;

public class HadoopVersionTest {
  @Test
  public void testParseHadoopVersion() {
    assertTrue(HadoopVersion.hasHaddopVersion());

    // assume the hadoop version at build time is 3.2.1
    assertTrue(HadoopVersion.isHaddopVersionAtLeast(3));
    assertTrue(HadoopVersion.isHaddopVersionAtLeast(3, 2));
    assertTrue(HadoopVersion.isHaddopVersionAtLeast(3, 2, 1));

    assertFalse(HadoopVersion.isHaddopVersionAtLeast(4));
    assertFalse(HadoopVersion.isHaddopVersionAtLeast(3, 3));
    assertFalse(HadoopVersion.isHaddopVersionAtLeast(3, 2, 2));

    assertTrue(HadoopVersion.isHaddopVersionAtLeast(2, 6));
    assertTrue(HadoopVersion.isHaddopVersionAtLeast(2, 7));

    assertTrue(HadoopVersion.SUPPORTS_GPU);
  }
}
