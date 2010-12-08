package play.data.binding.types;

import play.data.binding.TypeBinder;
import java.lang.annotation.Annotation;
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

    public Date bind(String name, Annotation[] annotations, String value, Class actualClass) throws Exception {

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

        try {
            SimpleDateFormat sdf = new SimpleDateFormat(ISO8601);
            sdf.setLenient(false);
            return sdf.parse(value);
        } catch(Exception e) {
            throw new IllegalArgumentException("Cannot convert [" + value + "] to a Date: " + e.toString());
        }
        
    }

}
