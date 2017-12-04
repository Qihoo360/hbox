package net.qihoo.xlearning.webapp;

import org.apache.hadoop.yarn.webapp.SubView;
import org.apache.hadoop.yarn.webapp.hamlet.Hamlet.HTML;
import org.apache.hadoop.yarn.webapp.view.TwoColumnLayout;

import static org.apache.hadoop.yarn.util.StringHelper.join;

public class InfoPage extends TwoColumnLayout implements AMParams {
  @Override
  protected void preHead(HTML<_> html) {
    super.preHead(html);
    setTitle(join($(APP_TYPE) + " Application ", $(APP_ID)));
  }

  @Override
  protected Class<? extends SubView> content() {
    if ($(APP_TYPE).equals("Tensorflow") || $(APP_TYPE).equals("Mxnet")) {
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
