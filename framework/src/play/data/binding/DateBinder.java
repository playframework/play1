package play.data.binding;

import java.lang.annotation.Annotation;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import play.Logger;

/**
 * Binder that support Date class.
 */
public class DateBinder implements SupportedType<Date> {

    public Date bind(Annotation[] annotations, String value) throws Exception {
        Logger.info("bind %s " , annotations[0]);
        return AlternativeDateFormat.getDefaultFormatter().parse(value);
    }

    public static class AlternativeDateFormat {

        Locale locale;
        List<SimpleDateFormat> formats = new ArrayList<SimpleDateFormat>();

        public AlternativeDateFormat(Locale locale, String... alternativeFormats) {
            super();
            this.locale = locale;
            setFormats(alternativeFormats);
        }

        public void setFormats(String... alternativeFormats) {
            for (String format : alternativeFormats) {
                formats.add(new SimpleDateFormat(format, locale));
            }
        }

        public Date parse(String source) throws ParseException {
            for (SimpleDateFormat dateFormat : formats) {
                if (source.length() == dateFormat.toPattern().replace("\'", "").length()) {
                    try {
                        return dateFormat.parse(source);
                    } catch (ParseException ex) {
                    }
                }
            }
            throw new ParseException("Date format not understood", 0);
        }
        static ThreadLocal<AlternativeDateFormat> dateformat = new ThreadLocal<AlternativeDateFormat>();

        public static AlternativeDateFormat getDefaultFormatter() {
            if (dateformat.get() == null) {
                dateformat.set(new AlternativeDateFormat(Locale.US,
                        "yyyy-MM-dd'T'hh:mm:ss'Z'", // ISO8601 + timezone
                        "yyyy-MM-dd'T'hh:mm:ss", // ISO8601
                        "yyyy-MM-dd",
                        "yyyyMMdd'T'hhmmss",
                        "yyyyMMddhhmmss",
                        "dd'/'MM'/'yyyy",
                        "dd-MM-yyyy",
                        "ddMMyyyy",
                        "MMddyy",
                        "MM-dd-yy",
                        "MM'/'dd'/'yy"));
            }
            return dateformat.get();
        }
    }
}
