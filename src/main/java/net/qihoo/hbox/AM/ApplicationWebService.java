package net.qihoo.hbox.AM;


import net.qihoo.hbox.api.ApplicationContext;
import net.qihoo.hbox.webapp.AMWebApp;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.http.HttpServer2;
import org.apache.hadoop.service.AbstractService;
import org.apache.hadoop.yarn.webapp.WebApp;
import org.apache.hadoop.yarn.webapp.WebAppException;
import org.apache.hadoop.yarn.webapp.WebApps;
import org.mortbay.jetty.servlet.DefaultServlet;
import org.mortbay.jetty.servlet.FilterHolder;
import org.mortbay.jetty.webapp.WebAppContext;

import java.io.IOException;

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
            webApp = WebApps.$for("proxy", ApplicationContext.class, applicationContext, "ws").with(getConfig()).build(new AMWebApp());
            HttpServer2 httpServer = webApp.httpServer();

            WebAppContext webAppContext = httpServer.getWebAppContext();
            WebAppContext appWebAppContext = new WebAppContext();
            appWebAppContext.setContextPath("/appResource");
            String appDir = getClass().getClassLoader().getResource("hboxWeb").toString();
            appWebAppContext.setResourceBase(appDir + "/static");
            appWebAppContext.addServlet(DefaultServlet.class, "/*");
            final String[] ALL_URLS = {"/*"};
            FilterHolder[] filterHolders =
                    webAppContext.getServletHandler().getFilters();
            for (FilterHolder filterHolder : filterHolders) {
                if (!"guice".equals(filterHolder.getName())) {
                    HttpServer2.defineFilter(appWebAppContext, filterHolder.getName(),
                            filterHolder.getClassName(), filterHolder.getInitParameters(),
                            ALL_URLS);
                }
            }
            httpServer.addContext(appWebAppContext, true);
            try {
                httpServer.start();
                LOG.info("Web app " + webApp.name() + " started at "
                        + httpServer.getConnectorAddress(0).getPort());
            } catch (IOException e) {
                throw new WebAppException("Error starting http server", e);
            }
        } catch (Exception e) {
            LOG.error("Error starting application web server!", e);
        }
    }

    public int getHttpPort() {
        return webApp.port();
    }
}
