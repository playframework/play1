package play.data.validation;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

import net.sf.oval.Validator;
import net.sf.oval.configuration.annotation.AbstractAnnotationCheck;
import net.sf.oval.context.OValContext;
import net.sf.oval.exception.OValException;

public class IPv6AddressCheck extends AbstractAnnotationCheck<IPv6Address> {

    final static String mes = "validation.ipv6";

    @Override
    public void configure(IPv6Address phone) {
        setMessage(phone.message());
    }

    public boolean isSatisfied(Object validatedObject, Object value, OValContext context, Validator validator)
    throws OValException {
        if (value == null || value.toString().length() == 0) {
            return true;
        }
        try {
            InetAddress addr = InetAddress.getByName(value.toString());
            return addr instanceof Inet6Address;
        } catch (UnknownHostException e) {
            return false;
        }
    }

}
