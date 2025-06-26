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
package net.qihoo.hbox.client;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.ginsberg.junit.exit.ExpectSystemExitWithStatus;
import com.ginsberg.junit.exit.FailOnSystemExit;
import com.github.blindpirate.extensions.CaptureSystemOutput;
import java.io.PrintStream;
import org.apache.commons.io.output.NullOutputStream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class ClientArgumentsTest {

    private static PrintStream origStdout = null;
    private static PrintStream origStderr = null;
    private static final PrintStream nullOut = new PrintStream(NullOutputStream.NULL_OUTPUT_STREAM);

    @BeforeAll
    public static void closeConsole() {
        origStdout = System.out;
        origStderr = System.err;
        System.setOut(nullOut);
        System.setErr(nullOut);
    }

    @AfterAll
    public static void restoreStdout() {
        System.setOut(origStdout);
        System.setErr(origStderr);
    }

    @Test
    @ExpectSystemExitWithStatus(0)
    @CaptureSystemOutput
    public void testEmptyArgs(final CaptureSystemOutput.OutputCapture outputCapture) throws Exception {
        new ClientArguments(new String[0]);
        outputCapture.expect(containsString("usage: Client"));
    }

    @Test
    @ExpectSystemExitWithStatus(0)
    @CaptureSystemOutput
    public void testHelp(final CaptureSystemOutput.OutputCapture outputCapture) throws Exception {
        new ClientArguments(new String[] {"--app-name", "111", "--help"});
        outputCapture.expect(containsString("usage: Client"));
    }

    @Test
    @ExpectSystemExitWithStatus(0)
    @CaptureSystemOutput
    public void testVersion(final CaptureSystemOutput.OutputCapture outputCapture) throws Exception {
        new ClientArguments(new String[] {"--app-name", "111", "--version"});
        outputCapture.expect(containsString("hbox version "));
    }

    @Test
    @ExpectSystemExitWithStatus(1)
    @CaptureSystemOutput
    public void testNoCmd(final CaptureSystemOutput.OutputCapture outputCapture) throws Exception {
        new ClientArguments(new String[] {"--app-name", "111"});
        outputCapture.expect(containsString("No commands to submit"));
        outputCapture.expect(containsString("usage: Client"));
    }

    @Test
    @FailOnSystemExit
    public void testNormalCmd() throws Exception {
        final ClientArguments cli = new ClientArguments(new String[] {"--app-name", "111", "aa", "bb"});
        assertEquals("111", cli.appName);
        assertArrayEquals(new String[] {"aa", "bb"}, cli.hboxCommandArgs);
    }

    @Test
    @FailOnSystemExit
    public void testDoubleDash() throws Exception {
        final ClientArguments cli =
                new ClientArguments(new String[] {"--app-name", "111", "--", "--conf", "--", "aa", "bb"});
        assertEquals("111", cli.appName);
        // the first "--" stops parsing for options, and remove any of the following "--"s
        assertArrayEquals(new String[] {"--conf", "aa", "bb"}, cli.hboxCommandArgs);
    }

    @Test
    @FailOnSystemExit
    public void testConfWithOldcmd() throws Exception {
        final ClientArguments cli = new ClientArguments(new String[] {
            "--hbox-cmd", "aa bb", "--conf", "hbox.test.conf1=", "--conf", "hbox.test.conf2=val2", "--app-name", "111"
        });
        assertEquals("111", cli.appName);
        assertArrayEquals(new String[] {"aa", "bb"}, cli.hboxCommandArgs);
        assertEquals("", cli.confs.getProperty("hbox.test.conf1"));
        assertEquals("val2", cli.confs.getProperty("hbox.test.conf2"));
    }

    @Test
    @FailOnSystemExit
    public void testConf() throws Exception {
        final ClientArguments cli = new ClientArguments(new String[] {
            "--conf", "hbox.test.conf2=val2", "--app-name", "111", "--conf", "hbox.test.conf1", "--", "aa", "bb"
        });
        assertEquals("111", cli.appName);
        assertArrayEquals(new String[] {"aa", "bb"}, cli.hboxCommandArgs);
        assertEquals("true", cli.confs.getProperty("hbox.test.conf1"));
        assertEquals("val2", cli.confs.getProperty("hbox.test.conf2"));
    }

    @Test
    @FailOnSystemExit
    public void testInput() throws Exception {
        final ClientArguments cli = new ClientArguments(
                new String[] {"--input", "/path/AAA#xxx", "--input", "/path/BBB", "--input", "/path/CCC#yyy", "aa", "bb"
                });
        assertArrayEquals(new String[] {"aa", "bb"}, cli.hboxCommandArgs);
        assertEquals("xxx", cli.inputs.getProperty("/path/AAA"));
        assertEquals("true", cli.inputs.getProperty("/path/BBB")); // default property value is "true"
        assertEquals("yyy", cli.inputs.getProperty("/path/CCC"));
    }

    @Test
    @FailOnSystemExit
    public void testS3Input() throws Exception {
        final ClientArguments cli = new ClientArguments(new String[] {
            "--s3input", "/path/AAA#xxx", "--s3input", "/path/BBB##", "--s3input", "/path/CCC#yyy", "aa", "bb"
        });
        assertArrayEquals(new String[] {"aa", "bb"}, cli.hboxCommandArgs);
        assertEquals("xxx", cli.s3Inputs.getProperty("/path/AAA"));
        assertEquals("#", cli.s3Inputs.getProperty("/path/BBB"));
        assertEquals("yyy", cli.s3Inputs.getProperty("/path/CCC"));
    }

    @Test
    @FailOnSystemExit
    public void testOutput() throws Exception {
        final ClientArguments cli = new ClientArguments(new String[] {
            "--output", "/path/AAA#xxx", "--output", "/path/BBB#yyy", "--output", "/path/CCC#zzz", "aa", "bb"
        });
        assertArrayEquals(new String[] {"aa", "bb"}, cli.hboxCommandArgs);
        assertEquals("xxx", cli.outputs.getProperty("/path/AAA"));
        assertEquals("yyy", cli.outputs.getProperty("/path/BBB"));
        assertEquals("zzz", cli.outputs.getProperty("/path/CCC"));
    }

    @Test
    @FailOnSystemExit
    public void testS3Output() throws Exception {
        final ClientArguments cli = new ClientArguments(new String[] {
            "--s3output", "/path/AAA#xxx", "--s3output", "/path/BBB#yyy", "--s3output", "/path/CCC#zzz", "aa", "bb"
        });
        assertArrayEquals(new String[] {"aa", "bb"}, cli.hboxCommandArgs);
        assertEquals("xxx", cli.s3Outputs.getProperty("/path/AAA"));
        assertEquals("yyy", cli.s3Outputs.getProperty("/path/BBB"));
        assertEquals("zzz", cli.s3Outputs.getProperty("/path/CCC"));
    }
}
