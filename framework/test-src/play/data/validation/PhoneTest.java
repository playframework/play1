package play.data.validation;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import play.data.validation.Validation.ValidationResult;
import play.i18n.MessagesBuilder;

public class PhoneTest {

    @Test
    public void validPhoneValidationTest() {
        new MessagesBuilder().build();
        Validation.current.set(new Validation());

        // US
        assertValidPhone(true, "(305) 613 09 58 ext 101");
        assertValidPhone(true, "(305) 613 09 58 extension 101");
        // France
        assertValidPhone(true, "+33 (0)147376224");
        assertValidPhone(true, "+33 0147376224");
        assertValidPhone(true, "+33 1 47 37 62 24 x3");
        assertValidPhone(true, "+33 (0)1 47 37 62 24 x3");
        assertValidPhone(true, "+33 0147376224 x3");
        // Germany
        assertValidPhone(true, "+49-4312 / 777 777");
        assertValidPhone(true, "+49-4312  /  777 777");
        // China
        assertValidPhone(true, "+86 (10)69445464");
        assertValidPhone(true, "+86 (00)69445464");

        // UK
        assertValidPhone(true, "(020) 1234 1234");
    }

    @Test
    public void invalidPhoneValidationTest() {
        new MessagesBuilder().build();
        Validation.current.set(new Validation());

        assertValidPhone(false, "(305) abc 09 58 ext 101");
        // France
        assertValidPhone(false, "+330147376224");
        assertValidPhone(false, "+33(0)147376224");
        assertValidPhone(false, "+33  (0)1 47 37 62 24 x3");

        // China
        assertValidPhone(false, "+86(10)69445464");
        assertValidPhone(false, "+86(0)69445464");

        assertValidPhone(false, "+1229 343433345");
    }

    public void assertValidPhone(Boolean valid, String phone) {
        Validation.clear();
        ValidationResult result = Validation.phone("phone", phone);
        assertEquals("Validation phone [" + phone + "] should be " + valid, valid, result.ok);
    }

}
