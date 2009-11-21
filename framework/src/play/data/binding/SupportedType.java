package play.data.binding;

import java.lang.annotation.Annotation;

/**
 * Supported type for binding
 */
public interface SupportedType<T> {
    
    T bind(Annotation[] annotations, String value) throws Exception;

}
