package play.data.validation;

import net.sf.oval.Validator;
import net.sf.oval.configuration.annotation.AbstractAnnotationCheck;
import net.sf.oval.context.OValContext;

import java.util.regex.Pattern;

@SuppressWarnings("serial")
public class URLCheck extends AbstractAnnotationCheck<URL> {

    static final String mes = "validation.url";

    boolean tldMandatory;
    boolean excludeLoopback;

    /**
     * big thank you to https://gist.github.com/dperini/729294
     * well suited url pattern for most of internet routeable ips and real domains
     */
    static String[] regexFragments = {
            /* 00 */ "^",
            // protocol identifier
            /* 01 */ "(?:(?:https?|s?ftp|rtsp|mms)://)",
            // user:pass authentication
            /* 02 */ "(?:\\S+(?::\\S*)?@)?",
            /* 03 */ "(?:",
            // IP address exclusion
            // private & local networks
            /* 04 */ "(?!(?:10|127)(?:\\.\\d{1,3}){3})",
            /* 05 */ "(?!(?:169\\.254|192\\.168)(?:\\.\\d{1,3}){2})",
            /* 06 */ "(?!172\\.(?:1[6-9]|2\\d|3[0-1])(?:\\.\\d{1,3}){2})",
            // IP address dotted notation octets
            // excludes loopback network 0.0.0.0
            // excludes reserved space >= 224.0.0.0
            // excludes network & broacast addresses
            // (first & last IP address of each class)
            /* 07 */ "(?:[1-9]\\d?|1\\d\\d|2[01]\\d|22[0-3])",
            /* 08 */ "(?:\\.(?:1?\\d{1,2}|2[0-4]\\d|25[0-5])){2}",
            /* 09 */ "(?:\\.(?:[1-9]\\d?|1\\d\\d|2[0-4]\\d|25[0-4]))",
            /* 10 */ "|",
            // host name
            /* 11 */ "(?:(?:[a-z\\u00a1-\\uffff0-9]-*)*[a-z\\u00a1-\\uffff0-9]+)",
            // domain name
            /* 12 */ "(?:\\.(?:[a-z\\u00a1-\\uffff0-9]-*)*[a-z\\u00a1-\\uffff0-9]+)*",
            // TLD identifier
            /* 13 */ "(?:\\.(?:[a-z\\u00a1-\\uffff]{2,}))",
            // TLD may end with dot
            /* 14 */ "\\.?",
            /* 15 */ ")",
            // port number
            /* 16 */ "(?::\\d{2,5})?",
            // resource path
            /* 17 */ "(?:[/?#]\\S*)?",
            /* 18 */ "$"
    };
    static Pattern urlPattern = Pattern.compile(String.join("", regexFragments), Pattern.CASE_INSENSITIVE);

    @Override
    public void configure(URL url) {
        setMessage(url.message());
        this.tldMandatory = url.tldMandatory();
        this.excludeLoopback = url.excludeLoopback();
    }

    @Override
    public boolean isSatisfied(Object validatedObject, Object value, OValContext context, Validator validator) {
        if (value == null || value.toString().length() == 0) {
            return true;
        }
        if (!tldMandatory || !excludeLoopback) {
            //slow for special cases
            String[] localRegexFragments = new String[regexFragments.length];
            System.arraycopy(regexFragments, 0, localRegexFragments, 0, regexFragments.length);
            if (!excludeLoopback) localRegexFragments[4] = "(?!(?:10)(?:\\.\\d{1,3}){3})";
            if (!tldMandatory) localRegexFragments[13] = "";
            Pattern localUrlPattern = Pattern.compile(String.join("", localRegexFragments), Pattern.CASE_INSENSITIVE);
            return localUrlPattern.matcher(value.toString()).matches();
        } else {
            return urlPattern.matcher(value.toString()).matches();
        }
    }

}
