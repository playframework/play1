package play.data.binding.types;

import play.data.binding.TypeBinder;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import play.data.binding.AnnotationHelper;
import play.libs.I18N;

/**
 * Binder that support Date class.
 */
public class DateBinder implements TypeBinder<Date> {

    public static final String ISO8601 = "'ISO8601:'yyyy-MM-dd'T'HH:mm:ssZ";

    @Override
    public Date bind(String name, Annotation[] annotations, String value, Class actualClass, Type genericType) throws Exception {
        if (value == null || value.trim().length() == 0) {
            return null;
        }

        Date date = AnnotationHelper.getDateAs(annotations, value);
        if (date != null) {
            return date;
        }

        try {
            SimpleDateFormat sdf = new SimpleDateFormat(I18N.getDateFormat());
            sdf.setLenient(false);
            return sdf.parse(value);
        } catch (ParseException e) {
            // Ignore
        }

        SimpleDateFormat sdf = new SimpleDateFormat(ISO8601);
        sdf.setLenient(false);
        return sdf.parse(value);
    }
}
