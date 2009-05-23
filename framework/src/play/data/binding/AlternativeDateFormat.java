package play.data.binding;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Date formatter that tries multiple formats before failing
 */
public class AlternativeDateFormat {

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
        throw new ParseException("Date format not understood",0);
    }
    
    static ThreadLocal<AlternativeDateFormat> dateformat = new ThreadLocal<AlternativeDateFormat>();

    public static AlternativeDateFormat getDefaultFormatter () {
    	if (dateformat.get()==null) {
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
