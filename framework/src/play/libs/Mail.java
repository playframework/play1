package play.libs;

import java.io.File;
import java.util.Date;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import play.Logger;
import play.Play;
import play.exceptions.MailException;
import play.exceptions.UnexpectedException;

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
    public static Future<Boolean> send(String from, String recipient, String subject, String body) {
        return send(from, new String[]{recipient}, subject, body, "text/plain", new Object[0]);
    }

    /**
     * Send an email
     * @param from From address
     * @param recipient To address
     * @param subject Subject
     * @param body Body
     * @param contentType The content type (text/plain or text/html)
     */
    public static Future<Boolean> send(String from, String recipient, String subject, String body, String contentType) {
        return send(from, new String[]{recipient}, subject, body, contentType, new Object[0]);
    }

    /**
     * Send an email
     * @param from From address
     * @param recipients To addresses
     * @param subject Subject
     * @param body Body
     */
    public static Future<Boolean> send(String from, String[] recipients, String subject, String body) {
        return send(from, recipients, subject, body, "text/plain", new Object[0]);
    }

    /**
     * Send an email
     * @param from From address
     * @param recipients To addresses
     * @param subject Subject
     * @param body Body
     * @param contentType The content type (text/plain or text/html)
     */
    public static Future<Boolean> send(String from, String[] recipients, String subject, String body, String contentType) {
        return send(from, recipients, subject, body, contentType, new Object[0]);
    }

    /**
     * Send an email
     * @param from From address
     * @param recipient To address
     * @param subject Subject
     * @param body Body
     * @param attachments File attachments
     */
    public static Future<Boolean> send(String from, String recipient, String subject, String body, Object... attachments) {
        return send(from, new String[]{recipient}, subject, body, "text/plain", attachments);
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
    public static Future<Boolean> send(String from, String recipient, String subject, String body, String contentType, Object... attachments) {
        return send(from, new String[]{recipient}, subject, body, contentType, attachments);
    }

    /**
     * Send an email
     * @param from From address
     * @param recipients To addresses
     * @param subject Subject
     * @param body Body
     * @param attachments File attachments
     */
    public static Future<Boolean> send(String from, String[] recipients, String subject, String body, Object... attachments) {
        return send(from, recipients, subject, body, "text/plain", attachments);
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
    public static Future<Boolean> send(String from, String[] recipients, String subject, String body, String contentType, Object... attachments) {
        try {
            return sendMessage(buildMessage(from, from, recipients, subject, body, contentType, attachments));
        } catch (MessagingException ex) {
            throw new MailException("Cannot send email", ex);
        }
    }
    
        /**
     * Send an email
     * @param from From address
       @param replyTo ReplyTo address
     * @param recipients To addresses
     * @param subject Subject
     * @param body Body
     * @param contentType The content type (text/plain or text/html)
     * @param attachments File attachments
     */
    public static Future<Boolean> send(String from, String replyTo, String[] recipients, String subject, String body, String contentType, Object... attachments) {
        try {
            return sendMessage(buildMessage(from, replyTo, recipients, subject, body, contentType, attachments));
        } catch (MessagingException ex) {
            throw new MailException("Cannot send email", ex);
        }
    }

    /**
     * Construct a MimeMessage
     * @param from From address
     * @param recipients To addresses
     * @param subject Subject
     * @param body Body
     * @param contentType The content type (text/plain or text/html)
     * @param attachments File attachments
     */
    public static MimeMessage buildMessage(String from, String replyTo, String[] recipients, String subject, String body, String contentType, Object... attachments) throws MessagingException {
        MimeMessage msg = new MimeMessage(getSession());

        if (from == null) {
            from = Play.configuration.getProperty("mail.smtp.from");
        }
        if (from == null) {
            throw new MailException("Please define a 'from' email address", new NullPointerException());
        }
        if (recipients == null || recipients.length == 0) {
            throw new MailException("Please define a recipient email address", new NullPointerException());
        }
        if (subject == null) {
            throw new MailException("Please define a subject", new NullPointerException());
        }

        if (contentType == null) {
            contentType = "text/plain";
        }

        msg.setFrom( new InternetAddress(from));
        msg.setReplyTo(new InternetAddress[]{new InternetAddress(replyTo == null ? from : replyTo)});

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

        return msg;
    }

    private static Session getSession() {
        if (session == null || (Play.mode == Play.Mode.DEV)) {
            Properties props = new Properties();
            props.put("mail.smtp.host", Play.configuration.getProperty("mail.smtp.host"));

            String channelEncryption = "clear";
            if (Play.configuration.containsKey("mail.smtp.protocol") && Play.configuration.getProperty("mail.smtp.protocol", "smtp").equals("smtps")) {
                // Backward compatibility before stable5
                channelEncryption = "starttls";
            } else {
                channelEncryption = Play.configuration.getProperty("mail.smtp.channel", "clear");
            }

            if (channelEncryption.equals("clear")) {
                props.put("mail.smtp.port", "25");
            } else if (channelEncryption.equals("ssl")) {
                // port 465 + setup yes ssl socket factory (won't verify that the server certificate is signed with a root ca.)
                props.put("mail.smtp.port", "465");
                props.put("mail.smtp.socketFactory.port", "465");
                props.put("mail.smtp.socketFactory.class", "play.libs.YesSSLSocketFactory");
                props.put("mail.smtp.socketFactory.fallback", "false");
            } else if (channelEncryption.equals("starttls")) {
                // port 25 + enable starttls + ssl socket factory
                props.put("mail.smtp.port", "25");
                props.put("mail.smtp.starttls.enable", "true");
            // can't install our socket factory. will work only with server that has a signed certificate
            // story to be continued in javamail 1.4.2 : https://glassfish.dev.java.net/issues/show_bug.cgi?id=5189
            }

            if (Play.configuration.containsKey("mail.smtp.localhost")) {
                props.put("mail.smtp.localhost", Play.configuration.get("mail.smtp.localhost"));            //override defaults
            }
            if (Play.configuration.containsKey("mail.smtp.socketFactory.class")) {
                props.put("mail.smtp.socketFactory.class", Play.configuration.get("mail.smtp.socketFactory.class"));
            }
            if (Play.configuration.containsKey("mail.smtp.port")) {
                props.put("mail.smtp.port", Play.configuration.get("mail.smtp.port"));
            }
            String user = Play.configuration.getProperty("mail.smtp.user");
            String password = Play.configuration.getProperty("mail.smtp.pass");
            if (password == null) {
                // Fallback to old convention
                password = Play.configuration.getProperty("mail.smtp.password");
            }
            String authenticator = Play.configuration.getProperty("mail.smtp.authenticator");
            session = null;

            if (authenticator != null) {
                props.put("mail.smtp.auth", "true");
                try {
                    session = Session.getInstance(props, (Authenticator) Play.classloader.loadClass(authenticator).newInstance());
                } catch (Exception e) {
                    Logger.error(e, "Cannot instanciate custom SMTP authenticator (%s)", authenticator);
                }
            }

            if (session == null) {
                if (user != null && password != null) {
                    props.put("mail.smtp.auth", "true");
                    session = Session.getInstance(props, new SMTPAuthenticator(user, password));
                } else {
                    props.remove("mail.smtp.auth");
                    session = Session.getInstance(props);
                }
            }

            if (Boolean.parseBoolean(Play.configuration.getProperty("mail.debug", "false"))) {
                session.setDebug(true);
            }
        }
        return session;
    }

    private static void handleAttachments(Multipart mp, Object... attachments) throws MessagingException {
        if (attachments != null) {
            for (Object attachment : attachments) {
                DataSource datasource = null;
                if (attachment instanceof File) {
                    datasource = new FileDataSource((File) attachment);
                } else if (attachment instanceof DataSource) {
                    datasource = (DataSource) attachment;
                } else {
                    throw new UnexpectedException(attachment.getClass().getName() + " type is not supported as attachement.");
                }
                MimeBodyPart part = new MimeBodyPart();
                part.setDataHandler(new DataHandler(datasource));
                part.setFileName(datasource.getName());
                part.setContentID(datasource.getName());
                mp.addBodyPart(part);
            }
        }
    }

    /**
     * Send a JavaMail message
     * @param msg A JavaMail message
     */
    public static Future<Boolean> sendMessage(final Message msg) {
        return executor.submit(new Callable<Boolean>() {

            public Boolean call() {
                try {
                    msg.setSentDate(new Date());
                    Transport.send(msg);
                    return true;
                } catch (Throwable e) {
                    MailException me = new MailException("Error while sending email", e);
                    Logger.error(me, "The email has not been sent");
                    return false;
                }
            }
        });
    }
    static ExecutorService executor = Executors.newCachedThreadPool();

    public static class SMTPAuthenticator extends Authenticator {

        private String user;
        private String password;

        public SMTPAuthenticator(String user, String password) {
            this.user = user;
            this.password = password;
        }

        protected PasswordAuthentication getPasswordAuthentication() {
            return new PasswordAuthentication(user, password);
        }
    }
}