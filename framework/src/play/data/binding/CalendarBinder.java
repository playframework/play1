package play.data.binding;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Locale;

import play.i18n.Lang;

public class CalendarBinder implements SupportedType<Calendar> {

	static ThreadLocal<AlternativeDateFormat> dateformat = new ThreadLocal<AlternativeDateFormat>();

    public static AlternativeDateFormat getFormatter () {
    	if (dateformat.get()==null) {
    		 dateformat.set(new AlternativeDateFormat(Locale.US,
    	                "yyyy-MM-dd'T'hh:mm:ss'Z'", // ISO8601 + timezone
    	                "yyyy-MM-dd'T'hh:mm:ss", // ISO8601
    	                "yyyy-MM-dd",
    	                "yyyyMMdd'T'hhmmss",
    	                "yyyyMMddhhmmss",
    	                "dd'/'MM'/'yyyy",
    	                "dd-MM-yyyy",
    	                "ddMMyyyy"));
    	}
    	return dateformat.get();
    }
    
    public Calendar bind(String value) {
        try {
            Calendar cal;
            if (Lang.get() != null && !"".equals(Lang.get()))
                cal = Calendar.getInstance(new Locale(Lang.get()));
            else
                cal = Calendar.getInstance(Locale.getDefault());
            cal.setTime(getFormatter().parse(value));
            return cal;
        } catch (ParseException ex) {
            play.Logger.warn("failed to parse calendar (%s)", value);
        }
        return null;
    }
}
