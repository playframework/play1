package play.libs;

import java.io.File;
import java.util.Date;
import java.util.Properties;
import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import play.Logger;
import play.Play;
import play.exceptions.MailException;

/**
 * Mail utils
 */
public class Mail {

    private static Session session;

    /**
     * Send an email
     * @param from From address
     * @param recipient To address
     * @param subject Subject
     * @param body Body
     */
    public static void send(String from, String recipient, String subject, String body) {
        send(from, new String[]{recipient}, subject, body, "text/plain", new File[0]);
    }

    /**
     * Send an email
     * @param from From address
     * @param recipient To address
     * @param subject Subject
     * @param body Body
     * @param contentType The content type (text/plain or text/html)
     */
    public static void send(String from, String recipient, String subject, String body, String contentType) {
        send(from, new String[]{recipient}, subject, body, contentType, new File[0]);
    }

    /**
     * Send an email
     * @param from From address
     * @param recipients To addresses
     * @param subject Subject
     * @param body Body
     */
    public static void send(String from, String[] recipients, String subject, String body) {
        send(from, recipients, subject, body, "text/plain", new File[0]);
    }

    /**
     * Send an email
     * @param from From address
     * @param recipients To addresses
     * @param subject Subject
     * @param body Body
     * @param contentType The content type (text/plain or text/html)
     */
    public static void send(String from, String[] recipients, String subject, String body, String contentType) {
        send(from, recipients, subject, body, contentType, new File[0]);
    }

    /**
     * Send an email
     * @param from From address
     * @param recipient To address
     * @param subject Subject
     * @param body Body
     * @param attachments File attachments
     */
    public static void send(String from, String recipient, String subject, String body, File... attachments) {
        send(from, new String[]{recipient}, subject, body, "text/plain", attachments);
    }

    /**
     * Send an email
     * @param from From address
     * @param recipient To address
     * @param subject Subject
     * @param body Body
     * @param contentType The content type (text/plain or text/html)
     * @param attachments File attachments
     */
    public static void send(String from, String recipient, String subject, String body, String contentType, File... attachments) {
        send(from, new String[]{recipient}, subject, body, contentType, attachments);
    }

    /**
     * Send an email
     * @param from From address
     * @param recipients To addresses
     * @param subject Subject
     * @param body Body
     * @param attachments File attachments
     */
    public static void send(String from, String[] recipients, String subject, String body, File... attachments) {
        send(from, recipients, subject, body, "text/plain", attachments);
    }

    /**
     * Send an email
     * @param from From address
     * @param recipients To addresses
     * @param subject Subject
     * @param body Body
     * @param contentType The content type (text/plain or text/html)
     * @param attachments File attachments
     */
    public static void send(String from, String[] recipients, String subject, String body, String contentType, File... attachments) {
        try {
            MimeMessage msg = new MimeMessage(getSession());
            
            if(from == null) {
                from = Play.configuration.getProperty("mail.smtp.from");
            }
            if(from == null) {
                throw new MailException("Please define a 'from' email address", new NullPointerException());
            }
            if(recipients == null || recipients.length == 0) {
                throw new MailException("Please define a recipient email address", new NullPointerException());
            }
            if(subject == null) {
                throw new MailException("Please define a subject", new NullPointerException());
            }
            
            if(contentType == null) {
                contentType = "text/plain";
            }

            InternetAddress addressFrom = new InternetAddress(from);
            msg.setFrom(addressFrom);

            InternetAddress[] addressTo = new InternetAddress[recipients.length];
            for (int i = 0; i < recipients.length; i++) {
                addressTo[i] = new InternetAddress(recipients[i]);
            }
            msg.setRecipients(javax.mail.Message.RecipientType.TO, addressTo);

            msg.setSubject(subject, "utf-8");

            Multipart mp = new MimeMultipart();
            MimeBodyPart bodyPart = new MimeBodyPart();

            bodyPart.setContent(body, contentType + "; charset=utf-8");
            mp.addBodyPart(bodyPart);

            handleAttachments(mp, attachments);

            msg.setContent(mp);

            sendMessage(msg);
        } catch (MessagingException ex) {
            throw new MailException("Cannot send email", ex);
        }
    }

    private static Session getSession() {
        if (session == null || (Play.mode == Play.Mode.DEV)) {
            Properties props = new Properties();
            props.put("mail.smtp.host", Play.configuration.getProperty("mail.smtp.host"));
            props.put("mail.smtp.starttls.enable", Play.configuration.getProperty("mail.smtp.protocol", "stmp").equals("smtps") ? "true" : "false");
            session = Session.getDefaultInstance(props, null);
        }
        return session;
    }

    private static void handleAttachments(Multipart mp, File... attachments) {
        if (attachments != null) {
            for (File attachment : attachments) {
                try {
                    MimeBodyPart part = new MimeBodyPart();
                    FileDataSource fds = new FileDataSource(attachment);
                    part.setDataHandler(new DataHandler(fds));
                    part.setFileName(fds.getName());
                    mp.addBodyPart(part);
                } catch (MessagingException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    /**
     * Send a JavaMail message
     * @param msg A JavaMail message
     */
    public static void sendMessage(final Message msg) {
        new Thread() {
            @Override
            public void run() {
                try {
                    msg.setSentDate(new Date());
                    Transport transport = getSession().getTransport("smtp");
                    transport.connect(getSession().getProperty("mail.smtp.host"), Play.configuration.getProperty("mail.smtp.user"), Play.configuration.getProperty("mail.smtp.pass"));
                    transport.sendMessage(msg, msg.getAllRecipients());
                    transport.close();
                } catch(Exception e) {
                    MailException me = new MailException("Error while sending email", e);
                    Logger.error(me, "The email has not been sent");
                }
            }
        }.start();
    }
}