package net.qihoo.xlearning.domain;

/**
 * @Author songxitang
 * @Description: TFConfig Bean for new api in distributed tensorflow
 * @Date 2017/12/25 15:45
 * @Modified By:
 */

/**
'{
    "cluster": {
        "chief": ["host0:2222"],
        "worker": ["host1:2222", "host2:2222", "host3:2222"],
        "ps": ["host4:2222", "host5:2222"]
    },
    "task": {"type": "chief", "index": 0}
 }'
 */

public class TFConfig {
    private Cluster cluster;
    private Task task;

    public TFConfig() {
    }

    public Cluster getCluster() {
        return cluster;
    }

    public void setCluster(Cluster cluster) {
        this.cluster = cluster;
    }

    public Task getTask() {
        return task;
    }

    public void setTask(Task task) {
        this.task = task;
    }

    @Override
    public String toString() {
        return "TFConfig{" +
                "cluster=" + cluster +
                ", task=" + task +
                '}';
    }
}
