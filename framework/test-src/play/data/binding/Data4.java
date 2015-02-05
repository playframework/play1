package play.data.binding;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class Data4 {

    public String s;
    public List<Data1> datas;
    public Data1[] datasArray;

    public Map<String, Data1> mapDatas;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Data4 data4 = (Data4) o;

        if (datas != null ? !datas.equals(data4.datas) : data4.datas != null) return false;
        //asList to ignore sequence of elements. It's not mandatory in binder
        if (datas != null ? !Arrays.asList(datas).equals(Arrays.asList(data4.datas)) : data4.datas != null) return false;
        if (s != null ? !s.equals(data4.s) : data4.s != null) return false;


        if (mapDatas != null ? !mapDatas.equals(data4.mapDatas) : data4.mapDatas != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = s != null ? s.hashCode() : 0;
        result = 31 * result + (datas != null ? datas.hashCode() : 0);
        result = 31 * result + (datasArray != null ? Arrays.hashCode(datasArray) : 0);
        result = 31 * result + (mapDatas != null ? mapDatas.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Data4{" +
                "s='" + s + '\'' +
                ", datas=" + datas +
                ", datasArray=" + Arrays.toString(datasArray) +
                '}';
    }
}
