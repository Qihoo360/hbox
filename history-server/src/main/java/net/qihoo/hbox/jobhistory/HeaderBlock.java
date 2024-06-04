package net.qihoo.hbox.jobhistory;

import net.qihoo.hbox.common.AMParams;
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

        html.
                div("#header.ui-widget").
                div("#user").
                __(loggedIn).__().
                div("#logo").
                img("/proxy/" + $(APP_ID) + "/static/hboxWebApp/logo.png").__().
                h1($(TITLE)).__();
    }
}
