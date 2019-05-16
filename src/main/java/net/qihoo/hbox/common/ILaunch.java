package net.qihoo.hbox.common;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public interface ILaunch {
  Process exec(String command, String[] envp, Map<String, String> envs, File dir) throws IOException;
}
