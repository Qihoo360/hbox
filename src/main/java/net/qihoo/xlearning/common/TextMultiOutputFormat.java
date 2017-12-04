package net.qihoo.xlearning.common;

import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileAlreadyExistsException;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.compress.CompressionCodec;
import org.apache.hadoop.io.compress.GzipCodec;
import org.apache.hadoop.mapred.*;
import org.apache.hadoop.util.Progressable;
import org.apache.hadoop.util.ReflectionUtils;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.NumberFormat;

public class TextMultiOutputFormat<K, V> extends TextOutputFormat<K, V> {
  public static final String MR_REDUCE_MAX_FILE_PER_FILE =
      "mapred.reduce.max.size.per.file";
  private static final long SPLIT_SIZE = 512 * 1024 * 1024; //512M

  public class NewDataOutputStream extends DataOutputStream {

    protected long written;

    /**
     * Creates a new data output stream to write data to the specified
     * underlying output stream. The counter <code>written</code> is
     * set to zero.
     *
     * @param out the underlying output stream, to be saved for later
     *            use.
     * @see java.io.FilterOutputStream#out
     */
    public NewDataOutputStream(OutputStream out) {
      super(out);
    }

    private void incCountNew(long value) {
      long temp = written + value;
      if (temp < 0) {
        temp = Long.MAX_VALUE;
      }
      written = temp;
    }

    /**
     * Writes the specified byte (the low eight bits of the argument
     * <code>b</code>) to the underlying output stream. If no exception
     * is thrown, the counter <code>written</code> is incremented by
     * <code>1</code>.
     * <p>
     * Implements the <code>write</code> method of <code>OutputStream</code>.
     *
     * @param b the <code>byte</code> to be written.
     * @throws IOException if an I/O error occurs.
     * @see java.io.FilterOutputStream#out
     */
    public synchronized void write(int b) throws IOException {
      super.write(b);
      incCountNew((long) 1);
    }

    /**
     * Writes <code>len</code> bytes from the specified byte array
     * starting at offset <code>off</code> to the underlying output stream.
     * If no exception is thrown, the counter <code>written</code> is
     * incremented by <code>len</code>.
     *
     * @param b   the data.
     * @param off the start offset in the data.
     * @param len the number of bytes to write.
     * @throws IOException if an I/O error occurs.
     * @see java.io.FilterOutputStream#out
     */
    public synchronized void write(byte b[], int off, int len)
        throws IOException {
      super.write(b, off, len);
      incCountNew((long) len);
    }

    /**
     * Flushes this data output stream. This forces any buffered output
     * bytes to be written out to the stream.
     * <p>
     * The <code>flush</code> method of <code>NewDataOutputStream</code>
     * calls the <code>flush</code> method of its underlying output stream.
     *
     * @throws IOException if an I/O error occurs.
     * @see java.io.FilterOutputStream#out
     * @see java.io.OutputStream#flush()
     */
    public void flush() throws IOException {
      super.flush();
    }

    public long getSize() {
      return written;
    }
  }

  private long splitSize;
  CompressionCodec codec;
  JobConf jobConf;
  Progressable jobProgress;
  String fileName;
  int fileNum = 0;

  public class MultiSplitRecordWriter<K, V>
      implements RecordWriter<K, V> {
    private boolean ignoreSeparatorOnNull;
    protected NewDataOutputStream out;
    private String keyValueSeparator;
    LineRecordWriter<K, V> writer;

    public MultiSplitRecordWriter(NewDataOutputStream out, String keyValueSeparator, boolean ignoreSeparatorOnNull) {
      this.out = out;
      this.keyValueSeparator = keyValueSeparator;
      this.ignoreSeparatorOnNull = ignoreSeparatorOnNull;
      this.writer = new LineRecordWriter<K, V>(out, keyValueSeparator);
    }

    public synchronized void write(K key, V value)
        throws IOException {
      if (splitSize < out.getSize()) {
        //	  writer.flush();
        writer.close(null);
        out = new NewDataOutputStream(codec.createOutputStream(createFile()));
        writer = new LineRecordWriter<K, V>(out, keyValueSeparator);
      }
      writer.write(key, value);
    }

    public synchronized void close(Reporter reporter) throws IOException {
      out.close();
      writer.close(reporter);
    }

  }

  protected static final NumberFormat numFormat = NumberFormat.getInstance();

  static {
    numFormat.setGroupingUsed(false);
    numFormat.setMinimumIntegerDigits(3);
  }

  private static String getFileExtention(int num) {
    return "-" + numFormat.format(num);
  }

  @Override
  public void checkOutputSpecs(FileSystem ignored, JobConf job)
      throws FileAlreadyExistsException,
      InvalidJobConfException, IOException {
    super.checkOutputSpecs(ignored, job);

    if (!getCompressOutput(job)) {
      throw new InvalidJobConfException("TextMultiOutputFormat only for compressed mode.");
    }

  }

  public FSDataOutputStream createFile()
      throws IOException {
    Path file;

    file = FileOutputFormat.getTaskOutputPath(jobConf, fileName + getFileExtention(fileNum++) + codec.getDefaultExtension());
    FileSystem fs = file.getFileSystem(jobConf);

    return fs.create(file, null);
  }

  public RecordWriter<K, V> getRecordWriter(FileSystem ignored,
                                            JobConf job,
                                            String name,
                                            Progressable progress)
      throws IOException {
    boolean ignoreSeparatorOnNull = job.getBoolean("mapred.textoutputformat.ignore.separator", false);
    String keyValueSeparator = job.get("mapred.textoutputformat.separator", "\t");
    splitSize = job.getLong(MR_REDUCE_MAX_FILE_PER_FILE, SPLIT_SIZE);
    jobConf = job;
    fileName = name;
    jobProgress = progress;
    Class<? extends CompressionCodec> codecClass =
        getOutputCompressorClass(job, GzipCodec.class);
    // create the named codec
    codec = ReflectionUtils.newInstance(codecClass, job);
    FSDataOutputStream fileOut = createFile();

    return new MultiSplitRecordWriter<K, V>(new NewDataOutputStream(codec.createOutputStream(fileOut)),
        keyValueSeparator, ignoreSeparatorOnNull);

  }

}

