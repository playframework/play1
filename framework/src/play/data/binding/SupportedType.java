package play.data.binding;

public interface SupportedType<T> {
    
    T bind(String value);

}
