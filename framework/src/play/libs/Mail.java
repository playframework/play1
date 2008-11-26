package play.libs;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.mail.Header;
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

public class Mail {
    
    private static Session session;

    /**
     * 
     * @param from email which will be shown to the receiver
     * @param recipient email of the recipient
     * @param params contains "subject", "body", "charset" and "contentType"
     */
    public static void send(String from, String recipient, Map<String, Object> params) {
        send(from, recipient, params, new File[0]);
    }

    /**
     * 
     * @param from email which will be shown to the receiver
     * @param recipient email of the recipient
     * @param params contains "subject", "body", "charset" and "contentType"
     * @param attachments files to attach
     */
    public static void send(String from, String recipient, Map<String, Object> params, File... attachments) {
        String[] recipients = {recipient};
        send(from, recipients, params, attachments);
    }

    
    /**
     * 
     * @param from email which will be shown to the receiver
     * @param recipients emails of the recipients
     * @param params contains "subject", "body", "charset" and "contentType"
     */
    public static void send(String from, String[] recipients, Map<String, Object> params) {
        send(from, recipients, params, new File[0]);
    }

    /**
     * 
     * @param from email which will be shown to the receiver
     * @param recipients emails of the recipients
     * @param params contains "subject", "body", "charset" and "contentType"
     * @param attachments files to attach
     */
    public static void send(String from, String[] recipients, Map<String, Object> params, File... attachments) {
        try {
            Message msg = new MimeMessage(getSession());

            InternetAddress addressFrom = new InternetAddress(from);
            msg.setFrom(addressFrom);

            InternetAddress[] addressTo = new InternetAddress[recipients.length];
            for (int i = 0; i < recipients.length; i++) {
                addressTo[i] = new InternetAddress(recipients[i]);
            }
            msg.setRecipients(javax.mail.Message.RecipientType.TO, addressTo);

            msg.setSubject(params.containsKey("subject") ? params.get("subject").toString() : "");


            Multipart mp = new MimeMultipart();

            handleContent(mp, params);
            handleAttachments(mp, attachments);

            msg.setContent(mp);

            sendMessage(msg);
        } catch (MessagingException ex) {
            Logger.error("An error occurred while processing mail");
        }
    }

    private static Session getSession() {
        if (session == null || (Play.mode == Play.Mode.DEV)) {
            Properties props = new Properties();
            props.put("mail.smtp.host", Play.configuration.getProperty("mail.smtp.host"));
            props.put("mail.smtp.starttls.enable", Play.configuration.getProperty("mail.smtp.starttls"));
            session = Session.getDefaultInstance(props, null);
        }
        return session;
    }
    
    private static void handleContent(Multipart mp, Map<String, Object> params) {
        try {
            String charset = (params.containsKey("charset")) ? (String) params.get("charset") : "utf-8";
            String contentType = (params.containsKey("contentType")) ? (String) params.get("contentType") : "text/html; charset=" + charset;
            MimeBodyPart body = new MimeBodyPart();
            String bodyContent = (params.containsKey("body")) ? params.get("body").toString() : "";
            body.setContent(bodyContent, contentType);
            mp.addBodyPart(body);
        } catch (MessagingException ex) {
            ex.printStackTrace();
        }
    }
    
    private static void handleAttachments(Multipart mp, File ... attachments) {
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
    
    public synchronized static void sendMessage(Message msg) {
        try {          
            msg.setSentDate(new Date());
            Transport transport = getSession().getTransport("smtp");
            transport.connect(getSession().getProperty("mail.smtp.host"), Play.configuration.getProperty("mail.smtp.user"), Play.configuration.getProperty("mail.smtp.pass"));
            transport.sendMessage(msg, msg.getAllRecipients());
            transport.close();
        } catch (MessagingException ex) {
            java.util.logging.Logger.getLogger(Mail.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}