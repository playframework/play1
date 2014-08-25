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
import cn.bran.japid.template.RenderResult;
import cn.bran.japid.util.DirUtil;
import cn.bran.japid.util.StackTraceUtils;
import cn.bran.japid.util.StringUtils;

/**
 *  Application mailer support, based on the Mail class in the Play distribution.
 * 
 *  Only html format is support for now. 
 *  
 *  The default template path is : /app/japidviews/_notifiers/{custom mailer package}/{custom mailer class}/{method name}.html
 */
public class JapidMailer  {

    protected static final String TEXT_HTML = "text/html";
//	private static final String TEXT_PLAIN = "text/plain";
//	private static final String CONTROLLERS = "controllers.";
//	private static final String NOTIFIERS = "notifiers.";
//	private static final String METHOD = "method";
	protected static final String HEADERS = "headers";
	protected static final String SUBJECT = "subject";
	protected static final String CHARSET = "charset";
	protected static final String REPLY_TO = "replyTo";
	protected static final String FROM = "from";
	protected static final String CONTENT_TYPE = "contentType";
	protected static final String ATTACHMENTS = "attachments";
	protected static final String CCS = "ccs";
	protected static final String BCCS = "bccs";
	protected static final String RECIPIENTS = "recipients";
	protected static ThreadLocal<HashMap<String, Object>> infos = new ThreadLocal<HashMap<String, Object>>() {
		@Override
		protected HashMap<String, Object> initialValue() {
			return new HashMap<String, Object>();
		}
	};

    public static void setSubject(String subject, Object... args) {
        HashMap<String, Object> map = getInfoMap();
        map.put(SUBJECT, String.format(subject, args));
        infos.set(map);
    }

	/**
	 * @return
	 */
	protected static HashMap<String, Object> getInfoMap() {
		HashMap<String, Object> map = infos.get();
        if (map == null) {
            throw new UnexpectedException("Mailer not instrumented ?");
        }
		return map;
	}

    @SuppressWarnings("unchecked")
    public static void addRecipient(Object... recipients) {
        HashMap<String, Object> map = getInfoMap();
        List recipientsList = (List<String>) map.get(RECIPIENTS);
        if (recipientsList == null) {
            recipientsList = new ArrayList<String>();
            map.put(RECIPIENTS, recipientsList);
        }
        recipientsList.addAll(Arrays.asList(recipients));
        infos.set(map);
    }

    @SuppressWarnings("unchecked")
    public static void addBcc(Object... bccs) {
        HashMap<String, Object> map = getInfoMap();
        List bccsList = (List<String>) map.get(BCCS);
        if (bccsList == null) {
            bccsList = new ArrayList<String>();
            map.put(BCCS, bccsList);
        }
        bccsList.addAll(Arrays.asList(bccs));
        infos.set(map);
    }

    @SuppressWarnings("unchecked")
    public static void addCc(Object... ccs) {
        HashMap<String, Object> map = getInfoMap();
        List ccsList = (List<String>) map.get(CCS);
        if (ccsList == null) {
            ccsList = new ArrayList<String>();
            map.put(CCS, ccsList);
        }
        ccsList.addAll(Arrays.asList(ccs));
        infos.set(map);
    }

    @SuppressWarnings("unchecked")
    public static void addAttachment(EmailAttachment... attachments) {
        HashMap<String, Object> map = getInfoMap();
        List<EmailAttachment> attachmentsList = (List<EmailAttachment>) map.get(ATTACHMENTS);
        if (attachmentsList == null) {
            attachmentsList = new ArrayList<EmailAttachment>();
            map.put(ATTACHMENTS, attachmentsList);
        }
        attachmentsList.addAll(Arrays.asList(attachments));
        infos.set(map);
    }

    public static void setContentType(String contentType) {
        HashMap<String, Object> map = getInfoMap();
        map.put(CONTENT_TYPE, contentType);
        infos.set(map);
    }

    /**
     * Can be of the form xxx <m@m.com>
     *
     * @param from
     */
    public static void setFrom(Object from) {
        HashMap<String, Object> map = getInfoMap();
        map.put(FROM, from);
        infos.set(map);
    }

    /**
     * Can be of the form xxx <m@m.com>
     *
     * @param replyTo
     */
    public static void setReplyTo(Object replyTo) {
        HashMap<String, Object> map = getInfoMap();
        map.put(REPLY_TO, replyTo);
        infos.set(map);
    }

    public static void setCharset(String bodyCharset) {
        HashMap<String, Object> map = getInfoMap();
        map.put(CHARSET, bodyCharset);
        infos.set(map);
    }

    @SuppressWarnings("unchecked")
    public static void addHeader(String key, String value) {
        HashMap<String, Object> map = getInfoMap();
        HashMap<String, String> headers = (HashMap<String, String>) map.get(HEADERS);
        if (headers == null) {
            headers = new HashMap<String, String>();
        }
        headers.put(key, value);
        map.put(HEADERS, headers);
        infos.set(map);
    }

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
//            if (templateNameBase.startsWith(NOTIFIERS)) {
//                templateNameBase = templateNameBase.substring(NOTIFIERS.length());
//            }
//            if (templateNameBase.startsWith(CONTROLLERS)) {
//                templateNameBase = templateNameBase.substring(CONTROLLERS.length());
//            }
//            templateNameBase = templateNameBase.substring(0, templateNameBase.indexOf("("));
//            templateNameBase = templateNameBase.replace(".", "/");

//            final Map<String, Object> templateHtmlBinding = new HashMap<String, Object>();
//            final Map<String, Object> templateTextBinding = new HashMap<String, Object>();
//            for (Object o : args) {
//                List<String> names = LocalVariablesNamesTracer.getAllLocalVariableNames(o);
//                for (String name : names) {
//                    templateHtmlBinding.put(name, o);
//                    templateTextBinding.put(name, o);
//                }
//            }

            String templateClassName = DirUtil.JAPIDVIEWS_ROOT + "._" + templateNameBase;
            
            String bodyHtml = null;
    		Class tClass = Play.classloader.getClassIgnoreCase(templateClassName);
    		if (tClass == null) {
    			String templateFileName = templateClassName.replace('.', '/') + ".html";
    			throw new RuntimeException("Japid Emailer: could not find a Japid template with the name of: " + templateFileName);
    		} else if (JapidTemplateBase.class.isAssignableFrom(tClass)) {
    			try {
					JapidController.render(tClass, args);
				} catch (JapidResult jr) {
					RenderResult rr = jr.getRenderResult();
					bodyHtml = rr.getContent().toString();
				}
    		} else {
    			throw new RuntimeException("The found class is not a Japid template class: " + templateClassName);
    		}

//    		System.out.println("email body: " + bodyHtml);
    		// The rule is as follow: If we ask for text/plain, we don't care about the HTML
            // If we ask for HTML and there is a text/plain we add it as an alternative.
            // If contentType is not specified look at the template available:
            // - .txt only -> text/plain
            // else
            // -           -> text/html
//            String contentType = (String) infoMap.get(CONTENT_TYPE);
//            String bodyText = "";
//            try {
//                Template templateHtml = TemplateLoader.load(templateNameBase + ".html");
//                bodyHtml = templateHtml.render(templateHtmlBinding);
//            } catch (TemplateNotFoundException e) {
//                if (contentType != null && !contentType.startsWith(TEXT_PLAIN)) {
//                    throw e;
//                }
//            }
////
//            try {
//                Template templateText = TemplateLoader.load(templateName + ".txt");
//                bodyText = templateText.render(templateTextBinding);
//            } catch (TemplateNotFoundException e) {
//                if (bodyHtml == null && (contentType == null || contentType.startsWith(TEXT_PLAIN))) {
//                    throw e;
//                }
//            }

            // Content type
    		
    		// bran html for now

//            if (contentType == null) {
//                if (bodyHtml != null) {
//                    contentType = TEXT_HTML;
//                } else {
//                    contentType = TEXT_PLAIN;
//                }
//            }

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
