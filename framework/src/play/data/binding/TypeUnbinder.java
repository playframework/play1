package play.data.binding;

import java.lang.annotation.Annotation;
import java.util.Map;

/**
 * Supported type for unbinding. This interface is used to implement custom unbinders.
 *
 */
public interface TypeUnbinder<T> {
    
    void unBind(Map<String, Object> result, Object src, Class<?> srcClazz, String name, Annotation[] annotations) throws Exception;

}
