package play.data.binding;

import java.text.ParseException;
import java.util.Date;
import java.util.Locale;

public class DateBinder implements SupportedType<Date> {
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

    public Date bind(String value) {
        try {
            return getFormatter().parse(value);
        } catch (ParseException ex) {
            play.Logger.warn("failed to parse date (%s)", value);
        }
        return null;
    }
}
