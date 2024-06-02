package net.qihoo.hbox.common;

import net.qihoo.hbox.storage.S3File;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;


public class InputInfo implements Writable {

    private String aliasName;

    private String inputType;

    private List<Path> paths = new ArrayList<>();

    private List<S3File> s3Files = new ArrayList<>();

    public InputInfo() {
    }

    public String getAliasName() {
        return aliasName;
    }

    public void setAliasName(String aliasName) {
        this.aliasName = aliasName;
    }

    public String getInputType() {
        return inputType;
    }

    public void setInputType(String inputType) {
        this.inputType = inputType;
    }

    public List<Path> getPaths() {
        return paths;
    }

    public void setPaths(List<Path> paths) {
        this.paths = paths;
    }

    public void addPath(Path path) {
        paths.add(path);
    }

    public List<S3File> getS3Files() {
        return s3Files;
    }

    public void setS3Files(List<S3File> s3Files) {
        this.s3Files = s3Files;
    }

    public void addS3File(S3File file){
        s3Files.add(file);
    }

    @Override
    public void write(DataOutput dataOutput) throws IOException {
        Text.writeString(dataOutput, aliasName);
        Text.writeString(dataOutput, inputType);
        dataOutput.writeInt(paths.size());
        for (Path p : paths) {
            Text.writeString(dataOutput, p.toString());
        }
        dataOutput.writeInt(s3Files.size());
        for (S3File f : s3Files) {
            Text.writeString(dataOutput, f.getBucket());
            Text.writeString(dataOutput, f.getKey());
            Text.writeString(dataOutput, f.getUrl());
        }
    }

    @Override
    public void readFields(DataInput dataInput) throws IOException {
        this.aliasName = Text.readString(dataInput);
        this.inputType = Text.readString(dataInput);
        this.paths = new ArrayList<>();
        int size = dataInput.readInt();
        for (int i = 0; i < size; i++) {
            this.paths.add(new Path(Text.readString(dataInput)));
        }
        this.s3Files = new ArrayList<>();
        int urlSize = dataInput.readInt();
        for (int i = 0; i < urlSize; i++) {
            this.s3Files.add(new S3File(Text.readString(dataInput), Text.readString(dataInput), Text.readString(dataInput)));
        }
    }
}
