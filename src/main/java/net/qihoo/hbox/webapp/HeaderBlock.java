package net.qihoo.hbox.webapp;

import org.apache.hadoop.yarn.webapp.hamlet.Hamlet.DIV;
import org.apache.hadoop.yarn.webapp.view.HtmlBlock;

public class HeaderBlock extends HtmlBlock implements AMParams {
    public HeaderBlock() {
    }

    protected void render(Block html) {
        String loggedIn = "";
        if (this.request().getRemoteUser() != null) {
            loggedIn = "Logged in as: " + this.request().getRemoteUser();
        }

        ((DIV) ((DIV) html.div("#header.ui-widget").div("#user")._(new Object[]{loggedIn})._()).div("#logo").img("/proxy/" + $(APP_ID) + "/static/xlWebApp/logo.png")._()).h1(this.$("title"))._();
    }
}
