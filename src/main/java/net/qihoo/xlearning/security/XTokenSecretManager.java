package net.qihoo.xlearning.security;

import org.apache.hadoop.security.token.SecretManager;

public class XTokenSecretManager extends
        SecretManager<XTokenIdentifier> {
    @Override
    protected byte[] createPassword(XTokenIdentifier xTokenIdentifier) {
        return xTokenIdentifier.getBytes();
    }

    @Override
    public byte[] retrievePassword(XTokenIdentifier xTokenIdentifier) throws InvalidToken {
        return xTokenIdentifier.getBytes();
    }

    @Override
    public XTokenIdentifier createIdentifier() {
        return new XTokenIdentifier();
    }
}
