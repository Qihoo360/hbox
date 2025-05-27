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
package net.qihoo.hbox.common;

import java.io.IOException;
import net.qihoo.hbox.conf.HboxConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

/**
 * Created by jiarunying-it on 2019/2/20.
 */
public class UploadTask implements Runnable {

    private static final Log LOG = LogFactory.getLog(UploadTask.class);

    private Configuration conf;

    private final Path uploadDst;

    private final Path uploadSrc;

    private final int downloadRetry;

    public UploadTask(Configuration conf, Path uploadDst, Path uploadSrc) throws IOException {
        this.conf = conf;
        this.uploadDst = uploadDst;
        this.uploadSrc = uploadSrc;
        this.downloadRetry = conf.getInt(
                HboxConfiguration.HBOX_DOWNLOAD_FILE_RETRY, HboxConfiguration.DEFAULT_HBOX_DOWNLOAD_FILE_RETRY);
    }

    @Override
    public void run() {
        LOG.info("Upload output file from " + this.uploadSrc + " to " + this.uploadDst);
        int retry = 0;
        while (true) {
            try {
                FileSystem dfs = uploadDst.getFileSystem(conf);
                if (dfs.exists(uploadDst)) {
                    LOG.info("Container remote output path " + uploadDst + " exists, so we has to delete is first.");
                    dfs.delete(uploadDst);
                }
                dfs.copyFromLocalFile(false, false, uploadSrc, uploadDst);
                LOG.info("Upload output file from " + this.uploadSrc + " to " + this.uploadDst + " successful.");
                dfs.close();
                break;
            } catch (Exception e) {
                if (retry < downloadRetry) {
                    LOG.warn(
                            "Upload output file from " + this.uploadSrc + " to " + this.uploadDst + " failed, retry in "
                                    + (++retry),
                            e);
                } else {
                    LOG.error(
                            "Upload output file from " + this.uploadSrc + " to " + this.uploadDst + " failed after "
                                    + downloadRetry + " retry times!",
                            e);
                    break;
                }
            }
        }
    }
}
