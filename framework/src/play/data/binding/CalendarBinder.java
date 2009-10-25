package play.data.binding;

import java.lang.annotation.Annotation;
import java.util.Calendar;
import java.util.Locale;

import play.data.binding.DateBinder.AlternativeDateFormat;
import play.i18n.Lang;

/**
 * Binder that support Calendar class.
 */
public class CalendarBinder implements SupportedType<Calendar> {

    public Calendar bind(Annotation[] annotations, String value) throws Exception {
        Calendar cal;
        if (Lang.get() != null && !"".equals(Lang.get())) {
            cal = Calendar.getInstance(new Locale(Lang.get()));
        } else {
            cal = Calendar.getInstance(Locale.getDefault());
        }
        cal.setTime(AlternativeDateFormat.getDefaultFormatter().parse(value));
        return cal;
    }
}
