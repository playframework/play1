package play.data.binding.types;

import play.data.binding.TypeBinder;
import java.lang.annotation.Annotation;
import java.util.Locale;

/**
 * Binder that support Locale class.
 */
public class LocaleBinder implements TypeBinder<Locale> {

    public Locale bind(String name, Annotation[] annotations, String value, Class actualClass) {
        if( value == null )
            return null;
        if (value.length() == 2) {
            return new Locale(value);
        }
        if (value.length() == 5) {
            return new Locale(value.substring(0, 1), value.substring(3, 4));
        }
        return Locale.getDefault();
    }
    
}

