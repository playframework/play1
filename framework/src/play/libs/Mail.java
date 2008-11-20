package play.libs;

import java.io.File;
import java.util.Date;
import java.util.Map;
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
import play.templates.Template;
import play.templates.TemplateLoader;

public class Mail {
    
    private static Session session;

    /**
     * 
     * @param templateName name of the template to be used. can be null
     * @param from email which will be shown to the receiver
     * @param recipient email of the recipient
     * @param params contains "subject", "body", "charset" and "contentType"
     */
    public static void send(String templateName, String from, String recipient, Map<String, Object> params) {
        send(templateName, from, recipient, params);
    }

    /**
     * 
     * @param templateName name of the template to be used. can be null
     * @param from email which will be shown to the receiver
     * @param recipient email of the recipient
     * @param params contains "subject", "body", "charset" and "contentType"
     * @param attachments files to attach
     */
    public static void send(String templateName, String from, String recipient, Map<String, Object> params, File... attachments) {
        String[] recipients = {recipient};
        send(templateName, from, recipients, params, attachments);
    }

    
    /**
     * 
     * @param templateName name of the template to be used. can be null
     * @param from email which will be shown to the receiver
     * @param recipients emails of the recipients
     * @param params contains "subject", "body", "charset" and "contentType"
     */
    public static void send(String templateName, String from, String[] recipients, Map<String, Object> params) {
        send(templateName, from, recipients, params);
    }

    /**
     * 
     * @param templateName name of the template to be used. can be null
     * @param from email which will be shown to the receiver
     * @param recipients emails of the recipients
     * @param params contains "subject", "body", "charset" and "contentType"
     * @param attachments files to attach
     */
    public static void send(String templateName, String from, String[] recipients, Map<String, Object> params, File... attachments) {
        try {
            Message msg = new MimeMessage(getSession());

            InternetAddress addressFrom = new InternetAddress(from);
            msg.setFrom(addressFrom);

            InternetAddress[] addressTo = new InternetAddress[recipients.length];
            for (int i = 0; i < recipients.length; i++) {
                addressTo[i] = new InternetAddress(recipients[i]);
            }
            msg.setRecipients(javax.mail.Message.RecipientType.TO, addressTo);
            msg.setSubject(params.get("subject").toString());

            Multipart mp = new MimeMultipart();

            handleContent(mp, templateName, params);
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
    
    private static void handleContent(Multipart mp, String templateName, Map<String, Object> params) {
        try {
            String charset = (params.containsKey("charset")) ? (String) params.get("charset") : "utf-8";
            String contentType = (params.containsKey("contentType")) ? (String) params.get("contentType") : "text/html; charset=" + charset;
            MimeBodyPart body = new MimeBodyPart();
            if (params.get("body") != null) {
                body.setContent(params.get("body").toString(), contentType);
                mp.addBodyPart(body);
            } else {
                Template template = TemplateLoader.load(templateName);
                body.setContent(template.render(params), contentType);
                
                mp.addBodyPart(body);
            }
        } catch (MessagingException ex) {
            Logger.error("An error occurred while processing mail content");
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
                    Logger.error("An error occurred while processing mail attachments");
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
            Logger.error("An error occurred while sending mail");
        }
    }
}