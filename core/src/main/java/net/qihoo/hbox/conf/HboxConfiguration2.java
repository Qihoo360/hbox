package net.qihoo.hbox.conf;

import org.apache.hadoop.mapred.InputFormat;
import org.apache.hadoop.mapred.OutputFormat;

// some configuration keys and default values only for core
public class HboxConfiguration2 {
    public static final String HBOX_INPUTF0RMAT_CLASS = "hbox.inputformat.class";
    public static final Class<? extends InputFormat> DEFAULT_HBOX_INPUTF0RMAT_CLASS = org.apache.hadoop.mapred.TextInputFormat.class;

    public static final String HBOX_OUTPUTFORMAT_CLASS = "hbox.outputformat.class";
    public static final Class<? extends OutputFormat> DEFAULT_HBOX_OUTPUTF0RMAT_CLASS = org.apache.hadoop.mapred.TextOutputFormat.class;
}
