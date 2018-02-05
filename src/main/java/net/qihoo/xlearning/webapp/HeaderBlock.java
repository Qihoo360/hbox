package net.qihoo.xlearning.webapp;

import org.apache.hadoop.yarn.webapp.hamlet2.Hamlet.DIV;
import org.apache.hadoop.yarn.webapp.view.HtmlBlock;

public class HeaderBlock extends HtmlBlock implements AMParams {
  public HeaderBlock() {
  }

  protected void render(Block html) {
    String loggedIn = "";
    if (this.request().getRemoteUser() != null) {
      loggedIn = "Logged in as: " + this.request().getRemoteUser();
    }

    ((DIV) ((DIV) html.div("#header.ui-widget").div("#user").__(new Object[]{loggedIn}).__()).div("#logo").img("/proxy/" + $(APP_ID) + "/static/xlWebApp/logo.png").__()).h1(this.$("title")).__();
  }
}
