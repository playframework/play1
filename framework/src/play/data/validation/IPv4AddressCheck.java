package play.data.validation;

import org.apache.commons.lang3.StringUtils;

public class IPv4AddressCheck extends AbstractAnnotationCheck<IPv4Address> {

    static final String mes = "validation.ipv4";

    @Override
    public void configure(IPv4Address ipv4Address) {
        setMessage(ipv4Address.message());
    }

    @Override
    public boolean isSatisfied(Object validatedObject, Object value, PlayValidationContext context) {
        if (value == null || value.toString().length() == 0) {
            return true;
        }
        try {
            String[] parts = value.toString().split("[.]");
            // Check that there is no trailing separator
            if (parts.length != 4 || StringUtils.countMatches(value.toString(), ".") != 3) {
                return false;
            }

            for (String part : parts) {
                // Check that we don't have empty part or (+-) sign
                if (part.isEmpty() || !part.matches("[0-9]{1,3}")) {
                    return false;
                }
                int p = Integer.parseInt(part);
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
