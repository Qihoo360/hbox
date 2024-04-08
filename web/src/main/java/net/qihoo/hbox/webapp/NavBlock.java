package net.qihoo.hbox.webapp;

import org.apache.hadoop.yarn.webapp.view.HtmlBlock;

public class NavBlock extends HtmlBlock {

    @Override
    protected void render(Block html) {
        html.
                div("#nav").
                h3("Tools").
                ul().
                li().a("/conf", "Configuration").__().
                li().a("/stacks", "Thread dump").__().
                li().a("/logs", "Logs").__().
                li().a("/jmx?qry=Hadoop:*", "Metrics").__().__().__();
    }
}
