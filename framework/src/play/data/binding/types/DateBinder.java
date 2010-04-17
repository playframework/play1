package play.data.binding.types;

import java.lang.annotation.Annotation;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import play.data.binding.annotations.AnnotationHelper;
import play.libs.I18N;

/**
 * Binder that support Date class.
 */
public class DateBinder implements SupportedType<Date> {

    public Date bind(Annotation[] annotations, String value) throws Exception {

        Date date = AnnotationHelper.getDateAs(annotations, value);
        if (date != null) {
            return date;
        }

        try {
            return new SimpleDateFormat(I18N.getDateFormat()).parse(value);
        } catch (ParseException e) {
             throw new IllegalArgumentException("Cannot convert [" + value + "] to a Date: " + e.toString());
            // Left empty. 
            // return Utils.AlternativeDateFormat.getDefaultFormatter().parse(value);
        }
        
    }

}
