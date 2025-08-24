package play.libs.mail;

import java.util.concurrent.Future;

import org.apache.commons.mail2.jakarta.Email;

import play.libs.Mail;

class ProductionMailSystem implements MailSystem {

    @Override
    public Future<Boolean> sendMessage(Email email) {
        email.setMailSession(Mail.getSession());
        return Mail.sendMessage(email);
    }

}
