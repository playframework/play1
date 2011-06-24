import org.junit.Test;

import play.data.validation.PhoneCheck;
import play.test.UnitTest;

public class PhoneValidationTest extends UnitTest {

	@Test
	public void germany() {
		assertTrue(test("09009-1234567", true));
		assertTrue(test("040 7 97 5138", true));
		assertTrue(test("04106 7 99 63 - 7320", true));
		assertTrue(test("(01234) 123456", true));
		assertTrue(test("+49 (1234) 123456", true));
		assertTrue(test("+49 4312 / 777 777", true));
		assertTrue(test("+49-4312 / 777 777", true));
		assertTrue(test("+49-4312 / 777 777", true));
		assertTrue(test("04312 / 777 777", true));
		assertTrue(test("04312 - 777 777", true));
		assertTrue(test("(0431) 777 777", true));
	}

	@Test
	public void usa() {

		assertTrue(test("+1 (305) 613-0958 x101", true));
		assertTrue(test("3056130958", true));
		assertTrue(test("13056130958", true));
		assertTrue(test("(305) 613-0958 x101", true));
		assertTrue(test("1 305 613-0958", true));
		assertTrue(test("(305) 613 09 58 ext 101", true));
		assertTrue(test("305-552-2432 extension 12", true));
	}

	@Test
	public void france() {

		assertTrue(test("+33 1 47 37 62 24", true));

		assertTrue(test("01 47 37 6224", true));
		assertTrue(test("01.47.37.6224", true));
		assertTrue(test("01-47-37-62 24", true));
		assertTrue(test("+33 1 47 37 62 24x3", true));
		assertTrue(test("+33 1 47 37 62 24 x3", true));
		assertTrue(test("+33 1 47 37 62 24 x 3", true));
		assertTrue(test("+33 1 47 37 62 24 ext 3", true));
		assertTrue(test("+33 1 47 37 62 24 extension 3", true));
	}

	@Test
	public void china() {

		assertTrue(test("+86 (10)69445464", true));
		assertTrue(test("(10)69445464", true));
	}

	@Test
	public void nigeria() {
		assertTrue(test("+234 80312345", true));
		assertTrue(test("080312345", true));
	}

	@Test
	public void uk() {
		assertTrue(test("(016977) 1234", true));
		assertTrue(test("(020) 1234 1234", true));
		assertTrue(test("(0151) 1234 123", true));
		assertTrue(test("(01865)  123456", true));
	}

	@Test
	public void venezuella() {

		assertTrue(test("0212 5551212", true));
		assertTrue(test("(0295) 4167216", true));
		assertTrue(test("+58 295416 7216", true));
		assertTrue(test("+58 212 5551212", true));
	}

	@Test
	public void india() {
		assertTrue(test("+91-12345-12345", true));
	}

	@Test
	public void switzerland() {

		assertTrue(test("+41 12 123 12 12", true));
	}

	@Test
	public void shortcodes() {
		// short numbers - typically emergencies
		assertTrue(test("123", true));
		assertTrue(test("1234", true));
		assertTrue(test("12345", true));
	}

	@Test
	public void expectedInvalid() {
		// invalid stuff
		// country code too long (max 3 digits)
		assertTrue(test("+1229 343433345", false));
		assertTrue(test("+494312 / 777 777", false));
		// bad country code
		assertTrue(test("+a33 1 47 37 62 24 extension 3", false));
		assertTrue(test("+33a 1 47 37 62 24 extension 3", false));

		assertTrue(test("1", false));
		assertTrue(test("ext", false));
		assertTrue(test("()", false));
		assertTrue(test("+", false));
		assertTrue(test("+()", false));
		assertTrue(test("efwfefwf", false));

		// no space in sub zone
		assertTrue(test("(0169 77) 1234", false));
		// missing extension
		assertTrue(test("305-552-2432 extension", false));
		assertTrue(test("305-552-2432 barf", false));

		// missing parenthesis
		assertTrue(test("(0295 4167216", false));
	}

	private boolean test(String number, boolean expected) {
		PhoneCheck check = new PhoneCheck();
		return expected == check.isSatisfied(null, number, null, null);
	}

}
