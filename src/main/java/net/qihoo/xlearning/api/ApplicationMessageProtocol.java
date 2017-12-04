package net.qihoo.xlearning.api;

import org.apache.hadoop.ipc.VersionedProtocol;
import net.qihoo.xlearning.common.Message;

/**
 * The Protocal between clients and ApplicationMaster to fetch Application Messages.
 */
public interface ApplicationMessageProtocol extends VersionedProtocol {

  public static final long versionID = 1L;

  /**
   * Fetch application from ApplicationMaster.
   */
  Message[] fetchApplicationMessages();

  Message[] fetchApplicationMessages(int maxBatch);
}
