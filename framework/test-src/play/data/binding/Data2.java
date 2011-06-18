package play.data.binding;

public class Data2 {
    public String a;
    public Boolean b;
    public int c;

    /**
     * Tried first with arrays and lists but the Unbinder fails in such situations.
     */

    public Data1 data1;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Data2 data2 = (Data2) o;

        if (c != data2.c) return false;
        if (a != null ? !a.equals(data2.a) : data2.a != null) return false;
        if (b != null ? !b.equals(data2.b) : data2.b != null) return false;
        if (data1 != null ? !data1.equals(data2.data1) : data2.data1 != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = a != null ? a.hashCode() : 0;
        result = 31 * result + (b != null ? b.hashCode() : 0);
        result = 31 * result + c;
        result = 31 * result + (data1 != null ? data1.hashCode() : 0);
        return result;
    }
}
