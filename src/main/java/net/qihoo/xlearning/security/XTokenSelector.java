package net.qihoo.xlearning.security;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.security.token.Token;
import org.apache.hadoop.security.token.TokenIdentifier;
import org.apache.hadoop.security.token.TokenSelector;

import java.util.Collection;

public class XTokenSelector implements
        TokenSelector<XTokenIdentifier> {

    @Override
    public Token<XTokenIdentifier> selectToken(Text service,
                                               Collection<Token<? extends TokenIdentifier>> tokens) {

        if (service == null) {
            return null;
        }
        for (Token<? extends TokenIdentifier> token : tokens) {
            if (XTokenIdentifier.KIND_NAME.equals(token.getKind())
                    && service.equals(token.getService())) {
                return (Token<XTokenIdentifier>) token;
            }
        }
        return null;
    }
}
