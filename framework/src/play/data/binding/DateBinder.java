package play.data.binding;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

/**
 * Binder that support Date class.
 */
public class DateBinder implements SupportedType<Date> {

    public Date bind(String value) throws Exception {
        return AlternativeDateFormat.getDefaultFormatter().parse(value);
    }

    public static class AlternativeDateFormat {

        Locale locale;
        List<DateTimeFormatter> formats = new ArrayList<DateTimeFormatter>();

        public AlternativeDateFormat(Locale locale, String... alternativeFormats) {
            super();
            this.locale = locale;
            setFormats(alternativeFormats);
        }

        public void setFormats(String... alternativeFormats) {
            for (String format : alternativeFormats) {
                formats.add(DateTimeFormat.forPattern(format));
            }
        }

        public Date parse(String source) throws ParseException {
            for (DateTimeFormatter dateFormat : formats) {
                try {
                    return dateFormat.parseDateTime(source).toDate();
                } catch (Exception ex) {
                }
            }
            throw new ParseException("Date format not understood", 0);
        }
        static ThreadLocal<AlternativeDateFormat> dateformat = new ThreadLocal<AlternativeDateFormat>();

        public static AlternativeDateFormat getDefaultFormatter() {
            if (dateformat.get() == null) {
                dateformat.set(new AlternativeDateFormat(Locale.US,
                        "yyyy-MM-dd'T'HH:mm:ss'Z'", // ISO8601 + timezone
                        "yyyy-MM-dd'T'HH:mm:ss", // ISO8601
                        "yyyy-MM-dd",
                        "yyyyMMdd'T'HHmmss",
                        "yyyyMMddHHmmss",
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
