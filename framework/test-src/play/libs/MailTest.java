package play.libs;

import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.SimpleEmail;
import org.junit.Test;

import play.PlayBuilder;
import play.exceptions.MailException;

public class MailTest {

	@Test(expected = MailException.class)
	public void buildMessageWithoutFrom() throws EmailException {
		new PlayBuilder().build();

		Email email = new SimpleEmail();
		email.addTo("from@playframework.org");
		email.setSubject("subject");
		Mail.buildMessage(new SimpleEmail());
	}

	@Test(expected = MailException.class)
	public void buildMessageWithoutRecipient() throws EmailException {
		new PlayBuilder().build();

		Email email = new SimpleEmail();
		email.setFrom("from@playframework.org");
		email.setSubject("subject");
		Mail.buildMessage(email);
	}

	@Test(expected = MailException.class)
	public void buildMessageWithoutSubject() throws EmailException {
		new PlayBuilder().build();

		Email email = new SimpleEmail();
		email.setFrom("from@playframework.org");
		email.addTo("to@playframework.org");
		Mail.buildMessage(email);
	}

	@Test
	public void buildValidMessages() throws EmailException {
		new PlayBuilder().build();

		Email email = new SimpleEmail();
		email.setFrom("from@playframework.org");
		email.addTo("to@playframework.org");
		email.setSubject("subject");
		Mail.buildMessage(email);

		email = new SimpleEmail();
		email.setFrom("from@playframework.org");
		email.addCc("to@playframework.org");
		email.setSubject("subject");
		Mail.buildMessage(email);

		email = new SimpleEmail();
		email.setFrom("from@playframework.org");
		email.addBcc("to@playframework.org");
		email.setSubject("subject");
		Mail.buildMessage(email);
	}
}
