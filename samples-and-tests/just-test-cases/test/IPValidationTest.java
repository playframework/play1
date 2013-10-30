import org.junit.Test;

import play.data.validation.IPv4AddressCheck;
import play.data.validation.IPv6AddressCheck;
import play.test.UnitTest;

public class IPValidationTest extends UnitTest {

	@Test
	public void ipv4() {
		assertTrue(testv4("0.125.212.5", true));
		assertTrue(testv4("999.524.251.55", false));
		assertTrue(testv4("192.167.1.2222", false));
		assertTrue(testv4("1922.167.1.222", false));
		assertTrue(testv4("092.167.1.222", true));
		assertTrue(testv4("192..1.222", false));
		assertTrue(testv4("392.3.1.222", false));
		assertTrue(testv4("127.0.0.1", true));
		assertTrue(testv4("192.167.1.2", true));
		assertTrue(testv4("32.3.222", false));
		assertTrue(testv4("32.3.1.222.32", false));
	}

	@Test
	public void ipv6() {
		assertTrue(testv6("3ffe:1900:4545:3:200:f8ff:fe21:67cf", true));
		assertTrue(testv6("fe80::200:f8ff:fe21:67cf", true));
		assertTrue(testv6("fe80:0:0:0:200:f8ff:fe21:67cf", true));

	}

	private boolean testv4(String addr, boolean expected) {
		IPv4AddressCheck check = new IPv4AddressCheck();
		return expected == check.isSatisfied(null, addr, null, null);
	}

	private boolean testv6(String addr, boolean expected) {
		IPv6AddressCheck check = new IPv6AddressCheck();
		return expected == check.isSatisfied(null, addr, null, null);
	}
}
