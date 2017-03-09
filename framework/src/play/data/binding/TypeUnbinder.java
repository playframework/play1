package play.data.binding;

import java.lang.annotation.Annotation;
import java.util.Map;

/**
 * Supported type for unbinding. This interface is used to implement custom unbinders.
 * 
 * @param <T>
 *            Type of the unbinder
 *
 */
public interface TypeUnbinder<T> {

    /**
     * @param result
     *            The result container
     * @param src
     *            the object you want to unbind
     * @param srcClazz
     *            The class of the object you want to associate the value with
     * @param name
     *            the name of you parameter ie myparam for a simple param but can also be a complex one :
     *            mybean.address.street
     * @param annotations
     *            An array of annotation that may be bound to your method parameter or your bean property
     * @return true if unwinder is successful, otherwise false and will use the default unbinder
     * @throws Exception
     *             if problem occurred
     */
    boolean unBind(Map<String, Object> result, Object src, Class<?> srcClazz, String name, Annotation[] annotations) throws Exception;
}
