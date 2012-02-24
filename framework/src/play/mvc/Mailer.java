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

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.mail.*;

import play.Logger;
import play.classloading.enhancers.LocalvariablesNamesEnhancer.LocalVariablesNamesTracer;
import play.classloading.enhancers.LocalvariablesNamesEnhancer.LocalVariablesSupport;
import play.exceptions.MailException;
import play.exceptions.TemplateNotFoundException;
import play.exceptions.UnexpectedException;
import play.libs.Mail;
import play.libs.MimeTypes;
import play.templates.Template;
import play.templates.TemplateLoader;
import play.vfs.VirtualFile;

import javax.activation.DataSource;
import javax.activation.URLDataSource;
import javax.mail.internet.InternetAddress;

/**
 * Application mailer support
 */
public class Mailer implements LocalVariablesSupport {

    protected static ThreadLocal<HashMap<String, Object>> infos = new ThreadLocal<HashMap<String, Object>>();

    public static void setSubject(String subject, Object... args) {
        HashMap<String, Object> map = infos.get();
        if (map == null) {
            throw new UnexpectedException("Mailer not instrumented ?");
        }
        map.put("subject", String.format(subject, args));
        infos.set(map);
    }

    @SuppressWarnings("unchecked")
    public static void addRecipient(Object... recipients) {
        HashMap<String, Object> map = infos.get();
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

    @SuppressWarnings("unchecked")
    public static void addBcc(Object... bccs) {
        HashMap<String, Object> map = infos.get();
        if (map == null) {
            throw new UnexpectedException("Mailer not instrumented ?");
        }
        List bccsList = (List<String>) map.get("bccs");
        if (bccsList == null) {
            bccsList = new ArrayList<String>();
            map.put("bccs", bccsList);
        }
        bccsList.addAll(Arrays.asList(bccs));
        infos.set(map);
    }

    @SuppressWarnings("unchecked")
    public static void addCc(Object... ccs) {
        HashMap<String, Object> map = infos.get();
        if (map == null) {
            throw new UnexpectedException("Mailer not instrumented ?");
        }
        List ccsList = (List<String>) map.get("ccs");
        if (ccsList == null) {
            ccsList = new ArrayList<String>();
            map.put("ccs", ccsList);
        }
        ccsList.addAll(Arrays.asList(ccs));
        infos.set(map);
    }

    @SuppressWarnings("unchecked")
    public static void addAttachment(EmailAttachment... attachments) {
        HashMap<String, Object> map = infos.get();
        if (map == null) {
            throw new UnexpectedException("Mailer not instrumented ?");
        }
        List<EmailAttachment> attachmentsList = (List<EmailAttachment>) map.get("attachments");
        if (attachmentsList == null) {
            attachmentsList = new ArrayList<EmailAttachment>();
            map.put("attachments", attachmentsList);
        }
        attachmentsList.addAll(Arrays.asList(attachments));
        infos.set(map);
    }

    public static void setContentType(String contentType) {
        HashMap<String, Object> map = infos.get();
        if (map == null) {
            throw new UnexpectedException("Mailer not instrumented ?");
        }
        map.put("contentType", contentType);
        infos.set(map);
    }

    /**
     * Can be of the form xxx <m@m.com>
     *
     * @param from
     */
    public static void setFrom(Object from) {
        HashMap<String, Object> map = infos.get();
        if (map == null) {
            throw new UnexpectedException("Mailer not instrumented ?");
        }
        map.put("from", from);
        infos.set(map);
    }
    
    private static class InlineImage {
    	/** content id */
        private final String cid;
        /** <code>DataSource</code> for the content */
        private final DataSource dataSource;
        
    	public InlineImage(String cid, DataSource dataSource) {
    		super();
    		this.cid = cid;
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
			
			VirtualFileDataSource rhs = (VirtualFileDataSource)obj;
			
			return this.virtualFile.equals(rhs.virtualFile);
		}
    }
    
	public static String getEmbedddedSrc(String urlString, String name) {
		HashMap<String, Object> map = infos.get();
		if (map == null) {
			throw new UnexpectedException("Mailer not instrumented ?");
		}
		
		URL url = null;
		try {
			url = new URL(urlString);
		} catch (MalformedURLException e1) {
			throw new UnexpectedException("Invalid URL '" + urlString + "'", e1);
		}
		
		if (StringUtils.isEmpty(name)) {
			String[] parts = url.getPath().split("/");
			name = parts[parts.length-1];
		}
		
		if (StringUtils.isEmpty(name)) {
			throw new UnexpectedException("name cannot be null or empty");
		}
		
		DataSource dataSource = url.getProtocol().equals("file")
				? new VirtualFileDataSource(url.getFile())
				: new URLDataSource(url);
				
		Map<String, InlineImage> inlineEmbeds = (Map<String, InlineImage>)map.get("inlineEmbeds");

		// Check if a URLDataSource for this name has already been attached;
		// if so, return the cached CID value.
		if (inlineEmbeds != null && inlineEmbeds.containsKey(name)) {
			InlineImage ii = inlineEmbeds.get(name);
			
			if (ii.getDataSource() instanceof URLDataSource) {
				URLDataSource urlDataSource = (URLDataSource)ii.getDataSource();
				// Make sure the supplied URL points to the same thing
				// as the one already associated with this name.
				// NOTE: Comparing URLs with URL.equals() is a blocking operation
				// in the case of a network failure therefore we use
				// url.toExternalForm().equals() here.
				if (!url.toExternalForm().equals(urlDataSource.getURL().toExternalForm())) {
					throw new UnexpectedException("embedded name '" + name + "' is already bound to URL "
							+ urlDataSource.getURL() + "; existing names cannot be rebound");
				}
			} else if (!ii.getDataSource().equals(dataSource)) {
				throw new UnexpectedException("embedded name '" + name + "' is already bound to URL "
						+ dataSource.getName() + "; existing names cannot be rebound");
			}
			
			return "cid:" + ii.getCid();
		}

		// Verify that the data source is valid.
		InputStream is = null;
		try {
			is = dataSource.getInputStream();
		} catch (IOException e) {
			throw new UnexpectedException("Invalid URL", e);
		} finally {
			IOUtils.closeQuietly(is);
		}
		
		String cid = RandomStringUtils.randomAlphabetic(HtmlEmail.CID_LENGTH).toLowerCase();
		InlineImage ii = new InlineImage(cid, dataSource);
		 
		if (inlineEmbeds == null) {
			inlineEmbeds = new HashMap<String, InlineImage>();
			map.put("inlineEmbeds", inlineEmbeds);
		}
		inlineEmbeds.put(name, ii);
		infos.set(map);

		return "cid:" + cid;
	}

    /**
     * Can be of the form xxx <m@m.com>
     *
     * @param replyTo
     */
    public static void setReplyTo(Object replyTo) {
        HashMap<String, Object> map = infos.get();
        if (map == null) {
            throw new UnexpectedException("Mailer not instrumented ?");
        }
        map.put("replyTo", replyTo);
        infos.set(map);
    }

    public static void setCharset(String bodyCharset) {
        HashMap<String, Object> map = infos.get();
        if (map == null) {
            throw new UnexpectedException("Mailer not instrumented ?");
        }
        map.put("charset", bodyCharset);
        infos.set(map);
    }

    @SuppressWarnings("unchecked")
    public static void addHeader(String key, String value) {
        HashMap<String, Object> map = infos.get();
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

    @SuppressWarnings("unchecked")
    public static Future<Boolean> send(Object... args) {
        try {
            final HashMap<String, Object> map = infos.get();
            if (map == null) {
                throw new UnexpectedException("Mailer not instrumented ?");
            }

            // Body character set
            final String charset = (String) infos.get().get("charset");

            // Headers
            final Map<String, String> headers = (Map<String, String>) infos.get().get("headers");

            // Subject
            final String subject = (String) infos.get().get("subject");

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

            final Map<String, Object> templateHtmlBinding = new HashMap<String, Object>();
            final Map<String, Object> templateTextBinding = new HashMap<String, Object>();
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
            final List<Object> recipientList = (List<Object>) infos.get().get("recipients");
            // From
            final Object from = infos.get().get("from");
            final Object replyTo = infos.get().get("replyTo");

            Email email = null;
            if (infos.get().get("attachments") == null && infos.get().get("inlineEmbeds") == null) {
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
            }
            email.setCharset("utf-8");

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


            List<Object> ccsList = (List<Object>) infos.get().get("ccs");
            if (ccsList != null) {
                for (Object cc : ccsList) {
                    email.addCc(cc.toString());
                }
            }

            List<Object> bccsList = (List<Object>) infos.get().get("bccs");
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
        } catch (InterruptedException e) {
            Logger.error(e, "Error while waiting Mail.send result");
        } catch (ExecutionException e) {
            Logger.error(e, "Error while waiting Mail.send result");
        }
        return false;
    }
}
