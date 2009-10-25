package play.libs;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.apache.commons.lang.StringUtils;
import play.Play;
import play.PlayConfiguration;

/**
 * I18N utils
 */
public class I18N {

    static Map<String, String> symbols = new HashMap<String, String>();

    static {
        symbols.put("JPY", "&yen;");
        symbols.put("USD", "$");
        symbols.put("EUR", "&euro;");
        symbols.put("GBP", "&pound;");
    }

    /**
     * Retrieve currency symbol for a currency
     * @param currency (JPY,USD,EUR,GBP,...)
     * @return ($, €, ...)
     */
    public static String getCurrencySymbol(String currency) {
        if (symbols.containsKey(currency)) {
            return symbols.get(currency);
        }
        return currency;
    }

    public static String getDateFormat() {
        final String localizedDateFormat = Play.configuration.getProperty(PlayConfiguration.DATE_FORMAT + "." + Locale.getISOLanguages());
        if (!StringUtils.isEmpty(localizedDateFormat)) {
            return localizedDateFormat;
        }
        final String globalDateFormat = Play.configuration.getProperty(PlayConfiguration.DATE_FORMAT);
        if (!StringUtils.isEmpty(localizedDateFormat)) {
            return globalDateFormat;
        }
        return new SimpleDateFormat().toPattern();
    }
    
}
