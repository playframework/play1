package play.data.binding;

@AttributeStripping
public class Data1S {

    public static int myStatic;

    private final String f = "final";

    public String a;

    public String n;

    @AttributeStripping(squish = true)
    public String q;

    public int b;

    public void abc(Integer a) {

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Data1S data1 = (Data1S) o;

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
