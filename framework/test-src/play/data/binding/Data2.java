package play.data.binding;

import java.util.List;

public class Data2 {
    public String a;
    public Boolean b;
    public int c;
	public List<Data1> datas;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Data2 data2 = (Data2) o;

        if (c != data2.c) return false;
        if (a != null ? !a.equals(data2.a) : data2.a != null) return false;
        if (b != null ? !b.equals(data2.b) : data2.b != null) return false;
        if (datas != null ? !datas.equals(data2.datas) : data2.datas != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = a != null ? a.hashCode() : 0;
        result = 31 * result + (b != null ? b.hashCode() : 0);
        result = 31 * result + c;
        result = 31 * result + (datas != null ? datas.hashCode() : 0);
        return result;
    }
}
