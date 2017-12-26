package net.qihoo.xlearning.domain;

/**
 * @Author songxitang
 * @Description: Task Bean in TFConfig
 * @Date 2017/12/25 15:54
 * @Modified By:
 */
public class Task {
    private String type;
    private int index;

    public Task() {
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    @Override
    public String toString() {
        return "Task{" +
                "type='" + type + '\'' +
                ", index=" + index +
                '}';
    }
}
