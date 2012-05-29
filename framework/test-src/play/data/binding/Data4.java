package play.data.binding;

import java.util.List;

public class Data4 {
    public String a;
    public Boolean b;
    public int c;

    /**
     * Tried first with arrays and lists but the Unbinder fails in such situations.
     */
    public List<Data1> dataList;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Data4 data4 = (Data4) o;

        if (c != data4.c) return false;
        if (a != null ? !a.equals(data4.a) : data4.a != null) return false;
        if (b != null ? !b.equals(data4.b) : data4.b != null) return false;
        if (dataList != null ? !dataList.equals(data4.dataList) : data4.dataList != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = a != null ? a.hashCode() : 0;
        result = 31 * result + (b != null ? b.hashCode() : 0);
        result = 31 * result + c;
        result = 31 * result + (dataList != null ? dataList.hashCode() : 0);
        return result;
    }
}
