// Copyright 2017-2025 Qihoo Inc
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package net.qihoo.hbox.webapp;

import java.io.IOException;
import net.qihoo.hbox.api.ApplicationContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.http.HttpServer2;
import org.apache.hadoop.service.AbstractService;
import org.apache.hadoop.yarn.webapp.WebApp;
import org.apache.hadoop.yarn.webapp.WebAppException;
import org.apache.hadoop.yarn.webapp.WebApps;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.webapp.WebAppContext;

public class ApplicationWebService extends AbstractService {
    private static final Log LOG = LogFactory.getLog(ApplicationWebService.class);
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
            webApp = WebApps.$for("proxy", ApplicationContext.class, applicationContext, "ws")
                    .with(getConfig())
                    .build(new AMWebApp());
            HttpServer2 httpServer = webApp.httpServer();

            WebAppContext webAppContext = httpServer.getWebAppContext();
            WebAppContext appWebAppContext = new WebAppContext();
            appWebAppContext.setContextPath("/static/hboxWebApp");
            String appDir =
                    getClass().getClassLoader().getResource("hboxWebApp").toString();
            appWebAppContext.setResourceBase(appDir);
            // appWebAppContext.addServlet(DefaultServlet.class, "/*");
            final String[] ALL_URLS = {"/*"};
            FilterHolder[] filterHolders = webAppContext.getServletHandler().getFilters();
            for (FilterHolder filterHolder : filterHolders) {
                if (!"guice".equals(filterHolder.getName())) {
                    HttpServer2.defineFilter(
                            appWebAppContext,
                            filterHolder.getName(),
                            filterHolder.getClassName(),
                            filterHolder.getInitParameters(),
                            ALL_URLS);
                }
            }
            httpServer.addHandlerAtFront(appWebAppContext);
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

    /* This block prevents the Maven Shade plugin to remove the specified classes */
    static {
        @SuppressWarnings("unused")
        Class<?>[] classes = new Class<?>[] {
            org.eclipse.jetty.servlet.NoJspServlet.class,
            org.eclipse.jetty.servlet.listener.IntrospectorCleaner.class,
            org.eclipse.jetty.servlet.listener.ELContextCleaner.class,
            org.eclipse.jetty.webapp.JettyWebXmlConfiguration.class,
            org.eclipse.jetty.webapp.FragmentConfiguration.class,
            org.eclipse.jetty.webapp.MetaInfConfiguration.class,
            org.eclipse.jetty.webapp.WebXmlConfiguration.class,
            org.eclipse.jetty.webapp.WebInfConfiguration.class
        };
    }
}
