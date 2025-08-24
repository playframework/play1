package play.libs.mail;

import java.util.concurrent.Future;

import org.apache.commons.mail2.jakarta.Email;

public interface MailSystem {

    Future<Boolean> sendMessage(Email email);

}
