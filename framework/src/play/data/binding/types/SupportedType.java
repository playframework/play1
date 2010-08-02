package play.data.binding.types;

import java.lang.annotation.Annotation;

/**
 * Supported type for binding
 */
public interface SupportedType<T> {
    
    Object bind(Annotation[] annotations, String value, Class actualClass) throws Exception;

}
