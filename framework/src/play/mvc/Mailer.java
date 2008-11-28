package play.mvc;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import play.classloading.enhancers.LocalvariablesNamesEnhancer.LocalVariablesNamesTracer;
import play.exceptions.UnexpectedException;
import play.libs.Mail;
import play.templates.Template;
import play.templates.TemplateLoader;

public class Mailer {

    protected static ThreadLocal<HashMap<String, Object>> infos = new ThreadLocal<HashMap<String, Object>>();

    public static void setSubject(String subject, Object... args) {
        HashMap map = infos.get();
        if (map == null) {
            throw new UnexpectedException("Mailer not instrumented ?");
        }
        map.put("subject", String.format(subject, args));
        infos.set(map);
    }

    public static void addRecipient(String... recipients) {
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

    public static void addAttachment(File... attachments) {
        HashMap map = infos.get();
        if (map == null) {
            throw new UnexpectedException("Mailer not instrumented ?");
        }
        List attachmentsList = (List) map.get("attachments");
        if (attachmentsList == null) {
            attachmentsList = new ArrayList<File>();
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

    public static void setFrom(String from) {
        HashMap map = infos.get();
        if (map == null) {
            throw new UnexpectedException("Mailer not instrumented ?");
        }
        map.put("from", from);
        infos.set(map);
    }

    public static void send(Object... args) {
        HashMap map = infos.get();
        if (map == null) {
            throw new UnexpectedException("Mailer not instrumented ?");
        }

        // Content type
        String contentType = (String) infos.get().get("contentType");
        if(contentType == null) {
            contentType = "text/plain";
        }

        // Subject
        String subject = (String) infos.get().get("subject");

        String templateName = (String)infos.get().get("method");
        if(templateName.startsWith("notifiers.")) {
            templateName = templateName.substring("notifiers.".length());
        }
        templateName = templateName.substring(0, templateName.indexOf("("));
        templateName = templateName.replace(".", "/");
        if(contentType.equals("text/html")) {
            templateName += ".html";
        } else {
            templateName += ".txt";
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
        List<String> recipientList = (List<String>) infos.get().get("recipients");
        String[] recipients = new String[recipientList.size()];
        int i = 0;
        for (String recipient : recipientList) {
            recipients[i] = recipient;
            i++;
        }

        // From
        String from = (String) infos.get().get("from");

        // Attachment
        File[] files = new File[0];
        if (infos.get().get("attachments") != null) {
            List<File> fileList = (List<File>) infos.get().get("attachments");
            files = new File[fileList.size()];
            i = 0;
            for (File file : fileList) {
                files[i] = file;
                i++;
            }

        }
        
        // Send
        Mail.send(from, recipients, subject, body, contentType, files);
    }
}
