package play.libs;

import java.util.Map;
import org.apache.commons.lang.StringUtils;
import play.Play;
import play.i18n.Lang;

/**
 * I18N utils
 */
public class I18N {

    static final Map<String, String> symbols = Map.ofEntries(
        Map.entry("ALL", "Lek"),
        Map.entry("USD", "$"),
        Map.entry("AFN", "؋"),
        Map.entry("ARS", "$"),
        Map.entry("AWG", "ƒ"),
        Map.entry("AUD", "$"),
        Map.entry("AZN", "ман"),
        Map.entry("BSD", "$"),
        Map.entry("BBD", "$"),
        Map.entry("BYR", "p."),
        Map.entry("EUR", "€"),
        Map.entry("BZD", "BZ$"),
        Map.entry("BMD", "$"),
        Map.entry("BOB", "$b"),
        Map.entry("BAM", "KM"),
        Map.entry("BWP", "P"),
        Map.entry("BGN", "лв"),
        Map.entry("BRL", "R$"),
        Map.entry("GBP", "£"),
        Map.entry("BND", "$"),
        Map.entry("KHR", "៛"),
        Map.entry("CAD", "$"),
        Map.entry("KYD", "$"),
        Map.entry("CLP", "$"),
        Map.entry("CNY", "¥"),
        Map.entry("COP", "$"),
        Map.entry("CRC", "₡"),
        Map.entry("HRK", "kn"),
        Map.entry("CUP", "₱"),
        Map.entry("CZK", "Kč"),
        Map.entry("DKK", "kr"),
        Map.entry("DOP", "RD$"),
        Map.entry("XCD", "$"),
        Map.entry("EGP", "£"),
        Map.entry("SVC", "$"),
        Map.entry("EEK", "kr"),
        Map.entry("FKP", "£"),
        Map.entry("FJD", "$"),
        Map.entry("GHC", "¢"),
        Map.entry("GIP", "£"),
        Map.entry("GTQ", "Q"),
        Map.entry("GGP", "£"),
        Map.entry("GYD", "$"),
        Map.entry("HNL", "L"),
        Map.entry("HKD", "$"),
        Map.entry("HUF", "Ft"),
        Map.entry("ISK", "kr"),
        Map.entry("INR", ""),
        Map.entry("IDR", "Rp"),
        Map.entry("IRR", "﷼"),
        Map.entry("IMP", "£"),
        Map.entry("ILS", "₪"),
        Map.entry("JMD", "J$"),
        Map.entry("JPY", "¥"),
        Map.entry("JEP", "£"),
        Map.entry("KZT", "лв"),
        Map.entry("KPW", "₩"),
        Map.entry("KGS", "лв"),
        Map.entry("LAK", "₭"),
        Map.entry("LVL", "Ls"),
        Map.entry("LBP", "£"),
        Map.entry("LRD", "$"),
        Map.entry("CHF", "CHF"),
        Map.entry("LTL", "Lt"),
        Map.entry("MKD", "ден"),
        Map.entry("MYR", "RM"),
        Map.entry("MUR", "₨"),
        Map.entry("MXN", "$"),
        Map.entry("MNT", "₮"),
        Map.entry("MZN", "MT"),
        Map.entry("NAD", "$"),
        Map.entry("NPR", "₨"),
        Map.entry("ANG", "ƒ"),
        Map.entry("NZD", "$"),
        Map.entry("NIO", "C$"),
        Map.entry("NGN", "₦"),
        Map.entry("NOK", "kr"),
        Map.entry("OMR", "﷼"),
        Map.entry("PKR", "₨"),
        Map.entry("PAB", "B/."),
        Map.entry("PYG", "Gs"),
        Map.entry("PEN", "S/."),
        Map.entry("PHP", "Php"),
        Map.entry("PLN", "zł"),
        Map.entry("QAR", "﷼"),
        Map.entry("RON", "lei"),
        Map.entry("RUB", "руб."),
        Map.entry("SHP", "£"),
        Map.entry("SAR", "﷼"),
        Map.entry("RSD", "Дин."),
        Map.entry("SCR", "₨"),
        Map.entry("SGD", "$"),
        Map.entry("SBD", "$"),
        Map.entry("SOS", "S"),
        Map.entry("ZAR", "R"),
        Map.entry("KRW", "₩"),
        Map.entry("LKR", "₨"),
        Map.entry("SEK", "kr"),
        Map.entry("SRD", "$"),
        Map.entry("SYP", "£"),
        Map.entry("TWD", "NT$"),
        Map.entry("THB", "฿"),
        Map.entry("TTD", "TT$"),
        Map.entry("TRY", "TL"),
        Map.entry("TRL", "₤"),
        Map.entry("TVD", "$"),
        Map.entry("UAH", "₴"),
        Map.entry("UYU", "$U"),
        Map.entry("UZS", "лв"),
        Map.entry("VEF", "Bs"),
        Map.entry("VND", "₫"),
        Map.entry("YER", "﷼"),
        Map.entry("ZWD", "Z$")
    );

    /**
     * Retrieve currency symbol for a currency
     * @param currency (JPY,USD,EUR,GBP,...)
     * @return ($, €, ...)
     */
    public static String getCurrencySymbol(String currency) {
        return symbols.getOrDefault(currency, currency);
    }

    public static String getDateFormat() {
        String localizedDateFormat = Play.configuration.getProperty("date.format." + Lang.get());
        if (!StringUtils.isEmpty(localizedDateFormat)) {
            return localizedDateFormat;
        }
        String globalDateFormat = Play.configuration.getProperty("date.format");
        if (!StringUtils.isEmpty(globalDateFormat)) {
            return globalDateFormat;
        }
        // Default value. It's completely arbitrary.
        return "yyyy-MM-dd";
    }

}
