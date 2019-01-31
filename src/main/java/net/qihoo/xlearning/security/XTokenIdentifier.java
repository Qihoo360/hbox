package net.qihoo.xlearning.security;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.hadoop.security.token.TokenIdentifier;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class XTokenIdentifier extends TokenIdentifier {
    private Text tokenid;
    private Text realUser;
    final static Text KIND_NAME = new Text("xlearning.token");


    public XTokenIdentifier(){
        this(new Text(),new Text());
    }

    public XTokenIdentifier(Text tokenid){
        this(tokenid,new Text());
    }

    public  XTokenIdentifier(Text tokenid, Text realUser){
        this.tokenid = tokenid == null ? new Text() : tokenid;
        this.realUser = realUser == null ? new Text() : realUser;
    }


    @Override
    public Text getKind() {
        return KIND_NAME;
    }

    @Override
    public UserGroupInformation getUser() {
        if (realUser.toString().isEmpty()) {
            return UserGroupInformation.createRemoteUser(tokenid.toString());
        } else {
            UserGroupInformation realUgi = UserGroupInformation
                    .createRemoteUser(realUser.toString());
            return UserGroupInformation
                    .createProxyUser(tokenid.toString(), realUgi);
        }
    }

    @Override
    public void write(DataOutput dataOutput) throws IOException {
        tokenid.write(dataOutput);
        realUser.write(dataOutput);
    }

    @Override
    public void readFields(DataInput dataInput) throws IOException {
        tokenid.readFields(dataInput);
        realUser.readFields(dataInput);
    }
}
