package play.data.binding;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Locale;

import play.i18n.Lang;

public class CalendarBinder implements SupportedType<Calendar> {    
    public Calendar bind(String value) {
        try {
            Calendar cal;
            if (Lang.get() != null && !"".equals(Lang.get()))
                cal = Calendar.getInstance(new Locale(Lang.get()));
            else
                cal = Calendar.getInstance(Locale.getDefault());
            cal.setTime(AlternativeDateFormat.getDefaultFormatter().parse(value));
            return cal;
        } catch (ParseException ex) {
            play.Logger.warn("failed to parse calendar (%s)", value);
        }
        return null;
    }
}
