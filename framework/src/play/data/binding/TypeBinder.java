package play.data.binding;

import java.lang.annotation.Annotation;

/**
 * Supported type for binding
 */
public interface TypeBinder<T> {
    
    Object bind(String name, Annotation[] annotations, String value, Class actualClass) throws Exception;

}
