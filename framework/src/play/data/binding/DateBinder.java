package play.data.binding;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * A date binder.
 * Bind date objects from the following date patterns : 
 *   - dd/MM/yyyy
 *   - dd-MM-yyyy
 *   - ddMMyyyy
 */
public class DateBinder implements SupportedType<Date> {

    static Map<String, DateFormat> formats = new HashMap<String, DateFormat>();
    static {
        formats.put("\\d\\d/\\d\\d/\\d\\d\\d\\d", new SimpleDateFormat("dd/MM/yyyy"));
        formats.put("\\d\\d-\\d\\d-\\d\\d\\d\\d", new SimpleDateFormat("dd-MM-yyyy"));
        formats.put("\\d\\d\\d\\d\\d\\d\\d\\d", new SimpleDateFormat("ddMMyyyy"));
    }
    
    public Date bind(String value) {
        for (String pattern : formats.keySet()) {
            if (value.matches(pattern)) {
                try {
                    return formats.get(pattern).parse(value);
                } catch (ParseException e) {
                    // Nope
                }
            }
        }
        return null;
    }

}
