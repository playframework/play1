package play.libs.mail;

import java.util.concurrent.Future;

import org.apache.commons.mail.Email;

public interface MailSystem {

    Future<Boolean> sendMessage(Email email);

}
