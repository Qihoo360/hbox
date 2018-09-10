package net.qihoo.hbox.webapp.dao;

import net.qihoo.hbox.api.ApplicationContext;
import net.qihoo.hbox.container.HboxContainerId;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Created by jiarunying-it on 2018/8/29.
 */
@XmlRootElement(name = "containerInfo")
@XmlAccessorType(XmlAccessType.FIELD)
public class ContainerInfo {
  protected String cid;
  protected String startTime;
  protected String status;
  protected String gpu;

  public ContainerInfo() {

  }

  public ContainerInfo(HboxContainerId cid, ApplicationContext context) {
    this.cid = cid.toString();
    if (context.getContainersAppStartTime().containsKey(cid)) {
      this.startTime = context.getContainersAppStartTime().get(cid);
    } else {
      this.startTime = "";
    }
    if (context.getContainerStatus(cid) != null) {
      this.status = context.getContainerStatus(cid).toString();
    } else {
      this.status = "";
    }
    if (context.getContainerGPUDevice(cid) == null) {
      this.gpu = context.getContainerGPUDevice(cid);
    } else {
      this.gpu = "";
    }
  }

}
