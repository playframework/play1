package play.data.validation;

import net.sf.oval.Validator;
import net.sf.oval.configuration.annotation.AbstractAnnotationCheck;
import net.sf.oval.context.OValContext;
import net.sf.oval.exception.OValException;

import org.apache.commons.lang.StringUtils;

public class IPv4AddressCheck extends AbstractAnnotationCheck<IPv4Address> {

    static final String mes = "validation.ipv4";

    @Override
    public void configure(IPv4Address ipv4Address) {
        setMessage(ipv4Address.message());
    }

    @Override
    public boolean isSatisfied(Object validatedObject, Object value, OValContext context, Validator validator) throws OValException {
        if (value == null || value.toString().length() == 0) {
            return true;
        }
        try {
            String[] parts = value.toString().split("[.]");
            // Check that there is no trailing separator
            if (parts.length != 4 || StringUtils.countMatches(value.toString(), ".") != 3) {
                return false;
            }

            for (int i = 0; i < parts.length; i++) {
                // Check that we don't have empty part or (+-) sign
                if (parts[i].isEmpty() || !parts[i].matches("[0-9]{1,3}")) {
                    return false;
                }
                int p = Integer.valueOf(parts[i]);
                if (p < 0 || p > 255) {
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

}
