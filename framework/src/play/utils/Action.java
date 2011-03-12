package play.utils;

public interface Action<T> {

    void invoke(T result);
}
