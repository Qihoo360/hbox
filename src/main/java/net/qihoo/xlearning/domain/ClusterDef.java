package net.qihoo.xlearning.domain;

import java.util.List;

/**
 * @Author songxitang
 * @Description: Old ClusterDef Bean
 * @Date 2017/12/25 16:31
 * @Modified By:
 */
public class ClusterDef {
  private List<String> ps;
  private List<String> worker;

    public ClusterDef() {
    }

    public List<String> getPs() {
        return ps;
    }

    public void setPs(List<String> ps) {
        this.ps = ps;
    }

    public List<String> getWorker() {
        return worker;
    }

    public void setWorker(List<String> worker) {
        this.worker = worker;
    }

    @Override
    public String toString() {
        return "ClusterDef{" +
                "ps=" + ps +
                ", worker=" + worker +
                '}';
    }
}
