package net.qihoo.xlearning.AM;

import net.qihoo.xlearning.api.ApplicationContext;
import net.qihoo.xlearning.webapp.AMWebApp;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.service.AbstractService;
import org.apache.hadoop.yarn.webapp.WebApp;
import org.apache.hadoop.yarn.webapp.WebApps;

public class ApplicationWebService extends AbstractService {
  private static final Log LOG = LogFactory.getLog(ApplicationContainerListener.class);
  private WebApp webApp;
  private final ApplicationContext applicationContext;

  public ApplicationWebService(ApplicationContext applicationContext, Configuration conf) {
    super(ApplicationWebService.class.getSimpleName());
    this.setConfig(conf);
    this.applicationContext = applicationContext;
  }

  @Override
  public void start() {
    LOG.info("Starting application web server");
    try {
      webApp = WebApps.$for("proxy", ApplicationContext.class, applicationContext, null)
          .with(getConfig()).start(new AMWebApp());
    } catch (Exception e) {
      LOG.error("Error starting application web server!", e);
    }
  }

  public int getHttpPort() {
    return webApp.port();
  }
}
