package play.data.binding;

import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Date formatter that tries multiple formats before failing
 *
 */
public class AlternativeDateFormat extends DateFormat {

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

    @Override
    public StringBuffer format(Date date, StringBuffer toAppendTo, FieldPosition fieldPosition) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Date parse(String source, ParsePosition pos) {
        for (SimpleDateFormat dateFormat : formats) {
            if (source.length() == dateFormat.toPattern().replace("\'", "").length()) {
                try {
                    return dateFormat.parse(source);
                } catch (ParseException ex) {
                }
            }
        }
        return null;
    }
}
