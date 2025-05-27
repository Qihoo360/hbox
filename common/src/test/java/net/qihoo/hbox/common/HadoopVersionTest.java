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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class HadoopVersionTest {
    @Test
    public void testParseHadoopVersion() {
        assertTrue(HadoopVersion.hasHaddopVersion());

        // assume the hadoop version at build time is 3.2.4
        assertTrue(HadoopVersion.isHaddopVersionAtLeast(3));
        assertTrue(HadoopVersion.isHaddopVersionAtLeast(3, 2));
        assertTrue(HadoopVersion.isHaddopVersionAtLeast(3, 2, 4));

        assertFalse(HadoopVersion.isHaddopVersionAtLeast(4));
        assertFalse(HadoopVersion.isHaddopVersionAtLeast(3, 3));
        assertFalse(HadoopVersion.isHaddopVersionAtLeast(3, 2, 5));

        assertTrue(HadoopVersion.isHaddopVersionAtLeast(2, 6));
        assertTrue(HadoopVersion.isHaddopVersionAtLeast(2, 7));

        assertTrue(HadoopVersion.SUPPORTS_GPU);
    }
}
