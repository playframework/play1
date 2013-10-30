package play.data.binding;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

/**
 * Supported type for binding. This interface is used to implement custom binders.
 *
 */
public interface TypeBinder<T> {

    /**
     * Called when your parameter needs to be bound.
     *
     * @param name the name of you parameter ie myparam for a simple param but can also be a complex one : mybean.address.street
     * @param annotations An array of annotation that may be bound to your method parameter or your bean property
     * @param value  the actual value as a string that needs to be bound
     * @param actualClass The class of the object you want to associate the value with
     * @param genericType  The generic type associated with the object you want to bound the value to
     * @return  the 'bound' object for example a date object if the value was '12/12/2002'
     * @throws Exception
     */
    Object bind(String name, Annotation[] annotations, String value, Class actualClass, Type genericType) throws Exception;

}
