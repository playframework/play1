package play.data.binding;

public class Data1 {

    public static int myStatic;

    private final String f = "final";

    public String a;

    public int b;

    public void abc(Integer a) {

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Data1 data1 = (Data1) o;

        if (b != data1.b) return false;
        if (a != null ? !a.equals(data1.a) : data1.a != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = a != null ? a.hashCode() : 0;
        result = 31 * result + b;
        return result;
    }
}
