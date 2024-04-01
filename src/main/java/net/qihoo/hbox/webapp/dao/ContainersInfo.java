package net.qihoo.hbox.webapp.dao;

import net.qihoo.hbox.api.ApplicationContext;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;

/**
 * Created by jiarunying-it on 2018/8/30.
 */
@XmlRootElement(name = "containerInfo")
@XmlAccessorType(XmlAccessType.FIELD)
public class ContainersInfo {
  protected ArrayList<ContainerInfo> containerInfos = new ArrayList<>();
  public ContainersInfo() {

  }

  public void add(ContainerInfo containerInfo) {
    containerInfos.add(containerInfo);
  }
}
