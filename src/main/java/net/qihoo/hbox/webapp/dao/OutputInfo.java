package net.qihoo.hbox.webapp.dao;

import net.qihoo.hbox.api.ApplicationContext;
import org.apache.hadoop.fs.Path;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;

/**
 * Created by jiarunying-it on 2018/9/17.
 */
@XmlRootElement(name = "outputInfo")
@XmlAccessorType(XmlAccessType.FIELD)
public class OutputInfo {
  protected ArrayList<String> outputInfos = new ArrayList<>();

  public OutputInfo() {

  }

  public OutputInfo(ApplicationContext context, String interResultPath) {
    for (net.qihoo.hbox.common.OutputInfo output : context.getOutputs()) {
      Path interResult = new Path(output.getDfsLocation() + interResultPath);
      outputInfos.add(interResult.toString());
    }
  }

}
