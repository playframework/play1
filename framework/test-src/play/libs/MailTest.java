package play.libs;


import java.util.concurrent.Future;

import org.apache.commons.mail2.jakarta.Email;
import org.apache.commons.mail2.core.EmailException;
import org.apache.commons.mail2.jakarta.SimpleEmail;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import play.PlayBuilder;
import play.exceptions.MailException;
import play.libs.mail.MailSystem;
import play.utils.ImmediateFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class MailTest {

    private static class SpyingMailSystem implements MailSystem {
        public Email receivedEmail = null;

        @Override
        public Future<Boolean> sendMessage(Email email) {
            receivedEmail = email;
            return new ImmediateFuture();
        }
    }

    private Email simpleEmail;
    private SpyingMailSystem spyingMailSystem;

    @BeforeEach
    public void initializeFixture() throws Exception {
        new PlayBuilder().build();

        simpleEmail =
                new SimpleEmail()
                        .setFrom("from@playframework.com")
                        .addTo("to@playframework.com")
                        .setSubject("subject");

        spyingMailSystem = new SpyingMailSystem();
    }

    @Test
    public void buildMessageWithoutFrom() {
        assertThrows(MailException.class, () -> {
            Email emailWithoutFrom = new SimpleEmail();
            emailWithoutFrom.addTo("from@playframework.com");
            emailWithoutFrom.setSubject("subject");
            Mail.buildMessage(new SimpleEmail());
        });
    }

    @Test()
    public void buildMessageWithoutRecipient() {
        assertThrows(MailException.class, () -> {
            Email emailWithoutRecipients =
                    new SimpleEmail()
                            .setFrom("from@playframework.com")
                            .setSubject("subject");
            Mail.buildMessage(emailWithoutRecipients);
        });
    }

    @Test
    public void buildMessageWithoutSubject() {
        assertThrows(MailException.class, () -> {
            Email emailWithoutSubject = new SimpleEmail();
            emailWithoutSubject.setFrom("from@playframework.com");
            emailWithoutSubject.addTo("to@playframework.com");
            Mail.buildMessage(emailWithoutSubject);
        });
    }

    @Test
    public void buildValidMessages() throws EmailException {
        Mail.buildMessage(
                emailWithoutRecipients().addTo("to@playframework.com"));
        Mail.buildMessage(
                emailWithoutRecipients().addCc("cc@playframework.com"));
        Mail.buildMessage(
                emailWithoutRecipients().addBcc("bcc@playframework.com"));
    }

    private Email emailWithoutRecipients() throws EmailException {
        return
                new SimpleEmail()
                        .setFrom("from@playframework.com")
                        .setSubject("subject");
    }

    @Test
    public void mailSystemShouldBeSubstitutable() throws Exception {
        Mail.useMailSystem(spyingMailSystem);

        Mail.send(simpleEmail);

        assertEquals(simpleEmail, spyingMailSystem.receivedEmail);
    }

}
