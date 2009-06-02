package play.mvc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import play.Logger;
import play.classloading.enhancers.LocalvariablesNamesEnhancer.LocalVariablesNamesTracer;
import play.classloading.enhancers.LocalvariablesNamesEnhancer.LocalVariablesSupport;
import play.exceptions.UnexpectedException;
import play.libs.Mail;
import play.templates.Template;
import play.templates.TemplateLoader;

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

    public static void setFrom(Object from) {
        HashMap map = infos.get();
        if (map == null) {
            throw new UnexpectedException("Mailer not instrumented ?");
        }
        map.put("from", from);
        infos.set(map);
    }
    
    public static void setReplyTo(Object replyTo) {
        HashMap map = infos.get();
        if (map == null) {
            throw new UnexpectedException("Mailer not instrumented ?");
        }
        map.put("replyTo", replyTo);
        infos.set(map);
    }


    public static Future<Boolean> send(Object... args) {
        HashMap map = infos.get();
        if (map == null) {
            throw new UnexpectedException("Mailer not instrumented ?");
        }

        // Content type
        String contentType = (String) infos.get().get("contentType");
        if (contentType == null) {
            contentType = "text/plain";
        }

        // Subject
        String subject = (String) infos.get().get("subject");

        String templateName = (String) infos.get().get("method");
        if (templateName.startsWith("notifiers.")) {
            templateName = templateName.substring("notifiers.".length());
        }
        templateName = templateName.substring(0, templateName.indexOf("("));
        templateName = templateName.replace(".", "/");
        if (contentType.equals("text/html")) {
            templateName += ".html";
        } else {
            templateName += ".txt";
        }
        // overrides Template name
        if (args.length > 0 && args[0] instanceof String && LocalVariablesNamesTracer.getAllLocalVariableNames(args[0]).isEmpty()) {
            templateName = args[0].toString();
        }

        HashMap<String, Object> params = new HashMap<String, Object>();
        Template template = TemplateLoader.load(templateName);

        Map<String, Object> templateBinding = new HashMap();
        for (Object o : args) {
            List<String> names = LocalVariablesNamesTracer.getAllLocalVariableNames(o);
            for (String name : names) {
                templateBinding.put(name, o);
            }
        }
        String body = template.render(templateBinding);

        // Recipients
        List<Object> recipientList = (List<Object>) infos.get().get("recipients");
        Object[] recipients = new Object[recipientList.size()];
        int i = 0;
        for (Object recipient : recipientList) {
            recipients[i] = recipient;
            i++;
        }

        // From
        Object from = infos.get().get("from");
        Object replyTo = infos.get().get("replyTo");

        // Attachment
        Object[] attachements = new Object[0];
        if (infos.get().get("attachments") != null) {
            List<Object> objectList = (List<Object>) infos.get().get("attachments");
            attachements = new Object[objectList.size()];
            i = 0;
            for (Object object : objectList) {
            	attachements[i] = object;
                i++;
            }

        }

        // Send
        return Mail.send(from, replyTo, recipients, subject, body, contentType, attachements);
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
