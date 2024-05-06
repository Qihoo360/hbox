package net.qihoo.hbox.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.text.translate.CharSequenceTranslator;
import org.apache.commons.text.translate.LookupTranslator;

import static org.apache.commons.text.StringEscapeUtils.escapeXSI;

public final class ShellEscapeUtils {
    private static Log LOG = LogFactory.getLog(ShellEscapeUtils.class);

    public static final CharSequenceTranslator ESCAPE_DQUOTE;
    static {
        final Map<CharSequence, CharSequence> escapeMap = new HashMap<>();
        escapeMap.put("\"", "\\\"");
        escapeMap.put("$", "\\$");
        escapeMap.put("`", "\\`");
        escapeMap.put("\\", "\\\\");

        ESCAPE_DQUOTE = new LookupTranslator(Collections.unmodifiableMap(escapeMap));
    }

    /** Escape a plain text to be paresed directly by posix/bash shell. The result should not put
     * directly between double-quotes. Then new-line sequence will be kept, this action is different
     * with StringEscapeUtils.escapeXSI.
     *
     * Example:
     *   abc ---> abc
     *   ab$c ---> 'ab$c'
     *   ab'c ---> 'ab'\''c'
     *
     * ref:
     *  - https://pubs.opengroup.org/onlinepubs/7908799/xcu/chap2.html
     *  - https://github.com/apache/commons-text/blob/rel/commons-text-1.12.0/src/main/java/org/apache/commons/text/StringEscapeUtils.java#L346
     *  - https://www.etalabs.net/sh_tricks.html
     */
    public static String escapePlain(final String input) {
        if (null == input || 0 == input.length()) {
            return "''";
        }

        if (input.equals(escapeXSI(input))) {
            return input;
        }

        // single-quote escape, this retains the new line chars
        return "'" + input.replaceAll("'", "'\\\\''") + "'";
    }

    /** Escape a plain text to be safely put between double quotes of a posix/bash shell.
     *
     * Example:
     *   abc ---> abc
     *   ab$c ---> ab\$c
     *   ab"c ---> ab\"c
     *   ab\c ---> ab\\c
     *
     * ref: https://pubs.opengroup.org/onlinepubs/7908799/xcu/chap2.html
     */
    public static String escapeInDoubleQuotes(final String input) {
        if (null == input) {
            return "";
        }

        return ESCAPE_DQUOTE.translate(input);
    }

    /** Escape an argument to be put in launch_container.sh, and execute by bash -c "...arg...".
     */
    public static String escapeContainerLaunch(final String arg) {
        final String parsedBy_bash_c = escapePlain(arg);
        final String parsedBy_launch_container_sh = escapeInDoubleQuotes(parsedBy_bash_c);
        return parsedBy_launch_container_sh;
    }
}
