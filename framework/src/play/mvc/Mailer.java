package play.mvc;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.activation.DataSource;
import javax.activation.URLDataSource;
import javax.mail.internet.InternetAddress;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailAttachment;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;
import org.apache.commons.mail.MultiPartEmail;
import org.apache.commons.mail.SimpleEmail;

import play.Logger;
import play.Play;
import play.classloading.enhancers.LocalvariablesNamesEnhancer.LocalVariablesNamesTracer;
import play.classloading.enhancers.LocalvariablesNamesEnhancer.LocalVariablesSupport;
import play.exceptions.MailException;
import play.exceptions.TemplateNotFoundException;
import play.exceptions.UnexpectedException;
import play.libs.F;
import play.libs.F.T4;
import play.libs.Mail;
import play.libs.MimeTypes;
import play.templates.Template;
import play.templates.TemplateLoader;
import play.vfs.VirtualFile;

/**
 * Application mailer support
 */
public class Mailer implements LocalVariablesSupport {

    protected static final ThreadLocal<Map<String, Object>> infos = new ThreadLocal<>();

    /**
     * Set subject of mail, optionally providing formatting arguments
     * 
     * @param subject
     *            plain String or formatted string - interpreted as formatted string only if arguments are provided
     * @param args
     *            optional arguments for formatting subject
     */
    public static void setSubject(String subject, Object... args) {
        Map<String, Object> map = infos.get();
        if (map == null) {
            throw new UnexpectedException("Mailer not instrumented ?");
        }
        if (args.length != 0) {
            subject = String.format(subject, args);
        }
        map.put("subject", subject);
        infos.set(map);
    }

    @SuppressWarnings("unchecked")
    public static void addRecipient(String... recipients) {
        List<String> recipientsParam = Arrays.asList(recipients);
        addRecipients(recipientsParam);
    }

    /**
     * Add recipients
     * 
     * @param recipients
     *            List of recipients
     * @deprecated use method {{@link #addRecipient(String...)}}
     */
    @Deprecated
    public static void addRecipient(Object... recipients) {
        List<String> recipientList = new ArrayList<>(recipients.length);
        for (Object recipient : recipients) {
            recipientList.add(recipient.toString());
        }
        addRecipients(recipientList);
    }

    private static void addRecipients(List<String> recipientsParam) {
        Map<String, Object> map = infos.get();
        if (map == null) {
            throw new UnexpectedException("Mailer not instrumented ?");
        }
        List<String> recipientsList = (List<String>) map.get("recipients");
        if (recipientsList == null) {
            recipientsList = new ArrayList<>();
            map.put("recipients", recipientsList);
        }
        recipientsList.addAll(recipientsParam);
        infos.set(map);
    }

    @SuppressWarnings("unchecked")
    public static void addBcc(String... bccs) {
        Map<String, Object> map = infos.get();
        if (map == null) {
            throw new UnexpectedException("Mailer not instrumented ?");
        }
        List<String> bccsList = (List<String>) map.get("bccs");
        if (bccsList == null) {
            bccsList = new ArrayList<>();
            map.put("bccs", bccsList);
        }
        bccsList.addAll(Arrays.asList(bccs));
        infos.set(map);
    }

    @SuppressWarnings("unchecked")
    public static void addCc(String... ccs) {
        Map<String, Object> map = infos.get();
        if (map == null) {
            throw new UnexpectedException("Mailer not instrumented ?");
        }
        List<String> ccsList = (List<String>) map.get("ccs");
        if (ccsList == null) {
            ccsList = new ArrayList<>();
            map.put("ccs", ccsList);
        }
        ccsList.addAll(Arrays.asList(ccs));
        infos.set(map);
    }

    @SuppressWarnings("unchecked")
    public static void addAttachment(EmailAttachment... attachments) {
        Map<String, Object> map = infos.get();
        if (map == null) {
            throw new UnexpectedException("Mailer not instrumented ?");
        }
        List<EmailAttachment> attachmentsList = (List<EmailAttachment>) map.get("attachments");
        if (attachmentsList == null) {
            attachmentsList = new ArrayList<>();
            map.put("attachments", attachmentsList);
        }
        attachmentsList.addAll(Arrays.asList(attachments));
        infos.set(map);
    }

    @SuppressWarnings("unchecked")
    public static void attachDataSource(DataSource dataSource, String name, String description, String disposition) {
        Map<String, Object> map = infos.get();
        if (map == null) {
            throw new UnexpectedException("Mailer not instrumented ?");
        }
        List<T4<DataSource, String, String, String>> datasourceList = (List<T4<DataSource, String, String, String>>) map.get("datasources");
        if (datasourceList == null) {
            datasourceList = new ArrayList<>();
            map.put("datasources", datasourceList);
        }
        datasourceList.add(F.T4(dataSource, name, description, disposition));
        infos.set(map);
    }

    public static void attachDataSource(DataSource dataSource, String name, String description) {
        attachDataSource(dataSource, name, description, EmailAttachment.ATTACHMENT);
    }

    public static String attachInlineEmbed(DataSource dataSource, String name) {
        Map<String, Object> map = infos.get();
        if (map == null) {
            throw new UnexpectedException("Mailer not instrumented ?");
        }

        InlineImage inlineImage = new InlineImage(dataSource);

        Map<String, InlineImage> inlineEmbeds = (Map<String, InlineImage>) map.get("inlineEmbeds");
        if (inlineEmbeds == null) {
            inlineEmbeds = new HashMap<>();
            map.put("inlineEmbeds", inlineEmbeds);
        }

        inlineEmbeds.put(name, inlineImage);
        infos.set(map);

        return "cid:" + inlineImage.cid;
    }

    public static void setContentType(String contentType) {
        Map<String, Object> map = infos.get();
        if (map == null) {
            throw new UnexpectedException("Mailer not instrumented ?");
        }
        map.put("contentType", contentType);
        infos.set(map);
    }

    /**
     * Can be of the form xxx &lt;m@m.com&gt;
     *
     * @param from
     *            The sender name (ex: xxx &lt;m@m.com&gt;)
     */
    public static void setFrom(String from) {
        Map<String, Object> map = infos.get();
        if (map == null) {
            throw new UnexpectedException("Mailer not instrumented ?");
        }
        map.put("from", from);
        infos.set(map);
    }

    public static void setFrom(InternetAddress from) {
        setFrom(from.toString());
    }

    private static class InlineImage {
        /** content id */
        private final String cid;
        /** <code>DataSource</code> for the content */
        private final DataSource dataSource;

        public InlineImage(DataSource dataSource) {
            this(null, dataSource);
        }

        public InlineImage(String cid, DataSource dataSource) {
            super();
            this.cid = cid != null ? cid : RandomStringUtils.randomAlphabetic(HtmlEmail.CID_LENGTH).toLowerCase();
            this.dataSource = dataSource;
        }

        public String getCid() {
            return this.cid;
        }

        public DataSource getDataSource() {
            return this.dataSource;
        }
    }

    private static class VirtualFileDataSource implements DataSource {
        private final VirtualFile virtualFile;

        public VirtualFileDataSource(VirtualFile virtualFile) {
            this.virtualFile = virtualFile;
        }

        public VirtualFileDataSource(String relativePath) {
            this.virtualFile = VirtualFile.fromRelativePath(relativePath);
        }

        @Override
        public String getContentType() {
            return MimeTypes.getContentType(this.virtualFile.getName());
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return this.virtualFile.inputstream();
        }

        @Override
        public String getName() {
            return this.virtualFile.getName();
        }

        @Override
        public OutputStream getOutputStream() throws IOException {
            return this.virtualFile.outputstream();
        }

        public VirtualFile getVirtualFile() {
            return this.virtualFile;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof VirtualFileDataSource)) {
                return false;
            }

            VirtualFileDataSource rhs = (VirtualFileDataSource) obj;

            return this.virtualFile.equals(rhs.virtualFile);
        }
    }

    @Deprecated
    public static String getEmbedddedSrc(String urlString, String name) {
        return getEmbeddedSrc(urlString, name);
    }

    public static String getEmbeddedSrc(String urlString, String name) {
        Map<String, Object> map = infos.get();
        if (map == null) {
            throw new UnexpectedException("Mailer not instrumented ?");
        }

        DataSource dataSource;
        URL url = null;

        VirtualFile img = Play.getVirtualFile(urlString);
        if (img == null) {
            // Not a local image, check for a distant image
            try {
                url = new URL(urlString);
            } catch (MalformedURLException e1) {
                throw new UnexpectedException("Invalid URL '" + urlString + "'", e1);
            }

            if (name == null || name.isEmpty()) {
                String[] parts = url.getPath().split("/");
                name = parts[parts.length - 1];
            }

            if (StringUtils.isEmpty(name)) {
                throw new UnexpectedException("name cannot be null or empty");
            }

            dataSource = url.getProtocol().equals("file") ? new VirtualFileDataSource(url.getFile()) : new URLDataSource(url);
        } else {
            dataSource = new VirtualFileDataSource(img);
        }

        Map<String, InlineImage> inlineEmbeds = (Map<String, InlineImage>) map.get("inlineEmbeds");

        // Check if a URLDataSource for this name has already been attached;
        // if so, return the cached CID value.
        if (inlineEmbeds != null && inlineEmbeds.containsKey(name)) {
            InlineImage ii = inlineEmbeds.get(name);

            if (ii.getDataSource() instanceof URLDataSource) {
                URLDataSource urlDataSource = (URLDataSource) ii.getDataSource();
                // Make sure the supplied URL points to the same thing
                // as the one already associated with this name.
                // NOTE: Comparing URLs with URL.equals() is a blocking
                // operation
                // in the case of a network failure therefore we use
                // url.toExternalForm().equals() here.
                if (url == null || urlDataSource == null || !url.toExternalForm().equals(urlDataSource.getURL().toExternalForm())) {
                    throw new UnexpectedException("embedded name '" + name + "' is already bound to URL " + urlDataSource.getURL()
                            + "; existing names cannot be rebound");
                }
            } else if (!ii.getDataSource().equals(dataSource)) {
                throw new UnexpectedException("embedded name '" + name + "' is already bound to URL " + dataSource.getName()
                        + "; existing names cannot be rebound");
            }

            return "cid:" + ii.getCid();
        }

        // Verify that the data source is valid.

        try (InputStream is = dataSource.getInputStream()) {
        } catch (IOException e) {
            throw new UnexpectedException("Invalid URL " + urlString + " for image " + name, e);
        }

        return attachInlineEmbed(dataSource, name);
    }

    /**
     * Can be of the form xxx &lt;m@m.com&gt;
     *
     * @param replyTo
     *            : The reply to address (ex: xxx &lt;m@m.com&gt;)
     */
    public static void setReplyTo(String replyTo) {
        Map<String, Object> map = infos.get();
        if (map == null) {
            throw new UnexpectedException("Mailer not instrumented ?");
        }
        map.put("replyTo", replyTo);
        infos.set(map);
    }

    public static void setReplyTo(InternetAddress replyTo) {
        setReplyTo(replyTo.toString());
    }

    public static void setCharset(String bodyCharset) {
        Map<String, Object> map = infos.get();
        if (map == null) {
            throw new UnexpectedException("Mailer not instrumented ?");
        }
        map.put("charset", bodyCharset);
        infos.set(map);
    }

    @SuppressWarnings("unchecked")
    public static void addHeader(String key, String value) {
        Map<String, Object> map = infos.get();
        if (map == null) {
            throw new UnexpectedException("Mailer not instrumented ?");
        }
        Map<String, String> headers = (Map<String, String>) map.get("headers");
        if (headers == null) {
            headers = new HashMap<>();
        }
        headers.put(key, value);
        map.put("headers", headers);
        infos.set(map);
    }

    public static Future<Boolean> send(Object... args) {
        try {
            Map<String, Object> map = infos.get();
            if (map == null) {
                throw new UnexpectedException("Mailer not instrumented ?");
            }

            // Body character set
            String charset = (String) infos.get().get("charset");

            // Headers
            Map<String, String> headers = (Map<String, String>) infos.get().get("headers");

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

            Map<String, Object> templateHtmlBinding = new HashMap<>();
            Map<String, Object> templateTextBinding = new HashMap<>();
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
            // - -> text/html
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
                if (bodyHtml == null && (contentType == null || contentType.startsWith("text/plain"))) {
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
            List<String> recipientList = (List<String>) infos.get().get("recipients");
            // From
            String from = (String) infos.get().get("from");
            String replyTo = (String) infos.get().get("replyTo");

            Email email;
            if (infos.get().get("attachments") == null && infos.get().get("datasources") == null
                    && infos.get().get("inlineEmbeds") == null) {
                if (StringUtils.isEmpty(bodyHtml)) {
                    email = new SimpleEmail();
                    email.setMsg(bodyText);
                } else {
                    HtmlEmail htmlEmail = new HtmlEmail();
                    htmlEmail.setHtmlMsg(bodyHtml);
                    if (!StringUtils.isEmpty(bodyText)) {
                        htmlEmail.setTextMsg(bodyText);
                    }
                    email = htmlEmail;
                }

            } else {
                if (StringUtils.isEmpty(bodyHtml)) {
                    email = new MultiPartEmail();
                    email.setMsg(bodyText);
                } else {
                    HtmlEmail htmlEmail = new HtmlEmail();
                    htmlEmail.setHtmlMsg(bodyHtml);
                    if (!StringUtils.isEmpty(bodyText)) {
                        htmlEmail.setTextMsg(bodyText);
                    }
                    email = htmlEmail;

                    Map<String, InlineImage> inlineEmbeds = (Map<String, InlineImage>) infos.get().get("inlineEmbeds");
                    if (inlineEmbeds != null) {
                        for (Map.Entry<String, InlineImage> entry : inlineEmbeds.entrySet()) {
                            htmlEmail.embed(entry.getValue().getDataSource(), entry.getKey(), entry.getValue().getCid());
                        }
                    }
                }

                MultiPartEmail multiPartEmail = (MultiPartEmail) email;
                List<EmailAttachment> objectList = (List<EmailAttachment>) infos.get().get("attachments");
                if (objectList != null) {
                    for (EmailAttachment object : objectList) {
                        multiPartEmail.attach(object);
                    }
                }

                // Handle DataSource
                List<T4<DataSource, String, String, String>> datasourceList = (List<T4<DataSource, String, String, String>>) infos.get()
                        .get("datasources");
                if (datasourceList != null) {
                    for (T4<DataSource, String, String, String> ds : datasourceList) {
                        multiPartEmail.attach(ds._1, ds._2, ds._3, ds._4);
                    }
                }
            }
            email.setCharset("utf-8");

            if (from != null) {
                try {
                    InternetAddress iAddress = new InternetAddress(from);
                    email.setFrom(iAddress.getAddress(), iAddress.getPersonal());
                } catch (Exception e) {
                    email.setFrom(from);
                }

            }

            if (replyTo != null) {
                try {
                    InternetAddress iAddress = new InternetAddress(replyTo);
                    email.addReplyTo(iAddress.getAddress(), iAddress.getPersonal());
                } catch (Exception e) {
                    email.addReplyTo(replyTo);
                }

            }

            if (recipientList != null) {
                for (String recipient : recipientList) {
                    try {
                        InternetAddress iAddress = new InternetAddress(recipient);
                        email.addTo(iAddress.getAddress(), iAddress.getPersonal());
                    } catch (Exception e) {
                        email.addTo(recipient);
                    }
                }
            } else {
                throw new MailException("You must specify at least one recipient.");
            }

            List<String> ccsList = (List<String>) infos.get().get("ccs");
            if (ccsList != null) {
                for (String cc : ccsList) {
                    email.addCc(cc);
                }
            }

            List<String> bccsList = (List<String>) infos.get().get("bccs");
            if (bccsList != null) {

                for (String bcc : bccsList) {
                    try {
                        InternetAddress iAddress = new InternetAddress(bcc);
                        email.addBcc(iAddress.getAddress(), iAddress.getPersonal());
                    } catch (Exception e) {
                        email.addBcc(bcc);
                    }
                }
            }
            if (!StringUtils.isEmpty(charset)) {
                email.setCharset(charset);
            }

            email.setSubject(subject);
            email.updateContentType(contentType);

            if (headers != null) {
                for (String key : headers.keySet()) {
                    email.addHeader(key, headers.get(key));
                }
            }

            return Mail.send(email);
        } catch (EmailException ex) {
            throw new MailException("Cannot send email", ex);
        }
    }

    public static boolean sendAndWait(Object... args) {
        try {
            Future<Boolean> result = send(args);
            return result.get();
        } catch (InterruptedException | ExecutionException e) {
            Logger.error(e, "Error while waiting Mail.send result");
        }
        return false;
    }
}
