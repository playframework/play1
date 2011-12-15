package play.libs;

import static org.junit.Assert.*;

import java.util.concurrent.Future;

import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.SimpleEmail;
import org.junit.Before;
import org.junit.Test;

import play.PlayBuilder;
import play.exceptions.MailException;
import play.libs.mail.MailSystem;

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

	@Before
	public void initializeFixture() throws Exception {
		new PlayBuilder().build();

		simpleEmail =
			new SimpleEmail()
				.setFrom("from@playframework.org")
				.addTo("to@playframework.org")
				.setSubject("subject");

		spyingMailSystem = new SpyingMailSystem();
	}

	@Test(expected = MailException.class)
	public void buildMessageWithoutFrom() throws EmailException {
		Email emailWithoutFrom = new SimpleEmail();
		emailWithoutFrom.addTo("from@playframework.org");
		emailWithoutFrom.setSubject("subject");
		Mail.buildMessage(new SimpleEmail());
	}

	@Test(expected = MailException.class)
	public void buildMessageWithoutRecipient() throws EmailException {
		Email emailWithoutRecipients =
			new SimpleEmail()
				.setFrom("from@playframework.org")
				.setSubject("subject");
		Mail.buildMessage(emailWithoutRecipients);
	}

	@Test(expected = MailException.class)
	public void buildMessageWithoutSubject() throws EmailException {
		Email emailWithoutSubject = new SimpleEmail();
		emailWithoutSubject.setFrom("from@playframework.org");
		emailWithoutSubject.addTo("to@playframework.org");
		Mail.buildMessage(emailWithoutSubject);
	}

	@Test
	public void buildValidMessages() throws EmailException {
		Mail.buildMessage(
			emailWitoutRecipients().addTo("to@playframework.org"));
		Mail.buildMessage(
			emailWitoutRecipients().addCc("cc@playframework.org"));
		Mail.buildMessage(
			emailWitoutRecipients().addBcc("bcc@playframework.org"));
	}

	protected Email emailWitoutRecipients() throws EmailException {
		return
			new SimpleEmail()
				.setFrom("from@playframework.org")
				.setSubject("subject");
	}

	@Test
	public void mailSystemShouldBeSubstitutable() throws Exception {
		Mail.useMailSystem(spyingMailSystem);

		Mail.send(simpleEmail);

		assertEquals(simpleEmail, spyingMailSystem.receivedEmail);
	}

}
