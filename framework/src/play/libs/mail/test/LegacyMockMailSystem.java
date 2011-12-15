package play.libs.mail.test;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Future;

import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.mail.Email;

import play.Logger;
import play.libs.Mail;
import play.libs.mail.MailSystem;
import play.utils.ImmediateFuture;

/**
 * Just kept for compatibility reasons, use test double substitution mechanism instead.
 *
 * @see    Mail#Mock
 * @see    Mail#useMailSystem(MailSystem)
 * @author Andreas Simon <a.simon@quagilis.de>
 */
public class LegacyMockMailSystem implements MailSystem {

    // Has to remain static to preserve the possibility of testing mail sending within Selenium tests
    static Map<String, String> emails = new HashMap<String, String>();

    @Override
    public Future<Boolean> sendMessage(Email email) {
        try {
            final StringBuffer content = new StringBuffer();
            Properties props = new Properties();
            props.put("mail.smtp.host", "myfakesmtpserver.com");

            Session session = Session.getInstance(props);
            email.setMailSession(session);

            email.buildMimeMessage();

            MimeMessage msg = email.getMimeMessage();
            msg.saveChanges();

            String body = getContent(msg);

            content.append("From Mock Mailer\n\tNew email received by");


            content.append("\n\tFrom: " + email.getFromAddress().getAddress());
            content.append("\n\tReplyTo: " + ((InternetAddress) email.getReplyToAddresses().get(0)).getAddress());

            addAddresses(content, "To",  email.getToAddresses());
            addAddresses(content, "Cc",  email.getCcAddresses());
            addAddresses(content, "Bcc", email.getBccAddresses());

            content.append("\n\tSubject: " + email.getSubject());
            content.append("\n\t" + body);

            content.append("\n");
            Logger.info(content.toString());

            for (Object add : email.getToAddresses()) {
                content.append(", " + add.toString());
                emails.put(((InternetAddress) add).getAddress(), content.toString());
            }

        } catch (Exception e) {
            Logger.error(e, "error sending mock email");
        }
        return new ImmediateFuture();
    }


    private static String getContent(Part message) throws MessagingException,
            IOException {

        if (message.getContent() instanceof String) {
            return message.getContentType() + ": " + message.getContent() + " \n\t";
        } else if (message.getContent() != null && message.getContent() instanceof Multipart) {
            Multipart part = (Multipart) message.getContent();
            String text = "";
            for (int i = 0; i < part.getCount(); i++) {
                BodyPart bodyPart = part.getBodyPart(i);
                if (!Message.ATTACHMENT.equals(bodyPart.getDisposition())) {
                    text += getContent(bodyPart);
                } else {
                    text += "attachment: \n" +
                   "\t\t name: " + (StringUtils.isEmpty(bodyPart.getFileName()) ? "none" : bodyPart.getFileName()) + "\n" +
                   "\t\t disposition: " + bodyPart.getDisposition() + "\n" +
                   "\t\t description: " +  (StringUtils.isEmpty(bodyPart.getDescription()) ? "none" : bodyPart.getDescription())  + "\n\t";
                }
            }
            return text;
        }
        if (message.getContent() != null && message.getContent() instanceof Part) {
            if (!Message.ATTACHMENT.equals(message.getDisposition())) {
                return getContent((Part) message.getContent());
            } else {
                return "attachment: \n" +
                       "\t\t name: " + (StringUtils.isEmpty(message.getFileName()) ? "none" : message.getFileName()) + "\n" +
                       "\t\t disposition: " + message.getDisposition() + "\n" +
                       "\t\t description: " + (StringUtils.isEmpty(message.getDescription()) ? "none" : message.getDescription()) + "\n\t";
            }
        }

        return "";
    }


    private static void addAddresses(final StringBuffer content,
            String header, List<?> ccAddresses) {
        if (ccAddresses != null && !ccAddresses.isEmpty()) {
            content.append("\n\t" + header + ": ");
            for (Object add : ccAddresses) {
                content.append(add.toString() + ", ");
            }
            removeTheLastComma(content);
        }
    }

    private static void removeTheLastComma(final StringBuffer content) {
        content.delete(content.length() - 2, content.length());
    }


    public String getLastMessageReceivedBy(String email) {
        return emails.get(email);
    }

    public void reset(){
        emails.clear();
    }
}
