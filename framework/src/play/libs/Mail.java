package play.libs;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
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

import org.apache.commons.lang.StringUtils;
import play.Logger;
import play.Play;
import play.exceptions.MailException;
import play.exceptions.UnexpectedException;

/**
 * Mail utils
 */
public class Mail {

    public static Session session;
    public static boolean asynchronousSend = true;

    /**
     * Send an email in plain text
     * @param from From address
     * @param recipient To address
     * @param subject Subject
     * @param body Body
     */
    public static Future<Boolean> send(String from, String recipient, String subject, String body) {
        return send(from, new String[]{recipient}, subject, body, null, "text/plain", new Object[0]);
    }

    /**
     * Send an email in text/html with a text/plain alternative
     * @param from From address
     * @param recipient To address
     * @param subject Subject
     * @param body text/html body content
     * @param alternate text/plain alternative content (optional)
     */
    public static Future<Boolean> send(String from, String recipient, String subject, String body, String alternate) {
        return send(from, new String[]{recipient}, subject, body, alternate, "text/html", new Object[0]);
    }

    /**
     * Send an email in text/html with a text/plain alternative and attachments
     * @param from From address
     * @param recipient To address
     * @param subject Subject
     * @param body text/html body content
     * @param alternate text/plain alternative content (optional)
     * @param obj the attachments to the email
     */
    public static Future<Boolean> send(String from, String recipient, String subject, String body, String alternate, Object... obj) {
        return send(from, new String[]{recipient}, subject, body, alternate, "text/html", obj);
    }

    /**
     * Send an email in text/plain format
     * @param from From address
     * @param recipients To addresses
     * @param subject Subject
     * @param body The text/plain body of the email
     */
    public static Future<Boolean> send(String from, String[] recipients, String subject, String body) {
        return send(from, recipients, subject, body, null, "text/plain", new Object[0]);
    }

    /**
     * Send an email in text/plain
     * @param from From address
     * @param recipient To address
     * @param subject Subject
     * @param body plain/text body of the email
     * @param attachments File attachments
     */
    public static Future<Boolean> send(String from, String recipient, String subject, String body, Object... attachments) {
        return send(from, new String[]{recipient}, subject, body, null, "text/plain", attachments);
    }


    /**
     * Send an email in text/plain
     * @param from From address
     * @param recipients To addresses
     * @param subject Subject
     * @param body Body
     * @param attachments File attachments
     */
    public static Future<Boolean> send(String from, String[] recipients, String subject, String body, Object... attachments) {
        return send(from, null, recipients, subject, body, null, "text/plain", attachments);
    }

    
    /**
     * Send an email
     * @param from From address
     * @param replyTo ReplyTo address
     * @param recipients To addresses
     * @param subject Subject
     * @param body body of the email
     * @param alternate text/plain body (optional). This parameter is ignored if contentType is set to text/plain or is null.
     * @param contentType The content type of the body (text/plain or text/html)
     * @param attachments File attachments
     */
    public static Future<Boolean> send(Object from, Object replyTo, Object[] recipients, String subject, String body, String alternate, String contentType, Object... attachments) {
        try {
            if (from == null) {
                from = Play.configuration.getProperty("mail.smtp.from", "user@localhost");
            }
            if (replyTo == null) {
                replyTo = from;
            }
            if (Play.configuration.getProperty("mail.smtp", "").equals("mock") && Play.mode == Play.Mode.DEV) {
                Mock.send(from, replyTo, recipients, subject, body, alternate, contentType, attachments);
                return new Future<Boolean>() {

                    public boolean cancel(boolean mayInterruptIfRunning) {
                        return false;
                    }

                    public boolean isCancelled() {
                        return false;
                    }

                    public boolean isDone() {
                        return true;
                    }

                    public Boolean get() throws InterruptedException, ExecutionException {
                        return true;
                    }

                    public Boolean get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
                        return true;
                    }
                };
            }
            return sendMessage(buildMessage(from, replyTo, recipients, subject, body, alternate, contentType, attachments));
        } catch (MessagingException ex) {
            throw new MailException("Cannot send email", ex);
        }
    }

    /**
     * Construct a MimeMessage
     * @param from From address
     * @param recipients To addresses
     * @param subject Subject
     * @param body body of the email
     * @param alternate text/plain body (optional). This parameter is ignored if contentType is set to text/plain or is null.
     * @param contentType The content type of the body (text/plain or text/html) (optional)
     * @param attachments File attachments
     */
    public static MimeMessage buildMessage(Object from, Object replyTo, Object[] recipients, String subject, String body, String alternate, String contentType, Object... attachments) throws MessagingException {
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

        msg.setFrom(from instanceof InternetAddress ? (InternetAddress) from : new InternetAddress(from.toString()));
        InternetAddress reply;
        if (replyTo == null) {
            reply = from instanceof InternetAddress ? (InternetAddress) from : new InternetAddress(from.toString());
        } else {
            reply = replyTo instanceof InternetAddress ? (InternetAddress) replyTo : new InternetAddress(replyTo.toString());
        }
        msg.setReplyTo(new InternetAddress[]{reply});

        InternetAddress[] addressTo = new InternetAddress[recipients.length];
        for (int i = 0; i < recipients.length; i++) {
            addressTo[i] = recipients[i] instanceof InternetAddress ? (InternetAddress) recipients[i] : new InternetAddress(recipients[i].toString());
        }
        msg.setRecipients(javax.mail.Message.RecipientType.TO, addressTo);

        msg.setSubject(subject, "utf-8");
        if ("text/plain".equals(contentType)) {
            msg.setText(body);
            if (attachments != null && attachments.length > 0) {
                Multipart mp = new MimeMultipart();
                handleAttachments(mp, attachments);
                msg.setContent(mp);
            }
        } else {

            if (attachments != null && attachments.length > 0) {

                Multipart mixed = new MimeMultipart("mixed");

                Multipart mp = getMultipart(body, alternate, contentType);

                // Create a body part to house the multipart/alternative Part
                MimeBodyPart contentPartRoot = new MimeBodyPart();
                contentPartRoot.setContent(mp);

                mixed.addBodyPart(contentPartRoot);

                // Add an attachment
                handleAttachments(mixed, attachments);

                msg.setContent(mixed);
            } else {

                msg.setContent(getMultipart(body, alternate, contentType));
            }

        }
        return msg;
    }

    private static Multipart getMultipart(String body, String alternate, String contentType) throws MessagingException {
        Multipart mp = new MimeMultipart("alternative");

        if (!StringUtils.isEmpty(alternate)) {
            MimeBodyPart alternatePart = new MimeBodyPart();
            alternatePart.setContent(alternate, "text/plain; charset=utf-8");
            mp.addBodyPart(alternatePart);
        }

        MimeBodyPart bodyPart = new MimeBodyPart();
        bodyPart.setContent(body, contentType + "; charset=utf-8");
        mp.addBodyPart(bodyPart);

        return mp;
    }

    private static Session getSession() {
        if (session == null) {
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
                props.put("mail.smtp.socketFactory.class", "play.utils.YesSSLSocketFactory");
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
                part.setContentID(Codec.UUID() + datasource.getName());
                mp.addBodyPart(part);
            }
        }
    }

    /**
     * Send a JavaMail message
     * @param msg A JavaMail message
     */
    public static Future<Boolean> sendMessage(final Message msg) {
        if (asynchronousSend) {
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
        } else {
            final StringBuffer result = new StringBuffer();
            try {
                msg.setSentDate(new Date());
                Transport.send(msg);
            } catch (Throwable e) {
                MailException me = new MailException("Error while sending email", e);
                Logger.error(me, "The email has not been sent");
                result.append("oops");
            }
            return new Future<Boolean>() {

                public boolean cancel(boolean mayInterruptIfRunning) {
                    return false;
                }

                public boolean isCancelled() {
                    return false;
                }

                public boolean isDone() {
                    return true;
                }

                public Boolean get() throws InterruptedException, ExecutionException {
                    return result.length() == 0;
                }

                public Boolean get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
                    return result.length() == 0;
                }
            };
        }
    }
    static ExecutorService executor = Executors.newCachedThreadPool();

    public static class SMTPAuthenticator extends Authenticator {

        private String user;
        private String password;

        public SMTPAuthenticator(String user, String password) {
            this.user = user;
            this.password = password;
        }

        @Override
        protected PasswordAuthentication getPasswordAuthentication() {
            return new PasswordAuthentication(user, password);
        }
    }

    public static class Mock {

        static Map<String, String> emails = new HashMap();

        static void send(Object from, Object replyTo, Object[] recipients, String subject, String body, String alternate, String contentType, Object... attachments) {
            StringBuffer email = new StringBuffer();
            email.append("From Mock Mailer\n\tNew email received by");
            for (Object add : recipients) {
                email.append(", " + (add instanceof InternetAddress ? ((InternetAddress) add).toString() : add.toString()));
            }
            email.append("\n\tFrom: " + (from instanceof InternetAddress ? ((InternetAddress) from).toString() : from.toString()));
            email.append("\n\tReplyTo: " + (replyTo instanceof InternetAddress ? ((InternetAddress) replyTo).toString() : replyTo.toString()));
            email.append("\n\tSubject: " + subject);
            email.append("\n\tAttachments: " + attachments.length);
            email.append("\n\tBody(" + contentType + "): " + body);
            if (!StringUtils.isEmpty(alternate)) {
                email.append("\n\tAlternate Body(text/plain): " + alternate);
            }
            email.append("\n");
            Logger.info(email.toString());
            for (Object add : recipients) {
                emails.put((add instanceof InternetAddress ? ((InternetAddress) add).getAddress() : add.toString()), email.toString());
            }
        }

        public static String getLastMessageReceivedBy(String email) {
            return emails.get(email);
        }
    }
}
