package play.data.binding;

import java.util.Locale;

public class LocaleBinder implements SupportedType<Locale> {

    public Locale bind(String value) {
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

