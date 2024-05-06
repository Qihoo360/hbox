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
