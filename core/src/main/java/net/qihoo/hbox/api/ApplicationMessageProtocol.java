package net.qihoo.hbox.api;

import net.qihoo.hbox.common.Message;
import org.apache.hadoop.ipc.VersionedProtocol;

/**
 * The Protocal between clients and ApplicationMaster to fetch Application Messages.
 */
public interface ApplicationMessageProtocol extends VersionedProtocol {

    public static final long versionID = 1L;
    public static final int DEFAULT_BATCH = 200;

    /*
     * Fetch application from ApplicationMaster.
     */
    Message[] fetchApplicationMessages();

    Message[] fetchApplicationMessages(int maxBatch);
}
