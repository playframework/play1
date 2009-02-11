package play.data.binding;

import java.util.Date;

/**
 * Binder that support Date class
 */
public class DateBinder implements SupportedType<Date> {

    public Date bind(String value) throws Exception {
        return AlternativeDateFormat.getDefaultFormatter().parse(value);
    }
}
