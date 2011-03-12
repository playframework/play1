package play.data.binding;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

/**
 * Supported type for binding
 */
public interface TypeBinder<T> {
    
    Object bind(String name, Annotation[] annotations, String value, Class actualClass, Type genericType) throws Exception;

}
