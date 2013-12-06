package play.data.validation;

import java.util.regex.Pattern;
import net.sf.oval.Validator;
import net.sf.oval.configuration.annotation.AbstractAnnotationCheck;
import net.sf.oval.context.OValContext;

@SuppressWarnings("serial")
public class URLCheck extends AbstractAnnotationCheck<URL> {

    final static String mes = "validation.url";    
      static  String  urlRegex =   "^((https|http|ftp|rtsp|mms)?://)?(([0-9a-z_!~*'().&=+$%-]+: )?[0-9a-z_!~*'().&=+$%-]+@)?(([0-9]{1,3}.){3}[0-9]{1,3}|([0-9a-z_!~*'()-]+.)*([0-9a-z][0-9a-z-]{0,61})?[0-9a-z].[a-z]{2,6})(:[0-9]{1,4})?((/?)|(/[0-9a-z_!~*'().;?:@&=+$,%#-]+)+/?)$";

    static Pattern urlPattern = Pattern.compile(urlRegex);

    @Override
    public void configure(URL url) {
        setMessage(url.message());
    }

    public boolean isSatisfied(Object validatedObject, Object value, OValContext context, Validator validator) {
        if (value == null || value.toString().length() == 0) {
            return false;
        }
        return urlPattern.matcher(value.toString()).matches();
    }

}
