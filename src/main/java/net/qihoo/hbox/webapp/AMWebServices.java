package net.qihoo.hbox.webapp;

import com.google.inject.Inject;
import net.qihoo.hbox.api.ApplicationContext;
import net.qihoo.hbox.common.LogType;
import net.qihoo.hbox.container.HboxContainerId;
import net.qihoo.hbox.webapp.dao.AppInfo;
import net.qihoo.hbox.webapp.dao.ContainerInfo;
import net.qihoo.hbox.webapp.dao.ContainersInfo;
import net.qihoo.hbox.webapp.dao.OutputInfo;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.yarn.api.records.Container;
import org.apache.hadoop.yarn.util.ConverterUtils;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

/**
 * Created by jiarunying-it on 2018/8/28.
 */

@Path("/ws")
public class AMWebServices {
  private final ApplicationContext appCtx;
  private final App app;
  private final Configuration conf;

  private
  @Context
  HttpServletResponse response;

  @Inject
  public AMWebServices(final App app, final ApplicationContext context, final Configuration conf) {
    this.appCtx = context;
    this.app = app;
    this.conf = conf;
  }

  private void init() {
    //clear content type
    response.setContentType(null);
  }

  @GET
  @Path("/app")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public AppInfo getAppInfo() {
    init();
    return new AppInfo(this.app, this.appCtx);
  }

  @GET
  @Path("/containers")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public ContainersInfo getContainersInfo() {
    init();
    ContainersInfo containersInfo = new ContainersInfo();
    containersInfo.add(new ContainerInfo(new HboxContainerId(ConverterUtils.toContainerId(appCtx.getAMContainerID())), appCtx));
    for (Container c : appCtx.getPsContainers()) {
      containersInfo.add(new ContainerInfo(new HboxContainerId(c.getId()), appCtx));
    }
    for (Container c : appCtx.getWorkerContainers()) {
      containersInfo.add(new ContainerInfo(new HboxContainerId(c.getId()), appCtx));
    }
    return containersInfo;
  }

  @GET
  @Path("/containers/{containerid}")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public ContainerInfo getContainerInfo(@PathParam("containerid") String cid) {
    init();
    return new ContainerInfo(new HboxContainerId(ConverterUtils.toContainerId(cid)), appCtx);
  }

  @GET
  @Path("/containers/{containerid}/{logType}")
  @Produces(MediaType.TEXT_PLAIN)
  public String getContainerLog(@PathParam("containerid") String cid, @PathParam("logType") String logType) {
    init();
    if (appCtx.getContainerStarted()) {
      if (logType.toUpperCase().equals(LogType.STDOUT.toString()))
        return appCtx.getContainerStdOut(new HboxContainerId(ConverterUtils.toContainerId(cid)));
      if (logType.toUpperCase().equals(LogType.STDERR.toString()))
        return appCtx.getContainerStdErr(new HboxContainerId(ConverterUtils.toContainerId(cid)));
    }
    return "";
  }

  @GET
  @Path("/app/savemodel")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public OutputInfo saveModel() {
    if (appCtx.getOutputs().size() > 0 && appCtx.getContainerStarted()) {
      Boolean startSaving = appCtx.getStartSavingStatus();
      if (!startSaving) {
        appCtx.startSavingModelStatus(true);
      }
      while (appCtx.getStartSavingStatus()) {
        if (appCtx.getLastSavingStatus()) {
          app.context.startSavingModelStatus(false);
          break;
        }
      }
      return new OutputInfo(appCtx, appCtx.getLastInterSavingPath());
    }
    return new OutputInfo();
  }

  @GET
  @Path("/app/signal/{sid}")
  @Produces(MediaType.TEXT_PLAIN)
  public String sendSignal(@PathParam("sid") String sid) {
    try {
      int sID = Integer.parseInt(sid);
      appCtx.sendSignal(sID);
    } catch (Exception e) {
      return "FAILED";
    }
    return "SUCCEED";
  }

}
