package play.data.binding;

import java.lang.annotation.Annotation;
import java.util.Map;

/**
 * Supported type for binding. This interface is used to implement custom binders.
 *
 */
public interface ExtendedTypeBinder<T> extends TypeBinder<T>  {
    
    void unBind(Map<String, Object> result, Object src, Class<?> srcClazz, String name, Annotation[] annotations) throws Exception;

}
