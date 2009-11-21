package play.data.binding.annotations;

import java.lang.annotation.Annotation;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.apache.commons.lang.StringUtils;

public class AnnotationHelper {

    public static Date getDateAs(Annotation[] annotations, String value) throws ParseException {
        // Look up for the As annotation
        if (annotations != null) {
            for (Annotation annotation : annotations) {
                if (annotation.annotationType().equals(Bind.class)) {
                    final String format = ((Bind) annotation).format();
                    if (!StringUtils.isEmpty(format)) {
                         // TODO: Check that the format matches otherwise throws an exception?
                        return new SimpleDateFormat(format).parse(value);
                    }
                }
            }
        }
        return null;
     }
    
}
