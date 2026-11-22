package play.data.validation;

import java.util.regex.Pattern;

public class URLCheck extends AbstractAnnotationCheck<URL> {

    static final String mes = "validation.url";
    static final Pattern urlPattern = Pattern.compile("^(http|https|ftp)\\://[a-zA-Z0-9\\-\\.]+\\.[a-z" +
            "A-Z]{2,3}(:[a-zA-Z0-9]*)?/?([a-zA-Z0-9\\-\\._\\?\\,\\'/\\\\\\+&amp;%\\$#\\=~\\!])*$");

    @Override
    public void configure(URL url) {
        setMessage(url.message());
    }

    @Override
    public boolean isSatisfied(Object validatedObject, Object value, PlayValidationContext context) {
        if (value == null || value.toString().length() == 0) {
            return true;
        }
        return urlPattern.matcher(value.toString()).matches();
    }

}
