package net.qihoo.hbox.AM;

import net.qihoo.hbox.api.ApplicationContext;
import net.qihoo.hbox.container.HboxContainerId;
import org.apache.hadoop.mapred.InputSplit;
import java.util.List;

public interface ApplicationMasterContext extends ApplicationContext {
    List<InputSplit> getStreamInputs(HboxContainerId containerId);
}
