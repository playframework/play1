package play.data.binding.types;

import play.data.binding.TypeBinder;
import play.i18n.Lang;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Locale;

/**
 * Binder that support Locale class.
 */
public class LocaleBinder implements TypeBinder<Locale> {

    public Locale bind(String name, Annotation[] annotations, String value, Class actualClass, Type genericType) {
        if( value == null )
            return null;
        
        return Lang.getLocale(value);
    }
}