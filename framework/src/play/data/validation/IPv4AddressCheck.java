package play.data.validation;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.regex.Pattern;

import net.sf.oval.Validator;
import net.sf.oval.configuration.annotation.AbstractAnnotationCheck;
import net.sf.oval.context.OValContext;
import net.sf.oval.exception.OValException;

public class IPv4AddressCheck extends AbstractAnnotationCheck<IPv4Address> {

	final static String mes = "validation.ipv4";

	@Override
	public void configure(IPv4Address phone) {
		setMessage(phone.message());
	}

	@Override
	public boolean isSatisfied(Object validatedObject, Object value, OValContext context, Validator validator)
			throws OValException {
		if (value == null || value.toString().length() == 0) {
			return true;
		}
		try {
			InetAddress addr = InetAddress.getByName(value.toString());
			return addr instanceof Inet4Address;
		} catch (UnknownHostException e) {
			return false;
		}
	}

}