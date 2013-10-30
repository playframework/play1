package play.data.validation;

import net.sf.oval.Validator;
import net.sf.oval.configuration.annotation.AbstractAnnotationCheck;
import net.sf.oval.context.OValContext;
import net.sf.oval.exception.OValException;

public class IPv4AddressCheck extends AbstractAnnotationCheck<IPv4Address> {

    final static String mes = "validation.ipv4";

    @Override
    public void configure(IPv4Address ipv4Address) {
        setMessage(ipv4Address.message());
    }

    public boolean isSatisfied(Object validatedObject, Object value, OValContext context, Validator validator)
    throws OValException {
        if (value == null || value.toString().length() == 0) {
            return true;
        }
        try {
            String[] parts = value.toString().split("[.]");
            if (parts.length != 4) {
                return false;
            }
            
            for(int i=0; i<parts.length; i++) {
                int p = Integer.valueOf(parts[i]);
                if(p < 0 || p > 255) {
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

}
