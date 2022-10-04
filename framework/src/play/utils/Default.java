package play.utils;

public class Default {

    final Object o;

    public Default(Object o) {
        this.o = o;
    }

    @Override
    public String toString() {
        return o.toString();
    }

}
