package net.qihoo.xlearning.domain;

import java.util.List;

/**
 * @Author songxitang
 * @Description: Cluster Bean in TFConfig
 * @Date 2017/12/25 15:48
 * @Modified By:
 */
public class Cluster {
    private List<String> chief;
    private List<String> worker;
    private List<String> ps;

    public Cluster() {
    }

    public List<String> getChief() {
        return chief;
    }

    public void setChief(List<String> chief) {
        this.chief = chief;
    }

    public List<String> getWorker() {
        return worker;
    }

    public void setWorker(List<String> worker) {
        this.worker = worker;
    }

    public List<String> getPs() {
        return ps;
    }

    public void setPs(List<String> ps) {
        this.ps = ps;
    }

    @Override
    public String toString() {
        return "Cluster{" +
                "chief=" + chief +
                ", worker=" + worker +
                ", ps=" + ps +
                '}';
    }
}
