package play.data.binding.annotations;

import java.lang.annotation.Annotation;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.apache.commons.lang.StringUtils;
import play.i18n.Lang;

public class AnnotationHelper {

    /**
     *
     * @param annotations
     * @param value
     * @return null if it cannot be converted because there is no annotation.
     * @throws ParseException
     */
    public static Date getDateAs(Annotation[] annotations, String value) throws ParseException {
        // Look up for the Bind annotation
        if (annotations != null) {
            for (Annotation annotation : annotations) {
                if (annotation.annotationType().equals(Bind.class)) {
                    final String format = ((Bind) annotation).format();
                    if (!StringUtils.isEmpty(format)) {
                        return new SimpleDateFormat(format, Lang.getLocale()).parse(value);
                    }
                }
            }
        }
        return null;
     }
    
}
