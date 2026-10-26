package play.mvc;

import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.SimpleEmail;
import org.junit.Test;

import java.util.HashMap;

import static org.junit.Assert.assertEquals;

public class MailerTest {
    static class MailInitializer extends Mailer {
        /**
         * The method annotated with @Before seems to be executed in a separate thread from the test method.
         * This method initializes thread-local variables and must be called at the beginning of the test method.
         */
        public static void init() {
            infos.set(new HashMap<>());
        }
        public static void clear() {
            infos.remove();
        }
    }

    private static String exampleAddress = "user@example.com";
    private static String multibyteCharAddress = "Eva Nováková <eva@example.com>";
    private static String encodedAddress = "=?UTF-8?Q?Eva_Nov=C3=A1kov=C3=A1?= <eva@example.com>";

    @Test
    public void mailFromSupportsMultibyteCharAddress() throws EmailException {
        MailInitializer.init();
        try {
            Email email = new SimpleEmail();
            Mailer.addRecipient(exampleAddress);
            Mailer.setFrom(multibyteCharAddress);
            Mailer.setAddresses(email);
            assertEquals(encodedAddress, email.getFromAddress().toString());
        } finally {
            MailInitializer.clear();
        }
    }

    @Test
    public void mailReplyToSupportsMultibyteCharAddress() throws EmailException {
        MailInitializer.init();
        try {
            Email email = new SimpleEmail();
            Mailer.addRecipient(exampleAddress);
            Mailer.setReplyTo(multibyteCharAddress);
            Mailer.setAddresses(email);
            assertEquals(encodedAddress, email.getReplyToAddresses().get(0).toString());
        } finally {
            MailInitializer.clear();
        }
    }

    @Test
    public void mailToSupportsMultibyteCharAddress() throws EmailException {
        MailInitializer.init();
        try {
            Email email = new SimpleEmail();
            Mailer.addRecipient(multibyteCharAddress);
            Mailer.setAddresses(email);
            assertEquals(encodedAddress, email.getToAddresses().get(0).toString());
        } finally {
            MailInitializer.clear();
        }
    }

    @Test
    public void mailCcSupportsMultibyteCharAddress() throws EmailException {
        MailInitializer.init();
        try {
            Email email = new SimpleEmail();
            Mailer.addRecipient(exampleAddress);
            Mailer.addCc(multibyteCharAddress);
            Mailer.setAddresses(email);
            assertEquals(encodedAddress, email.getCcAddresses().get(0).toString());
        } finally {
            MailInitializer.clear();
        }
    }

    @Test
    public void mailBccSupportsMultibyteCharAddress() throws EmailException {
        MailInitializer.init();
        try {
            Email email = new SimpleEmail();
            Mailer.addRecipient(exampleAddress);
            Mailer.addBcc(multibyteCharAddress);
            Mailer.setAddresses(email);
            assertEquals(encodedAddress, email.getBccAddresses().get(0).toString());
        } finally {
            MailInitializer.clear();
        }
    }
}
