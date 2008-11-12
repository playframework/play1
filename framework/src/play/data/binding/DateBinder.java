package play.data.binding;

import java.text.ParseException;
import java.util.Date;

public class DateBinder implements SupportedType<Date> {
	
    public Date bind(String value) {
        try {
            return AlternativeDateFormat.getDefaultFormatter().parse(value);
        } catch (ParseException ex) {
            play.Logger.warn("failed to parse date (%s)", value);
        }
        return null;
    }
}
