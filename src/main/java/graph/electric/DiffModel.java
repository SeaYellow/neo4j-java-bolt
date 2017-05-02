package graph.electric;

/**
 * Created by Administrator on 2017/4/26.
 */
public class DiffModel {
    private int id;
    private Integer diffGroup;

    public DiffModel(int id, Integer type) {
        this.id = id;
        this.diffGroup = type;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Integer getDiffGroup() {
        return diffGroup;
    }

    public void setDiffGroup(Integer diffGroup) {
        this.diffGroup = diffGroup;
    }
}
