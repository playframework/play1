package play.libs;

import java.util.HashMap;
import java.util.Map;

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
}
