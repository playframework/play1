package cn.bran.play;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.mail.internet.InternetAddress;

import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailAttachment;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;
import org.apache.commons.mail.MultiPartEmail;

import play.Logger;
import play.Play;
import play.exceptions.MailException;
import play.exceptions.UnexpectedException;
import play.libs.Mail;
import cn.bran.japid.template.JapidTemplateBaseWithoutPlay;
import cn.bran.japid.template.RenderResult;
import cn.bran.japid.util.DirUtil;
import cn.bran.japid.util.StackTraceUtils;
import cn.bran.japid.util.StringUtils;

/**
 *  Application mailer support, based on the Mail class in the Play distribution.
 * 
 *  Only html format is support for now. 
 *  
 *  This mailer is supposed to be used with JapidController2. 
 *  
 *  The default template path is : /japidroot/japidviews/_notifiers/{custom mailer package}/{custom mailer class}/{method name}.html
 */
public class JapidMailer2 extends JapidMailer {
    @SuppressWarnings("unchecked")
    public static Future<Boolean> send(Object... args) {
        try {
            final HashMap<String, Object> infoMap = getInfoMap();

            // Body character set
            final String charset = (String) infoMap.get(CHARSET);

            // Headers
            final Map<String, String> headers = (Map<String, String>) infoMap.get(HEADERS);

            // Subject
            final String subject = (String) infoMap.get(SUBJECT);

            // xxx how to determine the method name???
//            String templateName = (String) infoMap.get(METHOD);
            String templateNameBase = StackTraceUtils.getCaller();
            if (!templateNameBase.startsWith("notifiers")) {
            	throw new RuntimeException("The emailers must be put in the \"notifiers\" package.");
            }

            String templateClassName = DirUtil.JAPIDVIEWS_ROOT + "._" + templateNameBase;
            
            String bodyHtml = null;
            Class<? extends JapidTemplateBaseWithoutPlay> tClass = JapidPlayRenderer.getTemplateClass(templateClassName);//Play.classloader.getClassIgnoreCase(templateClassName);
    		if (tClass == null) {
    			String templateFileName = templateClassName.replace('.', '/') + ".html";
    			throw new RuntimeException("Japid Emailer: could not find a Japid template with the name of: " + templateFileName);
    		} else {
    			try {
					JapidController2.render(tClass, args);
				} catch (JapidResult jr) {
					RenderResult rr = jr.getRenderResult();
					bodyHtml = rr.getContent().toString();
				}
    		} 

            // Recipients
            final List<Object> recipientList = (List<Object>) infoMap.get(RECIPIENTS);
            // From
            final Object from = infoMap.get(FROM);
            final Object replyTo = infoMap.get(REPLY_TO);

            Email email = null;
            if (infoMap.get(ATTACHMENTS) == null) {
//                if (StringUtils.isEmpty(bodyHtml)) {
//                    email = new SimpleEmail();
//                    email.setMsg(bodyText);
//                } else {
                    HtmlEmail htmlEmail = new HtmlEmail();
                    htmlEmail.setHtmlMsg(bodyHtml);
//                    if (!StringUtils.isEmpty(bodyText)) {
//                        htmlEmail.setTextMsg(bodyText);
//                    }
                    email = htmlEmail;
//                }

            } else {
//                if (StringUtils.isEmpty(bodyHtml)) {
//                    email = new MultiPartEmail();
//                    email.setMsg(bodyText);
//                } else {
                    HtmlEmail htmlEmail = new HtmlEmail();
                    htmlEmail.setHtmlMsg(bodyHtml);
//                    if (!StringUtils.isEmpty(bodyText)) {
//                        htmlEmail.setTextMsg(bodyText);
//                    }
                    email = htmlEmail;
//                }
                MultiPartEmail multiPartEmail = (MultiPartEmail) email;
                List<EmailAttachment> objectList = (List<EmailAttachment>) infoMap.get(ATTACHMENTS);
                for (EmailAttachment object : objectList) {
                    multiPartEmail.attach(object);
                }
            }

            if (from != null) {
                try {
                    InternetAddress iAddress = new InternetAddress(from.toString());
                    email.setFrom(iAddress.getAddress(), iAddress.getPersonal());
                } catch (Exception e) {
                    email.setFrom(from.toString());
                }

            }

            if (replyTo != null) {
                try {
                    InternetAddress iAddress = new InternetAddress(replyTo.toString());
                    email.addReplyTo(iAddress.getAddress(), iAddress.getPersonal());
                } catch (Exception e) {
                    email.addReplyTo(replyTo.toString());
                }

            }

            if (recipientList != null) {
                for (Object recipient : recipientList) {
                    try {
                        InternetAddress iAddress = new InternetAddress(recipient.toString());
                        email.addTo(iAddress.getAddress(), iAddress.getPersonal());
                    } catch (Exception e) {
                        email.addTo(recipient.toString());
                    }
                }
            } else {
                throw new MailException("You must specify at least one recipient.");
            }


            List<Object> ccsList = (List<Object>) infoMap.get(CCS);
            if (ccsList != null) {
                for (Object cc : ccsList) {
                    email.addCc(cc.toString());
                }
            }

            List<Object> bccsList = (List<Object>) infoMap.get(BCCS);
            if (bccsList != null) {

                for (Object bcc : bccsList) {
                    try {
                        InternetAddress iAddress = new InternetAddress(bcc.toString());
                        email.addBcc(iAddress.getAddress(), iAddress.getPersonal());
                    } catch (Exception e) {
                        email.addBcc(bcc.toString());
                    }
                }
            }
            if (!StringUtils.isEmpty(charset)) {
                email.setCharset(charset);
            }

            email.setSubject(subject);
            email.updateContentType(TEXT_HTML);

            if (headers != null) {
                for (String key : headers.keySet()) {
                    email.addHeader(key, headers.get(key));
                }
            }
            // reset the infomap
            infos.remove();
            return Mail.send(email);
        } catch (EmailException ex) {
            throw new MailException("Cannot send email", ex);
        }
    }

    public static boolean sendAndWait(Object... args) {
        try {
            Future<Boolean> result = send(args);
            return result.get();
        } catch (InterruptedException e) {
            Logger.error(e, "Error while waiting Mail.send result");
        } catch (ExecutionException e) {
            Logger.error(e, "Error while waiting Mail.send result");
        }
        return false;
    }
}
