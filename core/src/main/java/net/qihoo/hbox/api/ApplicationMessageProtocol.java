// Copyright 2017-2025 Qihoo Inc
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
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
