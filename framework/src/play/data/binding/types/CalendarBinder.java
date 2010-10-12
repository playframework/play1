package play.data.binding.types;

import play.data.binding.TypeBinder;
import java.lang.annotation.Annotation;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import play.data.binding.AnnotationHelper;
import play.i18n.Lang;
import play.libs.I18N;

/**
 * Binder that support Calendar class.
 */
public class CalendarBinder implements TypeBinder<Calendar> {

    public Calendar bind(String name, Annotation[] annotations, String value, Class actualClass) throws Exception {
        Calendar cal = Calendar.getInstance(Lang.getLocale());
        try {
            Date date = AnnotationHelper.getDateAs(annotations, value);
            if (date != null) {
                cal.setTime(date);
            } else {
                SimpleDateFormat sdf = new SimpleDateFormat(I18N.getDateFormat());
                sdf.setLenient(false);
                cal.setTime( sdf.parse(value));
            }
        } catch (ParseException e) {
            throw new IllegalArgumentException("Cannot convert [" + value + "] to a Calendar: " + e.toString());
        }

        return cal;
    }
}
