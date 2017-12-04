package net.qihoo.xlearning.jobhistory;

import static org.apache.hadoop.yarn.util.StringHelper.join;

import net.qihoo.xlearning.webapp.AMParams;
import net.qihoo.xlearning.webapp.NavBlock;
import org.apache.hadoop.yarn.webapp.SubView;
import org.apache.hadoop.yarn.webapp.view.TwoColumnLayout;

public class HsJobPage extends TwoColumnLayout implements AMParams {

  @Override
  protected void preHead(Page.HTML<_> html) {
    super.preHead(html);
    String jobID = $(APP_ID);
    set(TITLE, jobID.isEmpty() ? "Bad request: missing job ID"
        : join($(APP_TYPE) + " Application ", $(APP_ID)));
  }

  @Override
  protected Class<? extends SubView> nav() {
    return NavBlock.class;
  }

  @Override
  protected Class<? extends SubView> content() {
    if($(APP_TYPE).equals("Tensorflow") || $(APP_TYPE).equals("Mxnet")) {
      return HsJobBlock.class;
    }else{
      return HsSingleJobBlock.class;
    }
  }

}
