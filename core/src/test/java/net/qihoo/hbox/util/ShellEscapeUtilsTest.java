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
package net.qihoo.hbox.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class ShellEscapeUtilsTest {
    @Test
    public void testEscapePlain() {
        assertEquals("''", ShellEscapeUtils.escapePlain(null));
        assertEquals("''", ShellEscapeUtils.escapePlain(""));
        assertEquals("abc", ShellEscapeUtils.escapePlain("abc"));
        assertEquals("'ab$c'", ShellEscapeUtils.escapePlain("ab$c"));
        assertEquals("'ab'\\''c'", ShellEscapeUtils.escapePlain("ab'c"));
        assertEquals("'ab\nc'", ShellEscapeUtils.escapePlain("ab\nc"));
    }

    @Test
    public void testEscapeInDoubleQuotes() {
        assertEquals("", ShellEscapeUtils.escapeInDoubleQuotes(null));
        assertEquals("", ShellEscapeUtils.escapeInDoubleQuotes(""));
        assertEquals("abc", ShellEscapeUtils.escapeInDoubleQuotes("abc"));
        assertEquals("ab\\$c", ShellEscapeUtils.escapeInDoubleQuotes("ab$c"));
        assertEquals("ab\\\"c", ShellEscapeUtils.escapeInDoubleQuotes("ab\"c"));
        assertEquals("ab'c", ShellEscapeUtils.escapeInDoubleQuotes("ab'c"));
        assertEquals("ab\\\\c", ShellEscapeUtils.escapeInDoubleQuotes("ab\\c"));
        assertEquals("ab\\`c", ShellEscapeUtils.escapeInDoubleQuotes("ab`c"));
        assertEquals("ab\nc", ShellEscapeUtils.escapeInDoubleQuotes("ab\nc"));
    }
}
