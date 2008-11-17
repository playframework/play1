package play.server;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.channels.FileChannel;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import org.apache.asyncweb.common.Cookie;
import org.apache.asyncweb.common.DefaultCookie;
import org.apache.asyncweb.common.DefaultHttpResponse;
import org.apache.asyncweb.common.HttpHeaderConstants;
import org.apache.asyncweb.common.HttpRequest;
import org.apache.asyncweb.common.HttpResponseStatus;
import org.apache.asyncweb.common.MutableHttpRequest;
import org.apache.asyncweb.common.MutableHttpResponse;
import org.apache.mina.common.IdleStatus;
import org.apache.mina.common.IoBuffer;
import org.apache.mina.common.IoFuture;
import org.apache.mina.common.IoFutureListener;
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.WriteFuture;
import play.Invoker;
import play.Logger;
import play.libs.MimeTypes;
import play.Play;
import play.exceptions.EmptyAppException;
import play.exceptions.PlayException;
import play.mvc.ActionInvoker;
import play.mvc.Http;
import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.templates.TemplateLoader;
import play.vfs.FileSystemFile;
import play.vfs.VirtualFile;

public class HttpHandler implements IoHandler {

    public void messageReceived(IoSession session, Object message) throws Exception {
        HttpRequest minaRequest = (HttpRequest) message;
        MutableHttpResponse minaResponse = new DefaultHttpResponse();
        URI uri = minaRequest.getRequestUri();
        if (uri.getPath().startsWith("/public/")) {
            Logger.trace("Serve static: " + uri.getPath());
            serveStatic(session, minaRequest, minaResponse);
        } else if (Play.mode == Play.Mode.DEV) {
            Invoker.invokeInThread(new MinaInvocation(session, minaRequest, minaResponse));
        } else {
            Invoker.invoke(new MinaInvocation(session, minaRequest, minaResponse));
        }
    }

    public void serveStatic(IoSession session, HttpRequest request, MutableHttpResponse response) throws IOException {
        URI uri = request.getRequestUri();
        VirtualFile file = VirtualFile.search(Play.staticResources, uri.getPath().substring("/public/".length()));
        if (file == null || file.isDirectory()) {
            serve404(session, request, response);
        } else {
            if (Play.mode == Play.Mode.DEV) {
                response.setHeader("Cache-Control", "no-cache");
                attachFile(session, response, file);
            } else {
                long last = file.lastModified();
                String etag = last + "-" + file.hashCode();
                if (!isModified(etag, last, request)) {
                    response.setHeader("Etag", etag);
                    response.setStatus(HttpResponseStatus.NOT_MODIFIED);
                } else {
                    response.setHeader("Last-Modified", formatter.format(new Date(last)));
                    response.setHeader("Cache-Control", "max-age=3600");
                    response.setHeader("Etag", etag);
                    attachFile(session, response, file);
                }
            }
            writeResponse(session, request, response);
        }
    }

    public static void attachFile(IoSession session, MutableHttpResponse response, VirtualFile file) throws IOException {
        response.setStatus(HttpResponseStatus.OK);
        response.setHeader("Content-Type",MimeTypes.getMimeType(file.getName()));
        if (file instanceof FileSystemFile) {
            session.setAttribute("file", file.channel());
            response.setHeader(HttpHeaderConstants.KEY_CONTENT_LENGTH, "" + file.length());
        } else {
            response.setContent(IoBuffer.wrap(file.content()));
        }
    }

    public static boolean isModified(String etag, long last, HttpRequest request) {
        if (!(request.getHeaders().containsKey("If-None-Match") && request.getHeaders().containsKey("If-Modified-Since"))) {
            return true;
        } else {
            String browserEtag = request.getHeader("If-None-Match");
            if (!browserEtag.equals(etag)) {
                return true;
            } else {
                try {
                    Date browserDate = formatter.parse(request.getHeader("If-Modified-Since"));
                    if (browserDate.getTime() >= last) {
                        return false;
                    }
                } catch (ParseException ex) {
                    Logger.error("Can't parse date", ex);
                }
                return true;
            }
        }
    }

    public static void serve404(IoSession session, HttpRequest request, MutableHttpResponse response) {
        Logger.warn("404 -> %s %s", request.getMethod(), request.getRequestUri());
        response.setStatus(HttpResponseStatus.NOT_FOUND);
        response.setContentType("text/html");
        Map<String, Object> binding = new HashMap<String, Object>();
        String errorHtml = TemplateLoader.load("errors/404.html").render(binding);
        try {
            response.setContent(IoBuffer.wrap(errorHtml.getBytes("utf-8")));
        } catch (UnsupportedEncodingException fex) {
            Logger.error(fex, "(utf-8 ?)");
        }
        writeResponse(session, request, response);
    }

    public static void serve500(Exception e, IoSession session, HttpRequest request, MutableHttpResponse response) {
        Map<String, Object> binding = new HashMap<String, Object>();
        if (!(e instanceof PlayException)) {
            e = new play.exceptions.UnexpectedException(e);
        }
        // Empty app : 
        if (e instanceof EmptyAppException) {
            try {
                response.setStatus(HttpResponseStatus.forId(200));
                response.setContentType("text/html");
                String errorHtml = TemplateLoader.load("errors/empty.html").render(binding);
                response.setContent(IoBuffer.wrap(errorHtml.getBytes("utf-8")));
                writeResponse(session, request, response);
                return;
            } catch (Throwable ex) {
                Logger.error(ex, "Internal Server Error (500)");
                try {
                    response.setContent(IoBuffer.wrap("Internal Error (check logs)".getBytes("utf-8")));
                    writeResponse(session, request, response);
                } catch (UnsupportedEncodingException fex) {
                    Logger.error(fex, "(utf-8 ?)");
                }
            }
        }
        binding.put("exception", e);
        boolean ajax = "XMLHttpRequest".equals(request.getHeader("X-Requested-With"));
        response.setStatus(HttpResponseStatus.forId(500));
        if (ajax) {
            response.setContentType("text/plain");
        } else {
            response.setContentType("text/html");
        }
        try {
            String errorHtml = TemplateLoader.load("errors/500." + (ajax ? "txt" : "html")).render(binding);
            response.setContent(IoBuffer.wrap(errorHtml.getBytes("utf-8")));
            writeResponse(session, request, response);
            Logger.error(e, "Internal Server Error (500)");
        } catch (Throwable ex) {
            Logger.error(e, "Internal Server Error (500)");
            Logger.error(ex, "Error during the 500 response generation");
            try {
                response.setContent(IoBuffer.wrap("Internal Error (check logs)".getBytes("utf-8")));
                writeResponse(session, request, response);
            } catch (UnsupportedEncodingException fex) {
                Logger.error(fex, "(utf-8 ?)");
            }
        }
    }

    public static void writeResponse(IoSession session, HttpRequest req, MutableHttpResponse res) {
        res.normalize(req);
        WriteFuture future = session.write(res);
        if ( (session.getAttribute("file")==null) && !HttpHeaderConstants.VALUE_KEEP_ALIVE.equalsIgnoreCase(res.getHeader(HttpHeaderConstants.KEY_CONNECTION)))
	            future.addListener(IoFutureListener.CLOSE);
    }

    public void messageSent(IoSession session, Object message) throws Exception {
        if (message instanceof DefaultHttpResponse) {
            if (session.getAttribute("file") != null) {
                FileChannel channel = ((FileChannel) session.getAttribute("file"));
                WriteFuture future = session.write(channel);
                final DefaultHttpResponse res = (DefaultHttpResponse) message;
                future.addListener(new IoFutureListener<IoFuture>() {
                    public void operationComplete(IoFuture future) {
                        FileChannel channel = (FileChannel) future.getSession().getAttribute("file");
                        future.getSession().removeAttribute("file");
                        try {
                            channel.close();
                        } catch (IOException e) {
                            Logger.error(e, "Unexpected error");
                        }
                        if (!HttpHeaderConstants.VALUE_KEEP_ALIVE.equalsIgnoreCase(res.getHeader(HttpHeaderConstants.KEY_CONNECTION)))
                        	 future.getSession().close();
                    }
                });
            } 
        }
    }

    public void sessionClosed(IoSession session) throws Exception {
    }

    public void sessionCreated(IoSession session) throws Exception {
    }

    public void sessionIdle(IoSession session, IdleStatus status) throws Exception {
        session.close();
    }

    public void sessionOpened(IoSession session) throws Exception {
        session.getConfig().setIdleTime(IdleStatus.BOTH_IDLE, 300);
    }

    public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
        if (!(cause instanceof IOException)) {
            Logger.error(cause,"Caught ");
        }
        session.close();
    }

    class MinaInvocation extends Invoker.Invocation {

        private IoSession session;
        private MutableHttpRequest minaRequest;
        private MutableHttpResponse minaResponse;

        public MinaInvocation(IoSession session, HttpRequest minaRequest, MutableHttpResponse minaResponse) {
            this.session = session;
            this.minaRequest = (MutableHttpRequest) minaRequest;
            this.minaResponse = minaResponse;
        }

        @Override
        public void run() {
            try {
                super.run();
            } catch (Exception e) {
                serve500(e, session, minaRequest, minaResponse);
                return;
            }
        }

        public void execute() throws Exception {
            if (session.isClosing()) {
                return;
            }
            URI uri = minaRequest.getRequestUri();
            IoBuffer buffer = (IoBuffer) minaRequest.getContent();
            Request request = new Request();
            if (minaRequest.getHeader("Content-Type") != null) {
                request.contentType = minaRequest.getHeader("Content-Type").split(";")[0].trim().toLowerCase();
                // todo : keep charset encoding ?
            } else {
                request.contentType = "text/html";
            }
            request.method = minaRequest.getMethod().toString();
            if (minaRequest.getFileContent() == null) {
                request.body = buffer.asInputStream();
            } else {
                request.body = new FileInputStream(minaRequest.getFileContent());
            }
            request.secure = false;
            request.path = uri.getPath();
            request.querystring = uri.getQuery() == null ? "" : uri.getRawQuery();
            request.url = minaRequest.getRequestUri().toString();
            request.host = minaRequest.getHeader("host");
            if(request.host.contains(":")) {
            	request.port=Integer.parseInt(request.host.split(":")[1]);
            	request.domain=request.host.split(":")[0];
            } else {
            	request.port=80;
            	request.domain=request.host;
            }            	
            request.remoteAddress = ((InetSocketAddress) session.getRemoteAddress()).getAddress().getHostAddress();

            for (String key : minaRequest.getHeaders().keySet()) {
                Http.Header hd = new Http.Header();
                hd.name = key.toLowerCase();
                hd.values = minaRequest.getHeaders().get(key);
                request.headers.put(hd.name, hd);                
            }

            for (Cookie cookie : minaRequest.getCookies()) {
                Http.Cookie playCookie = new Http.Cookie();
                playCookie.name = cookie.getName();
                playCookie.path = cookie.getPath();
                playCookie.secure = cookie.isSecure();
                playCookie.value = cookie.getValue();
                request.cookies.put(playCookie.name, playCookie);
            }

            Response response = new Response();
            response.out = new ByteArrayOutputStream();

            ActionInvoker.invoke(request, response);

            response.out.flush();
            Logger.trace("Invoke: " + uri.getPath() + ": " + response.status);
            //if (response.status == 404) {
            //    HttpHandler.serve404(session, minaRequest, minaResponse);
            //    return;
            //}
            if ((response.direct != null) && response.direct.isFile()) {
                session.setAttribute("file", new FileInputStream(response.direct).getChannel());
                response.setHeader(HttpHeaderConstants.KEY_CONTENT_LENGTH, "" + response.direct.length());
            } else {
                minaResponse.setContent(IoBuffer.wrap(((ByteArrayOutputStream) response.out).toByteArray()));
            }
            if (response.contentType != null) {
                minaResponse.setHeader("Content-Type", response.contentType + (response.contentType.startsWith("text/") ? "; charset=utf-8" : ""));
            } else {
                minaResponse.setHeader("Content-Type", "text/plain;charset=utf-8");
            }
            minaResponse.setStatus(HttpResponseStatus.forId(response.status));
            Map<String, Http.Header> headers = response.headers;
            for (String key : headers.keySet()) {
                Http.Header hd = headers.get((key));
                for (String value : hd.values) {
                    minaResponse.addHeader(key, value);
                }
            }           

            Map<String, Http.Cookie> cookies = response.cookies;
            for (String key : cookies.keySet()) {
                Http.Cookie cookie = cookies.get(key);
                DefaultCookie c = new DefaultCookie(cookie.name, cookie.value);
                c.setSecure(cookie.secure);
                c.setPath(cookie.path);
                minaResponse.addCookie(c);
            }
            if (!response.headers.containsKey("cache-control")) {
                minaResponse.setHeader("Cache-Control", "no-cache");
            }
            HttpHandler.writeResponse(session, minaRequest, minaResponse);
        }
    }
    public static SimpleDateFormat formatter = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);
    

    static {
        formatter.setTimeZone(TimeZone.getTimeZone("GMT"));
    }
}
