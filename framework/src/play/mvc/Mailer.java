package play.mvc;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import play.classloading.enhancers.LocalvariablesNamesEnhancer.LocalVariablesNamesTracer;
import play.libs.Mail;
import play.templates.Template;
import play.templates.TemplateLoader;


public class Mailer {

    private static ThreadLocal<HashMap<String, Object>> infos = new ThreadLocal<HashMap<String, Object>>();
    
    public static void setSubject(String subject) {
        HashMap map = infos.get();
        if (map == null) {
            map = new HashMap<String, String>();
        }
        map.put("subject", subject);
        infos.set(map);
    }

    public static void addRecipient(String ... recipients) {
        HashMap map = infos.get();
        if (map == null) {
            map = new HashMap<String, String>();
        }
        List recipientsList = (List<String>) map.get("recipients");
        if (recipientsList == null) {
            recipientsList = new ArrayList<String>();
            map.put("recipients", recipientsList);
        }
        recipientsList.addAll(Arrays.asList(recipients));
        infos.set(map);
    }

    public static void addAttachment(File ... attachments) {
        HashMap map = infos.get();
        if (map == null) {
            map = new HashMap<String, String>();
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
            map = new HashMap<String, String>();
        }
        map.put("contentType", contentType);
        infos.set(map);
    }

    public static void setCharset(String charset) {
        HashMap map = infos.get();
        if (map == null) {
            map = new HashMap<String, String>();
        }
        map.put("charset", charset);
        infos.set(map);
    }

    public static void setFrom(String from) {
        HashMap map = infos.get();
        if (map == null) {
            map = new HashMap<String, String>();
        }
        map.put("from", from);
        infos.set(map);
    }

    public static void renderBody(Object... args) {

        String templateName = null;

        String className = null;
        String methodName = null;

        StackTraceElement st = Thread.currentThread().getStackTrace()[2];
        className = st.getClassName();
        methodName = st.getMethodName();
        templateName = className.replaceAll("\\.", File.separator) + File.separator + methodName + ".html";

        HashMap<String, Object> params = new HashMap<String, Object>();
        Template template = TemplateLoader.load(templateName);

        Scope.RenderArgs templateBinding = Scope.RenderArgs.current();
        for (Object o : args) {
            List<String> names = LocalVariablesNamesTracer.getAllLocalVariableNames(o);
            for(String name : names) {
                templateBinding.put(name, o);
            }
        }
        params.put("body", template.render(templateBinding.data));
        params.remove("messages");
        params.remove("out");
        params.remove("lang");

        List<String> recipientList = (List<String>) infos.get().get("recipients");
        String[] recipients = new String[recipientList.size()];
        int i = 0;
        for(String recipient : recipientList) {
            recipients[i] = recipient;
            i++;
        }
        String from = (String) infos.get().get("from");
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
        
        if(infos.get().containsKey("contentType"))
            params.put("contentType", infos.get().get("contentType"));
        if(infos.get().containsKey("charset"))
            params.put("charset", infos.get().get("charset"));
        if(infos.get().containsKey("subject"))
            params.put("subject", infos.get().get("subject"));

        //Mail.send(from, recipients, params, files);
    }
    
}
