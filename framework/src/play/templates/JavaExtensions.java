package play.templates;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Currency;
import java.util.Date;
import java.util.Locale;
import play.i18n.Lang;
import play.libs.I18N;

public class JavaExtensions {

    public static String capitalizeWords(String source) {
        char prevc=' '; // first char of source is capitalized
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < source.length(); i++) {
            char c = source.charAt(i);
            if( c != ' ' && prevc == ' ')
                sb.append(Character.toUpperCase(c));
            else
                sb.append(c);
            prevc = c;
        }
        return sb.toString();
    }
    
    public static String escape(String htmlToEscape) {
        return htmlToEscape.replace("<", "&lt;").replace(">", "&gt;");
    }

    public static String format(Number number, String pattern) {
        return new DecimalFormat(pattern).format(number);
    }

    public static String format(Date date, String pattern) {
        return new SimpleDateFormat(pattern).format(date);
    }

    public static String formatCurrency(Number number, String currencyCode) {
        Currency currency = Currency.getInstance(currencyCode);
        NumberFormat numberFormat = NumberFormat.getCurrencyInstance(new Locale(Lang.get()));
        numberFormat.setCurrency(currency);
        numberFormat.setMaximumFractionDigits(currency.getDefaultFractionDigits());
        String s = numberFormat.format(number);
        s = s.replace(currencyCode, I18N.getCurrencySymbol(currencyCode));
        return s;
    }
}
