package net.qihoo.xlearning.webapp;

import org.apache.hadoop.yarn.webapp.view.HtmlBlock;

public class NavBlock extends HtmlBlock {

  @Override
  protected void render(Block html) {
    html.
        div("#nav").
        h3("Tools").
        ul().
        li().a("/conf", "Configuration")._().
        li().a("/stacks", "Thread dump")._().
        li().a("/logs", "Logs")._().
        li().a("/jmx?qry=Hadoop:*", "Metrics")._()._()._();
  }
}
