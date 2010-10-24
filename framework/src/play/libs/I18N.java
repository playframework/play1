package play.libs;

import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang.StringUtils;
import play.Play;
import play.i18n.Lang;

/**
 * I18N utils
 */
public class I18N {

    static final Map<String, String> symbols = new HashMap<String, String>();

    static {
        symbols.put("JPY", "¥");
        symbols.put("USD", "$");
        symbols.put("EUR", "€");
        symbols.put("GBP", "£");
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
        final String localizedDateFormat = Play.configuration.getProperty("date.format." + Lang.get());
        if (!StringUtils.isEmpty(localizedDateFormat)) {
            return localizedDateFormat;
        }
        final String globalDateFormat = Play.configuration.getProperty("date.format");
        if (!StringUtils.isEmpty(globalDateFormat)) {
            return globalDateFormat;
        }
        // Default value. It's completely arbitrary.
        return "yyyy-MM-dd";
    }
    
}
