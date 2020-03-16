package net.qihoo.xlearning.api;

import net.qihoo.xlearning.security.Utils;
import net.qihoo.xlearning.security.XTokenSelector;
import org.apache.hadoop.ipc.ProtocolInfo;
import org.apache.hadoop.ipc.VersionedProtocol;
import net.qihoo.xlearning.common.Message;
import org.apache.hadoop.security.KerberosInfo;
import org.apache.hadoop.security.token.TokenInfo;

/**
 * The Protocal between clients and ApplicationMaster to fetch Application Messages.
 */

@KerberosInfo(serverPrincipal = Utils.SERVER_PRINCIPAL_KEY)
@TokenInfo(XTokenSelector.class)
@ProtocolInfo(protocolName = "ApplicationMessageProtocol",
        protocolVersion = 1)
public interface ApplicationMessageProtocol extends VersionedProtocol {

  public static final long versionID = 1L;

  /**
   * Fetch application from ApplicationMaster.
   */
  Message[] fetchApplicationMessages();

  Message[] fetchApplicationMessages(int maxBatch);
}
