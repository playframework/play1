package play.data.binding;

import java.lang.annotation.Annotation;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Date;

import play.data.binding.annotations.AnnotationHelper;
import play.i18n.Lang;
import play.libs.I18N;

/**
 * Binder that support Calendar class.
 */
public class CalendarBinder implements SupportedType<Calendar> {

    public Calendar bind(Annotation[] annotations, String value) throws Exception {
        Calendar cal = Calendar.getInstance(Lang.getLocale());
        try {
            Date date = AnnotationHelper.getDateAs(annotations, value);
            if (date != null) {
                cal.setTime(date);
            } else {
                cal.setTime(new SimpleDateFormat(I18N.getDateFormat()).parse(value));
            }
        } catch (ParseException e) {
            throw new IllegalArgumentException("Cannot convert [" + value + "] to a Calendar: " + e.toString());
            //cal.setTime(Utils.AlternativeDateFormat.getDefaultFormatter().parse(value));
        }

        return cal;
    }
}
