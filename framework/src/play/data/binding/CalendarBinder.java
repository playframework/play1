package play.data.binding;

import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import play.i18n.Lang;

/**
 * a Date binder
 * 
 */
public class CalendarBinder implements SupportedType<Calendar> {

    // as SimpleDateFormat are not thread safe, we wrap it in a ThreadLocal
    static ThreadLocal<AlternativeDateFormat> dateformat = new ThreadLocal<AlternativeDateFormat>();
    

    static {
        dateformat.set(new AlternativeDateFormat(Locale.US,
                "yyyy-MM-dd'T'hh:mm:ss'Z'", // ISO8601 + timezone
                "yyyy-MM-dd'T'hh:mm:ss", // ISO8601
                "yyyy-MM-dd",
                "yyyyMMdd'T'hhmmss",
                "yyyyMMddhhmmss",
                "dd/MM/yyyy",
                "dd-MM-yyyy",
                "ddMMyyyy"));
    }

    public CalendarBinder() {
    }

    public Calendar bind(String value) {
        try {
            Calendar cal;
            if (Lang.get() != null && !"".equals(Lang.get())) {
                cal = Calendar.getInstance(new Locale(Lang.get()));
            } else {
                cal = Calendar.getInstance(Locale.getDefault());
            }
            cal.setTime(dateformat.get().parse(value));
            return cal;
        } catch (ParseException ex) {
            play.Logger.warn("failed to parse calendar (%s)", value);
        }
        return null;
    }
}
