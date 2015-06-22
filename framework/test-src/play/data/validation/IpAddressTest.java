package play.data.validation;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import play.data.validation.Validation.ValidationResult;
import play.i18n.MessagesBuilder;

public class IpAddressTest {

    @Test
    public void validIpV4AddressValidationTest() {
        new MessagesBuilder().build();
        Validation.current.set(new Validation());

        assertValidIpV4Address(true, "192.168.10.2");
        assertValidIpV4Address(true, "0.0.0.0");

        assertValidIpV4Address(true, "255.255.255.255");
        assertValidIpV4Address(true, "192.168.010.002");

    }

    @Test
    public void invalidIpV4AddressValidationTest() {
        new MessagesBuilder().build();
        Validation.current.set(new Validation());

        // With letters
        assertValidIpV4Address(false, "192.168.o10.002");
        assertValidIpV4Address(false, "(305) abc 09 58 ext 101");
        assertValidIpV4Address(false, "+330147376224");
        assertValidIpV4Address(false, "+33(0)147376224");

        // with other separator
        assertValidIpV4Address(false, "192,168.10.2");
        assertValidIpV4Address(false, "192.168,10.2");
        assertValidIpV4Address(false, "192.168.10,2");

        assertValidIpV4Address(false, "192.168.10-2.2");

        assertValidIpV4Address(false, "192-168.10.2");
        assertValidIpV4Address(false, "192.168-10.2");
        assertValidIpV4Address(false, "192.168.10-2");

        assertValidIpV4Address(false, "192_168.10.2");
        assertValidIpV4Address(false, "192.168_10.2");
        assertValidIpV4Address(false, "192.168.10_2");

        assertValidIpV4Address(false, "192/168.10.2");
        assertValidIpV4Address(false, "192.168/10.2");
        assertValidIpV4Address(false, "192.168.10/2");

        assertValidIpV4Address(false, "192+168.10.2");
        assertValidIpV4Address(false, "192.168+10.2");
        assertValidIpV4Address(false, "192.168.10+2");

        assertValidIpV4Address(false, "192*168.10.2");
        assertValidIpV4Address(false, "192.168*10.2");
        assertValidIpV4Address(false, "192.168.10*2");

        assertValidIpV4Address(false, "+192.168.10.2");
        assertValidIpV4Address(false, "192.+168.10.2");
        assertValidIpV4Address(false, "192.168.+10.2");
        assertValidIpV4Address(false, "192.168.10.+2");
        assertValidIpV4Address(false, "192.168.10.2+");

        // over 255
        assertValidIpV4Address(false, "256.255.255.255");
        assertValidIpV4Address(false, "255.256.255.255");
        assertValidIpV4Address(false, "255.255.256.255");
        assertValidIpV4Address(false, "255.255.255.256");
        assertValidIpV4Address(false, "1192.168.10.2");
        assertValidIpV4Address(false, "192.1168.10.2");
        assertValidIpV4Address(false, "192.168.1110.2");
        assertValidIpV4Address(false, "192.168.10.1112");

        // With space
        assertValidIpV4Address(false, " 192.168.10.2");
        assertValidIpV4Address(false, "192.168.10.2 ");
        assertValidIpV4Address(false, "192. 168.10.2");
        assertValidIpV4Address(false, "192.168 .10.2");

        // With negative number
        assertValidIpV4Address(false, "-192.168.10.2");
        assertValidIpV4Address(false, "-92.168.10.2");
        assertValidIpV4Address(false, "92.168.10.-2");

        // With additional character
        assertValidIpV4Address(false, "...");
        assertValidIpV4Address(false, "192..168.2");
        assertValidIpV4Address(false, "192..168.10.2");
        assertValidIpV4Address(false, "192.168..10.2");
        assertValidIpV4Address(false, "192.168.10..2");

        assertValidIpV4Address(false, "192.168.10.2.");
        assertValidIpV4Address(false, ".192.168.10.2");
        assertValidIpV4Address(false, "192.168.10.2+");
        assertValidIpV4Address(false, "192.168.10.2-");
        assertValidIpV4Address(false, "192.168.10.2/");
        assertValidIpV4Address(false, "192.168.10.2*");

        assertValidIpV4Address(false, "+192.168.10.2");
        assertValidIpV4Address(false, "-192.168.10.2");
        assertValidIpV4Address(false, "/192.168.10.2");
        assertValidIpV4Address(false, "*192.168.10.2");

        assertValidIpV4Address(false, ".192.168.10.2");
        assertValidIpV4Address(false, "255.255.255.255.");

    }

    public void assertValidIpV4Address(Boolean valid, String ipAddress) {
        Validation.clear();
        ValidationResult result = Validation.ipv4Address("phone", ipAddress);
        assertEquals("Validation ipv4Address [" + ipAddress + "] should be " + valid, valid, result.ok);
    }

}
