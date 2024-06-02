package net.qihoo.hbox.api;

import org.apache.hadoop.ipc.VersionedProtocol;
import net.qihoo.hbox.common.Message;

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
