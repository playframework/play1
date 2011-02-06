package play.libs;

public class F {

    public static interface Action0 {
        void invoke();
    }

    public static interface Action<T> {
        void invoke(T result);
    }
    
}
