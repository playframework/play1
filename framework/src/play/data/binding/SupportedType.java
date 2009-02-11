package play.data.binding;

/**
 * Supported type for binding
 */
public interface SupportedType<T> {
    
    T bind(String value) throws Exception;

}
