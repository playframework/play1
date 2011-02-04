package play.libs;

public class F {

    public static interface Action<T> {
        void invoke(T result);
    }
}
