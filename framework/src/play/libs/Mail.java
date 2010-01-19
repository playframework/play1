package play.libs;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
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
import javax.mail.*;
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
     *
     * @param from      From address. Can be of the form xxx <m@m.com>
     * @param recipient To address. Can be of the form xxx <m@m.com>
     * @param subject   Subject
     * @param body      Body
     */
    public static Future<Boolean> send(String from, String recipient, String subject, String body) {
        return send(from, null, new String[]{recipient}, subject, body, null, "text/plain", new File[0]);
    }

    /**
     * Send an email in text/html with a text/plain alternative
     *
     * @param from      From address. Can be of the form xxx <m@m.com>
     * @param recipient To address. Can be of the form xxx <m@m.com>
     * @param subject   Subject
     * @param body      text/html body content
     * @param alternate text/plain alternative content (optional)
     */
    public static Future<Boolean> send(String from, String recipient, String subject, String body, String alternate) {
        return send(from, null, new String[]{recipient}, subject, body, alternate, "text/html", new File[0]);
    }

    /**
     * Send an email in text/html with a text/plain alternative and attachments
     *
     * @param from        From address. Can be of the form xxx <m@m.com>
     * @param recipient   To address. Can be of the form xxx <m@m.com>
     * @param subject     Subject
     * @param body        text/html body content
     * @param alternate   text/plain alternative content (optional)
     * @param attachments File attachments
     */
    public static Future<Boolean> send(String from, String recipient, String subject, String body, String alternate, File... attachments) {
        return send(from, null, new String[]{recipient}, subject, body, alternate, "text/html", (Object[]) attachments);
    }

    /**
     * Send an email in text/html with a text/plain alternative and attachments
     *
     * @param from        From address. Can be of the form xxx <m@m.com>
     * @param recipient   To address. Can be of the form xxx <m@m.com>
     * @param subject     Subject
     * @param body        text/html body content
     * @param alternate   text/plain alternative content (optional)
     * @param attachments DataSource attachments
     */
    public static Future<Boolean> send(String from, String recipient, String subject, String body, String alternate, DataSource... attachments) {
        return send(from, null, new String[]{recipient}, subject, body, alternate, "text/html", (Object[]) attachments);
    }

    /**
     * Send an email in text/plain format
     *
     * @param from       From address. Can be of the form xxx <m@m.com>
     * @param recipients To addresses. Can be of the form xxx <m@m.com>
     * @param subject    Subject
     * @param body       The text/plain body of the email
     */
    public static Future<Boolean> send(String from, String[] recipients, String subject, String body) {
        return send(from, recipients, subject, body, new File[0]);
    }

    /**
     * Send an email in text/plain
     *
     * @param from        From address. Can be of the form xxx <m@m.com>
     * @param recipient   To address. Can be of the form xxx <m@m.com>
     * @param subject     Subject
     * @param body        plain/text body of the email
     * @param attachments File attachments
     */
    public static Future<Boolean> send(String from, String recipient, String subject, String body, File... attachments) {
        return send(from, new String[]{recipient}, subject, body, (File[]) attachments);
    }

    /**
     * Send an email in text/plain
     *
     * @param from        From address. Can be of the form xxx <m@m.com>
     * @param recipient   To address. Can be of the form xxx <m@m.com>
     * @param subject     Subject
     * @param body        plain/text body of the email
     * @param attachments DataSource attachments
     */
    public static Future<Boolean> send(String from, String recipient, String subject, String body, DataSource... attachments) {
        return send(from, new String[]{recipient}, subject, body, (DataSource[]) attachments);
    }

    /**
     * Send an email in text/plain
     *
     * @param from        From address Can be of the form xxx <m@m.com>
     * @param recipients  To addresses Can be of the form xxx <m@m.com>
     * @param subject     Subject
     * @param body        Body
     * @param attachments File attachments
     */
    public static Future<Boolean> send(String from, String[] recipients, String subject, String body, File... attachments) {
        return send(from, null, recipients, subject, body, null, "text/plain", (Object[]) attachments);
    }

    /**
     * Send an email in text/plain
     *
     * @param from        From address Can be of the form xxx <m@m.com>
     * @param recipients  To addresses Can be of the form xxx <m@m.com>
     * @param subject     Subject
     * @param body        Body
     * @param attachments DataSource attachments
     */
    public static Future<Boolean> send(String from, String[] recipients, String subject, String body, DataSource... attachments) {
        return send(from, null, recipients, subject, body, null, "text/plain", (Object[]) attachments);
    }

    /**
     * Send an email
     *
     * @param from        From address. Can be of the form xxx <m@m.com>
     * @param replyTo     ReplyTo address Can be of the form xxx <m@m.com>
     * @param recipients  To addresses
     * @param subject     Subject
     * @param body        body of the email
     * @param alternate   text/plain body (optional). This parameter is ignored if contentType is set to text/plain or is null.
     * @param contentType The content type of the body (text/plain or text/html)
     * @param attachments File attachments
     */
    public static Future<Boolean> send(String from, String replyTo, String[] recipients, String subject, String body, String alternate, String contentType, File... attachments) {
        return sendEmail(from, replyTo, recipients, subject, body, alternate, contentType, null, null, (Object[]) attachments);
    }

    /**
     * Send an email
     *
     * @param from        From address. Can be of the form xxx <m@m.com>
     * @param replyTo     ReplyTo address Can be of the form xxx <m@m.com>
     * @param recipients  To addresses
     * @param subject     Subject
     * @param body        body of the email
     * @param alternate   text/plain body (optional). This parameter is ignored if contentType is set to text/plain or is null.
     * @param contentType The content type of the body (text/plain or text/html)
     * @param attachments DataSource attachments
     */
    public static Future<Boolean> send(String from, String replyTo, String[] recipients, String subject, String body, String alternate, String contentType, DataSource... attachments) {
        return sendEmail(from, replyTo, recipients, subject, body, alternate, contentType, null, null, (Object[]) attachments);
    }

    /**
     * @param from        From address a String or an InternetAddress
     * @param replyTo     ReplyTo address  a String or an InternetAddress
     * @param recipients  To addresses  an Array of String or/and  InternetAddress
     * @param subject     Subject
     * @param body        body of the email
     * @param alternate   text/plain body (optional). This parameter is ignored if contentType is set to text/plain or is null.
     * @param contentType The content type of the body (text/plain or text/html)
     * @param attachments File attachments
     * @Deprecated Send an email
     */
    public static Future<Boolean> send(Object from, Object replyTo, Object[] recipients, String subject, String body, String alternate, String contentType, Object... attachments) {
        return sendEmail(from, replyTo, recipients, subject, body, alternate, contentType, null, null, (Object[]) attachments);
    }


    /**
     * Send an email
     *
     * @param from        From address a String or an InternetAddress
     * @param replyTo     ReplyTo address  a String or an InternetAddress
     * @param recipients  To addresses  an Array of String or/and  InternetAddress
     * @param subject     Subject
     * @param body        body of the email
     * @param alternate   text/plain body (optional). This parameter is ignored if contentType is set to text/plain or is null.
     * @param contentType The content type of the body (text/plain or text/html)
     * @param charset     The character set of the message (optional)
     * @param headers     The mail headers (optional)
     * @param attachments File or DataSource attachments
     */
    public static Future<Boolean> sendEmail(Object from, Object replyTo, Object[] recipients, String subject, String body, String alternate, String contentType, String charset, Map<String, String> headers, Object... attachments) {
        try {
            InternetAddress fromI = null;
            if (from != null) {
                fromI = new InternetAddress(from.toString());
            }
            InternetAddress replyToI = null;
            if (replyTo != null) {
                replyToI = new InternetAddress(replyTo.toString());
            }
            if (fromI == null) {
                fromI = new InternetAddress(Play.configuration.getProperty("mail.smtp.from", "user@localhost"));
            }
            if (replyToI == null) {
                replyToI = fromI;
            }
            InternetAddress[] recipientsI = new InternetAddress[recipients.length];
            for (int i = 0; i < recipients.length; i++) {
                if (recipients[i] instanceof InternetAddress) {
                    recipientsI[i] = (InternetAddress) recipients[i];
                } else {
                    recipientsI[i] = new InternetAddress(recipients[i].toString());
                }
            }

            if (Play.configuration.getProperty("mail.smtp", "").equals("mock") && Play.mode == Play.Mode.DEV) {
                Mock.send(buildMimeMessage(fromI, replyToI, recipientsI, subject, body, alternate, contentType, charset, headers == null ? new HashMap<String, String>() : headers, true, (Object[]) attachments));
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
            return sendMessage(buildMimeMessage(fromI, replyToI, recipientsI, subject, body, alternate, contentType, charset, headers == null ? new HashMap<String, String>() : headers, (Object[]) attachments));
        } catch (MessagingException ex) {
            throw new MailException("Cannot send email", ex);
        }
    }

    /**
     * Original method from the 1.0 release.
     *
     * @param from        From address a String or an InternetAddress
     * @param replyTo     ReplyTo address  a String or an InternetAddress
     * @param recipients  To addresses  an Array of String or/and  InternetAddress
     * @param subject
     * @param body
     * @param alternate
     * @param contentType
     * @param attachments
     * @return
     * @throws MessagingException
     * @Deprecated
     */
    public static MimeMessage buildMessage(Object from, Object replyTo, Object[] recipients, String subject, String body, String alternate, String contentType, Object... attachments) throws MessagingException {
        String[] addressTo = null;
        int i = 0;
        if (recipients != null) {
            addressTo = new String[recipients.length];
            for (Object recipient : recipients) {
                addressTo[i++] = recipient.toString();
            }
        }
        return buildMimeMessage(from != null ? from.toString() : (String) null, replyTo != null ? replyTo.toString() : (String) null, addressTo, subject, body, alternate, contentType, (String) null, new HashMap<String, String>(), (Object[]) attachments);
    }

    /**
     * @param from
     * @param replyTo
     * @param recipients
     * @param subject
     * @param body
     * @param alternate
     * @param contentType
     * @param attachments
     * @return
     * @throws MessagingException
     */
    public static MimeMessage buildMimeMessage(String from, String replyTo, String[] recipients, String subject, String body, String alternate, String contentType, Object... attachments) throws MessagingException {
        InternetAddress fromI = null;
        if (from != null) {
            fromI = new InternetAddress(from.toString());
        }
        InternetAddress replyToI = null;
        if (replyTo != null) {
            replyToI = new InternetAddress(replyTo.toString());
        }
        InternetAddress[] addressTo = null;
        int i = 0;
        if (recipients != null) {
            addressTo = new InternetAddress[recipients.length];
            for (Object recipient : recipients) {
                addressTo[i++] = new InternetAddress(recipient.toString());
            }
        }
        return buildMimeMessage(fromI, replyToI, addressTo, subject, body, alternate, contentType, null, new HashMap<String, String>(), (Object[]) attachments);
    }

    /**
     * Construct a MimeMessage
     *
     * @param from        From address
     * @param recipients  To addresses
     * @param subject     Subject
     * @param body        body of the email
     * @param alternate   text/plain body (optional). This parameter is ignored if contentType is set to text/plain or is null.
     * @param contentType The content type of the body (text/plain or text/html) (optional)
     * @param attachments File attachments
     */
    public static MimeMessage buildMimeMessage(InternetAddress from, InternetAddress replyTo, InternetAddress[] recipients, String subject, String body, String alternate, String contentType, Object... attachments) throws MessagingException {
        return buildMessage(from, replyTo, recipients, subject, body, alternate, contentType, null, new HashMap<String, String>(), (Object[]) attachments);
    }


    /**
     * Construct a MimeMessage
     *
     * @param from        From address
     * @param recipients  To addresses
     * @param subject     Subject
     * @param body        body of the email
     * @param alternate   text/plain body (optional). This parameter is ignored if contentType is set to text/plain or is null.
     * @param contentType The content type of the body (text/plain or text/html) (optional)
     * @param charset     The character set of the message (optional)
     * @param headers     The mail headers (optional)
     * @param attachments File or DataSource attachments
     */
    public static MimeMessage buildMimeMessage(InternetAddress from, InternetAddress replyTo, InternetAddress[] recipients, String subject, String body, String alternate, String contentType, String charset, Map<String, String> headers, Object... attachments) throws MessagingException {
        return buildMimeMessage(from, replyTo, recipients, subject, body, alternate, contentType, charset, headers, false, attachments);
    }


    private static MimeMessage buildMimeMessage(InternetAddress from, InternetAddress replyTo, InternetAddress[] recipients, String subject, String body, String alternate, String contentType, String charset, Map<String, String> headers, boolean mock, Object... attachments) throws MessagingException {

        MimeMessage msg = new MimeMessage(getSession(mock));

        if (from == null) {
            from = new InternetAddress(Play.configuration.getProperty("mail.smtp.from"));
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

        msg.setFrom(from);

        if (replyTo == null) {
            replyTo = from;
        }
        msg.setReplyTo(new InternetAddress[]{replyTo});
        msg.setRecipients(javax.mail.Message.RecipientType.TO, recipients);

        msg.setSubject(subject, charset != null ? charset : "utf-8");
        if ("text/plain".equals(contentType)) {
            msg.setText(body, charset != null ? charset : "utf-8");
            if (attachments != null && attachments.length > 0) {
                Multipart mp = new MimeMultipart();
                handleAttachments(mp, attachments);
                msg.setContent(mp);
            }
        } else {

            if (attachments != null && attachments.length > 0) {

                Multipart mixed = new MimeMultipart("mixed");

                Multipart mp = getMultipart(body, alternate, contentType, charset);

                // Create a body part to house the multipart/alternative Part
                MimeBodyPart contentPartRoot = new MimeBodyPart();
                contentPartRoot.setContent(mp);

                mixed.addBodyPart(contentPartRoot);

                // Add an attachment
                handleAttachments(mixed, attachments);

                msg.setContent(mixed);
            } else {

                msg.setContent(getMultipart(body, alternate, contentType, charset));
            }

        }

        // Apparently addHeaders must be after msg.setText, because setText will overwrite the "Content-Transfer-Encoding" header. Otherwise even if we write,
        // addHeader("Content-Transfer-Encoding", "7bit"); the header will be, "Content-Transfer-Encoding: quoted-printable". .
        msg = addHeaders(msg, headers);

        return msg;
    }

    protected static MimeMessage addHeaders(MimeMessage msg, Map<String, String> headers) throws MessagingException {
        if (headers != null && headers.size() > 0) {
            Iterator<String> it = headers.keySet().iterator();
            while (it.hasNext()) {
                String name = it.next();
                msg.setHeader(name, headers.get(name));
            }
        }
        return msg;
    }

    protected static Multipart getMultipart(String body, String alternate, String contentType, String charset) throws MessagingException {
        Multipart mp = new MimeMultipart("alternative");

        if (!StringUtils.isEmpty(alternate)) {
            MimeBodyPart alternatePart = new MimeBodyPart();
            alternatePart.setContent(alternate, "text/plain; charset=" + (charset != null ? charset : "utf-8"));
            mp.addBodyPart(alternatePart);
        }

        MimeBodyPart bodyPart = new MimeBodyPart();
        bodyPart.setContent(body, contentType + "; charset=" + (charset != null ? charset : "utf-8"));
        mp.addBodyPart(bodyPart);

        return mp;
    }

    private static Session getSession(boolean mock) {
        if (session == null) {
            Properties props = new Properties();
            if (!mock) {
                String smtpServer = Play.configuration.getProperty("mail.smtp.host");
                if (smtpServer == null) {
                    throw new MailException("Please configure your smtp server using the mail.smtp.host property in your application.conf file.");
                }

                props.put("mail.smtp.host", smtpServer);
            } else {
                props.put("mail.smtp.host", "myfakesmtpserver.com");
            }

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
                    if (attachment != null) {
                        throw new UnexpectedException(attachment.getClass().getName() + " type is not supported as attachement.");
                    } else {
                        throw new UnexpectedException("an attachment cannot be null.");
                    }
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
     *
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

        public static String getContent(Part message) throws MessagingException,
                IOException {

            if (message.getContent() instanceof String) {
                return message.getContentType() + ": " + (String) message.getContent() + " \n\t";
            } else if (message.getContent() != null && message.getContent() instanceof Multipart) {
                Multipart part = (Multipart) message.getContent();
                String text = "";
                for (int i = 0; i < part.getCount(); i++) {
                    BodyPart bodyPart = part.getBodyPart(i);
                    if (!Message.ATTACHMENT.equals(bodyPart.getDisposition())) {
                        text += getContent(part.getBodyPart(i));
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

        static void send(MimeMessage email) throws MessagingException {

            try {
                final StringBuffer content = new StringBuffer();
                Properties props = new Properties();
                props.put("mail.smtp.host", "myfakesmtpserver.com");

                final String body = getContent(email);

                content.append("From Mock Mailer\n\tNew email received by");


                content.append("\n\tFrom: " + ((InternetAddress)email.getFrom()[0]).getAddress());
                content.append("\n\tReplyTo: " + ((InternetAddress)email.getReplyTo()[0]).getAddress());
                content.append("\n\tSubject: " + email.getSubject());
                content.append("\n\t" + body);

                content.append("\n");
                Logger.info(content.toString());

                for (Object add : email.getAllRecipients()) {
                    content.append(", " + add.toString());
                    emails.put(((InternetAddress) add).getAddress(), content.toString());
                }

            } catch (Exception e) {
                Logger.error(e, "error sending mock email");
            }

        }

        static void send(Object from, Object replyTo, Object[] recipients, String subject, String body, String alternate, String contentType, Object... attachments) {
            StringBuffer email = new StringBuffer();
            email.append("From Mock Mailer\n\tNew email received by");
            for (Object add : recipients) {
                email.append(", " + (add instanceof InternetAddress ? ((InternetAddress) add).toString() : add.toString()));
            }
            email.append("\n\tFrom: " + (from instanceof InternetAddress ? ((InternetAddress) from).toString() : from.toString()));
            email.append("\n\tReplyTo: " + (replyTo instanceof InternetAddress ? ((InternetAddress) replyTo).toString() : replyTo.toString()));
            email.append("\n\tSubject: " + subject);
            if (attachments != null && attachments.length > 0) {
                email.append("\n\tAttachments length: " + attachments.length);
                for (Object attachment : attachments) {
                    email.append("\n\tAttachment: " + attachment);
                }
            }
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
