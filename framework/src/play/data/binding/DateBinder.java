package play.data.binding;

import java.lang.annotation.Annotation;
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

        // TODO: Check that the format matches otherwise throws an exception?
        return new SimpleDateFormat(I18N.getDateFormat()).parse(value);
    }
}
