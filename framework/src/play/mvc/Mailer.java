package play.mvc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import play.Logger;
import play.Play;
import play.classloading.enhancers.LocalvariablesNamesEnhancer.LocalVariablesNamesTracer;
import play.classloading.enhancers.LocalvariablesNamesEnhancer.LocalVariablesSupport;
import play.exceptions.TemplateNotFoundException;
import play.exceptions.UnexpectedException;
import play.libs.Mail;
import play.templates.Template;
import play.templates.TemplateLoader;

import javax.mail.internet.InternetAddress;

/**
 * Application mailer support
 */
public class Mailer implements LocalVariablesSupport {

    protected static ThreadLocal<HashMap<String, Object>> infos = new ThreadLocal<HashMap<String, Object>>();

    public static void setSubject(String subject, Object... args) {
        HashMap map = infos.get();
        if (map == null) {
            throw new UnexpectedException("Mailer not instrumented ?");
        }
        map.put("subject", String.format(subject, args));
        infos.set(map);
    }

    public static void addRecipient(Object... recipients) {
        HashMap map = infos.get();
        if (map == null) {
            throw new UnexpectedException("Mailer not instrumented ?");
        }
        List recipientsList = (List<String>) map.get("recipients");
        if (recipientsList == null) {
            recipientsList = new ArrayList<String>();
            map.put("recipients", recipientsList);
        }
        recipientsList.addAll(Arrays.asList(recipients));
        infos.set(map);
    }

    public static void addAttachment(Object... attachments) {
        HashMap map = infos.get();
        if (map == null) {
            throw new UnexpectedException("Mailer not instrumented ?");
        }
        List attachmentsList = (List) map.get("attachments");
        if (attachmentsList == null) {
            attachmentsList = new ArrayList<Object>();
            map.put("attachments", attachmentsList);
        }
        attachmentsList.addAll(Arrays.asList(attachments));
        infos.set(map);
    }

    public static void setContentType(String contentType) {
        HashMap map = infos.get();
        if (map == null) {
            throw new UnexpectedException("Mailer not instrumented ?");
        }
        map.put("contentType", contentType);
        infos.set(map);
    }

    /**
     * Can be of the form xxx <m@m.com>
     * @param from
     */
    public static void setFrom(Object from) {
        HashMap map = infos.get();
        if (map == null) {
            throw new UnexpectedException("Mailer not instrumented ?");
        }
        map.put("from", from);
        infos.set(map);
    }

    /**
     * Can be of the form xxx <m@m.com>
     * @param replyTo
     */
    public static void setReplyTo(Object replyTo) {
        HashMap map = infos.get();
        if (map == null) {
            throw new UnexpectedException("Mailer not instrumented ?");
        }
        map.put("replyTo", replyTo);
        infos.set(map);
    }

    /**
     * @deprecated 
     * @param personal 
     */
    public static void setPersonal(String personal) {
        HashMap map = infos.get();
        if (map == null) {
            throw new UnexpectedException("Mailer not instrumented ?");
        }
        map.put("personal", personal);
        infos.set(map);
    }

    public static void setCharset(String bodyCharset) {
        HashMap map = infos.get();
        if (map == null) {
            throw new UnexpectedException("Mailer not instrumented ?");
        }
        map.put("charset", bodyCharset);
        infos.set(map);
    }

    public static void addHeader(String key, String value) {
        HashMap map = infos.get();
        if (map == null) {
            throw new UnexpectedException("Mailer not instrumented ?");
        }
        HashMap<String, String> headers = (HashMap<String, String>) map.get("headers");
        if (headers == null) {
            headers = new HashMap<String, String>();
        }
        headers.put(key, value);
        map.put("headers", headers);
        infos.set(map);
    }

    public static Future<Boolean> send(Object... args) {
        HashMap map = infos.get();
        if (map == null) {
            throw new UnexpectedException("Mailer not instrumented ?");
        }

         // Body character set
         String charset = (String) infos.get().get("charset");

         // Headers
         Map<String, String> headers = (Map<String,String>) infos.get().get("headers");

        // Subject
        String subject = (String) infos.get().get("subject");

        String templateName = (String) infos.get().get("method");
        if (templateName.startsWith("notifiers.")) {
            templateName = templateName.substring("notifiers.".length());
        }
        if (templateName.startsWith("controllers.")) {
            templateName = templateName.substring("controllers.".length());
        }
        templateName = templateName.substring(0, templateName.indexOf("("));
        templateName = templateName.replace(".", "/");

        // overrides Template name
        if (args.length > 0 && args[0] instanceof String && LocalVariablesNamesTracer.getAllLocalVariableNames(args[0]).isEmpty()) {
            templateName = args[0].toString();
        }

        Map<String, Object> templateHtmlBinding = new HashMap();
        Map<String, Object> templateTextBinding = new HashMap();
        for (Object o : args) {
            List<String> names = LocalVariablesNamesTracer.getAllLocalVariableNames(o);
            for (String name : names) {
                templateHtmlBinding.put(name, o);
                templateTextBinding.put(name, o);
            }
        }

        // The rule is as follow: If we ask for text/plain, we don't care about the HTML
        // If we ask for HTML and there is a text/plain we add it as an alternative.
        // If contentType is not specified look at the template available:
        // - .txt only -> text/plain
        // else
        // -           -> text/html
        String contentType = (String) infos.get().get("contentType");
        String bodyHtml = null;
        String bodyText = "";
        try {
            Template templateHtml = TemplateLoader.load(templateName + ".html");
            bodyHtml = templateHtml.render(templateHtmlBinding);
        } catch (TemplateNotFoundException e) {
            if (contentType != null && !contentType.startsWith("text/plain")) {
                throw e;
            }
        }

        try {
            Template templateText = TemplateLoader.load(templateName + ".txt");
            bodyText = templateText.render(templateTextBinding);
        } catch (TemplateNotFoundException e) {
            if (bodyHtml == null && (contentType == null || (contentType != null && contentType.startsWith("text/plain")))) {
                throw e;
            }
        }

        // Content type

        if (contentType == null) {
            if (bodyHtml != null) {
                contentType = "text/html";
            } else {
                contentType = "text/plain";
            }
        }

        // Recipients
        List<Object> recipientList = (List<Object>) infos.get().get("recipients");
        if(recipientList == null || recipientList.isEmpty()) {
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
                    return false;
                }

                public Boolean get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
                    return false;
                }
            };
        }
        Object[] recipients = recipientList.toArray(new Object[recipientList.size()]);
 
        // From
        Object from = infos.get().get("from");
        Object replyTo = infos.get().get("replyTo");

        // Attachment
        Object[] attachments = new Object[]{};
        if (infos.get().get("attachments") != null) {
            List<Object> attachmentsList = (List<Object>) infos.get().get("attachments");
            attachments = attachmentsList.toArray(new Object[attachmentsList.size()]);
        }
        
        // Send
        final String body = (bodyHtml != null ? bodyHtml : bodyText);
        final String alternate = (bodyHtml != null ? bodyText : null);

       return Mail.sendEmail(from, replyTo, recipients, subject, body, alternate, contentType, charset, headers, attachments);
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
