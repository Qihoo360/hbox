package net.qihoo.hbox.webapp;

import org.apache.hadoop.yarn.webapp.SubView;
import org.apache.hadoop.yarn.webapp.hamlet2.Hamlet.HTML;
import org.apache.hadoop.yarn.webapp.view.TwoColumnLayout;

import static org.apache.hadoop.yarn.util.StringHelper.join;

public class InfoPage extends TwoColumnLayout implements AMParams {
    @Override
    protected void preHead(HTML<__> html) {
        super.preHead(html);
        setTitle(join($(APP_TYPE) + " Application ", $(APP_ID)));
    }

    @Override
    protected Class<? extends SubView> content() {
        if ($(APP_TYPE).equals("Tensorflow") || $(APP_TYPE).equals("Mxnet") || $(APP_TYPE).equals("Distlightlda") || $(APP_TYPE).equals("Xflow") || $(APP_TYPE).equals("Xdl")) {
            return InfoBlock.class;
        } else {
            return SingleInfoBlock.class;
        }
    }

    @Override
    protected Class<? extends SubView> nav() {
        return NavBlock.class;
    }

}
