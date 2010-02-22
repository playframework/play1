package play.data.binding.annotations;

import java.lang.annotation.Annotation;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.apache.commons.lang.StringUtils;
import play.i18n.Lang;

public class AnnotationHelper {

    /**
     * @param annotations
     * @param value
     * @return null if it cannot be converted because there is no annotation.
     * @throws ParseException
     */
    public static Date getDateAs(Annotation[] annotations, String value) throws ParseException {
        // Look up for the BindAs annotation
        if (annotations != null) {
            for (Annotation annotation : annotations) {
                if (annotation.annotationType().equals(As.class)) {
                    As as = (As) annotation;
                    final String format = as.value()[0];
                    if (!StringUtils.isEmpty(format)) {
                        if (!"*".equals(as.lang()[0])) {
                            // It can be comma separated
                            // if one of the lang matches
                            Locale locale = getLocale(as.lang());
                            if (locale != null) {
                                return new SimpleDateFormat(format, locale).parse(value);
                            }
                        }
                        return new SimpleDateFormat(format, Lang.getLocale()).parse(value);
                    }
                }
            }
        }
        return null;
    }

    public static Locale getLocale(String[] langs) {
        for (String lang: langs) {
            Locale locale = Lang.getLocale(lang);
            if (locale != null) {
                return locale;
            }
        }
        return null;
    }

}
