package net.qihoo.hbox.api;

import java.io.File;
import java.io.InputStream;

public interface Storage {
    boolean put(String name, File file);
    InputStream get(String fileName);
}
