package net.qihoo.hbox.AM;

import java.util.List;
import net.qihoo.hbox.api.ApplicationContext;
import net.qihoo.hbox.container.HboxContainerId;
import org.apache.hadoop.mapred.InputSplit;

public interface ApplicationMasterContext extends ApplicationContext {
    List<InputSplit> getStreamInputs(HboxContainerId containerId);
}
