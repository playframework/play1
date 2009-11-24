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
import play.utils.Utils;

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

        Date date = AnnotationHelper.getDateAs(annotations, value);
        if (date != null) {
            cal.setTime(date);
        } else {
            try {
                cal.setTime(new SimpleDateFormat(I18N.getDateFormat()).parse(value));
            } catch (ParseException e) {
                //cal.setTime(Utils.AlternativeDateFormat.getDefaultFormatter().parse(value));
            }
        }

        return cal;
    }
}
