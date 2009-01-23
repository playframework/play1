package play.server;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
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
import play.Play;
import play.exceptions.EmptyAppException;
import play.exceptions.PlayException;
import play.libs.MimeTypes;
import play.mvc.ActionInvoker;
import play.mvc.Http;
import play.mvc.Router;
import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.mvc.results.NotFound;
import play.mvc.results.RenderStatic;
import play.templates.TemplateLoader;
import play.vfs.FileSystemFile;
import play.vfs.VirtualFile;

/**
 * HTTP Handler
 */
public class HttpHandler implements IoHandler {

    public void messageReceived(IoSession session, Object message) throws Exception {
        MutableHttpRequest minaRequest = (MutableHttpRequest) message;
        MutableHttpResponse minaResponse = new DefaultHttpResponse();
        Request request = null;
        try {
            request = parseRequest(minaRequest, session);
        } catch (NotFound e) {
            serve404(session, minaResponse, minaRequest, e);
            return;
        } catch (RenderStatic e) {
            serveStatic(session, minaResponse, minaRequest, e);
            return;
        } catch (EmptyAppException e) {
            serve500(e, session, minaRequest, minaResponse);
            return;
        }
        Response response = new Response();
        response.out = new ByteArrayOutputStream();

        if (Play.mode == Play.Mode.DEV) {
            Invoker.invokeInThread(new MinaInvocation(session, minaRequest, minaResponse, request, response));
        } else {
            Invoker.invoke(new MinaInvocation(session, minaRequest, minaResponse, request, response));
        }
    }

    public static Request parseRequest(MutableHttpRequest minaRequest, IoSession session) throws IOException {
        URI uri = minaRequest.getRequestUri();
        Request request = new Request();
        request.method = minaRequest.getMethod().toString();
        request.path = URLDecoder.decode(uri.getRawPath(), "utf-8");
        request.querystring = uri.getQuery() == null ? "" : uri.getRawQuery();
        Http.Request.current.set(request);

        Router.detectChanges();
        Router.route(request);

        IoBuffer buffer = (IoBuffer) minaRequest.getContent();

        if (minaRequest.getHeader("Content-Type") != null) {
            request.contentType = minaRequest.getHeader("Content-Type").split(";")[0].trim().toLowerCase();
        } else {
            request.contentType = "text/html";
        }

        if (minaRequest.getFileContent() == null) {
            request.body = buffer.asInputStream();
        } else {
            request.body = new FileInputStream(minaRequest.getFileContent());
        }
        request.secure = false;

        request.url = minaRequest.getRequestUri().toString();
        request.host = minaRequest.getHeader("host");
        if (request.host.contains(":")) {
            request.port = Integer.parseInt(request.host.split(":")[1]);
            request.domain = request.host.split(":")[0];
        } else {
            request.port = 80;
            request.domain = request.host;
        }
        request.remoteAddress = ((InetSocketAddress) session.getRemoteAddress()).getAddress().getHostAddress();

        for (String key : minaRequest.getHeaders().keySet()) {
            Http.Header hd = new Http.Header();
            hd.name = key.toLowerCase();
            hd.values = minaRequest.getHeaders().get(key);
            request.headers.put(hd.name, hd);
        }
        request.resolveFormat();

        for (Cookie cookie : minaRequest.getCookies()) {
            Http.Cookie playCookie = new Http.Cookie();
            playCookie.name = cookie.getName();
            playCookie.path = cookie.getPath();
            playCookie.secure = cookie.isSecure();
            playCookie.value = cookie.getValue();
            request.cookies.put(playCookie.name, playCookie);
        }
        return request;
    }

    public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
        if (!(cause instanceof IOException)) {
            Logger.error(cause, "Caught in Server !");
        }
        session.close();
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
                        if (!HttpHeaderConstants.VALUE_KEEP_ALIVE.equalsIgnoreCase(res.getHeader(HttpHeaderConstants.KEY_CONNECTION))) {
                            future.getSession().close();
                        }
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

    public static void attachFile(IoSession session, MutableHttpResponse response, VirtualFile file) throws IOException {
        response.setStatus(HttpResponseStatus.OK);
        response.setHeader("Content-Type", MimeTypes.getMimeType(file.getName()));
        if (file instanceof FileSystemFile) {
            session.setAttribute("file", file.channel());
            response.setHeader(HttpHeaderConstants.KEY_CONTENT_LENGTH, "" + file.length());
        } else {
            response.setContent(IoBuffer.wrap(file.content()));
        }
    }

    public void serveStatic(IoSession session, MutableHttpResponse minaResponse, HttpRequest minaRequest, RenderStatic renderStatic) throws IOException {
        VirtualFile file = Play.getVirtualFile(renderStatic.file);
        if (file == null || file.isDirectory() || !file.exists()) {
            serve404(session, minaResponse, minaRequest, new NotFound(renderStatic.file + " not found"));
        } else {
            if (Play.mode == Play.Mode.DEV) {
                minaResponse.setHeader("Cache-Control", "no-cache");
                attachFile(session, minaResponse, file);
            } else {
                long last = file.lastModified();
                String etag = last + "-" + file.hashCode();
                if (!isModified(etag, last, minaRequest)) {
                    minaResponse.setHeader("Etag", etag);
                    minaResponse.setStatus(HttpResponseStatus.NOT_MODIFIED);
                } else {
                    minaResponse.setHeader("Last-Modified", getHttpDateFormatter().format(new Date(last)));
                    minaResponse.setHeader("Cache-Control", "max-age=3600");
                    minaResponse.setHeader("Etag", etag);
                    attachFile(session, minaResponse, file);
                }
            }
            writeResponse(session, minaRequest, minaResponse);
        }
    }

    public static void serve404(IoSession session, MutableHttpResponse minaResponse, HttpRequest minaRequest, NotFound e) {
        Logger.warn("404 -> %s %s (%s)", minaRequest.getMethod(), minaRequest.getRequestUri(), e.getMessage());
        minaResponse.setStatus(HttpResponseStatus.NOT_FOUND);
        minaResponse.setContentType("text/html");
        Map<String, Object> binding = new HashMap<String, Object>();
        String errorHtml = TemplateLoader.load("errors/404.html").render(binding);
        try {
            minaResponse.setContent(IoBuffer.wrap(errorHtml.getBytes("utf-8")));
        } catch (UnsupportedEncodingException fex) {
            Logger.error(fex, "(utf-8 ?)");
        }
        writeResponse(session, minaRequest, minaResponse);
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
                    Date browserDate = getHttpDateFormatter().parse(request.getHeader("If-Modified-Since"));
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

    public static void serve500(Exception e, IoSession session, HttpRequest request, MutableHttpResponse response) {
        try {
            Map<String, Object> binding = new HashMap<String, Object>();
            if (!(e instanceof PlayException)) {
                e = new play.exceptions.UnexpectedException(e);
            }
            // Flush some cookies
            try {
                Map<String, Http.Cookie> cookies = Response.current().cookies;
                for (String key : cookies.keySet()) {
                    Http.Cookie cookie = cookies.get(key);
                    if (cookie.sendOnError) {
                        DefaultCookie c = new DefaultCookie(cookie.name, cookie.value);
                        c.setSecure(cookie.secure);
                        c.setPath(cookie.path);
                        response.addCookie(c);
                    }
                }
            } catch(Exception exx) {
                // humm ?
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
        } catch (Throwable exxx) {
            try {
                response.setContent(IoBuffer.wrap("Internal Error (check logs)".getBytes("utf-8")));
                writeResponse(session, request, response);
            } catch (UnsupportedEncodingException fex) {
                Logger.error(fex, "(utf-8 ?)");
            }
            if(exxx instanceof RuntimeException) throw (RuntimeException)exxx;
            throw new RuntimeException(exxx);
        }
    }

    public static void writeResponse(IoSession session, HttpRequest req, MutableHttpResponse res) {
    	res.setHeader("Server", "Play! Framework");
        res.normalize(req);
        WriteFuture future = session.write(res);
        if ((session.getAttribute("file") == null) && !HttpHeaderConstants.VALUE_KEEP_ALIVE.equalsIgnoreCase(res.getHeader(HttpHeaderConstants.KEY_CONNECTION))) {
            future.addListener(IoFutureListener.CLOSE);
        }
    }

    static class MinaInvocation extends Invoker.Invocation {

        private IoSession session;
        private MutableHttpRequest minaRequest;
        private MutableHttpResponse minaResponse;
        private Request request;
        private Response response;

        public MinaInvocation(IoSession session, MutableHttpRequest minaRequest, MutableHttpResponse minaResponse, Request request, Response response) {
            super();
            this.minaRequest = minaRequest;
            this.minaResponse = minaResponse;
            this.request = request;
            this.response = response;
            this.session = session;
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

        @Override
        public void execute() throws Exception {
            if (session.isClosing()) {
                return;
            }
            ActionInvoker.invoke(request, response);

            response.out.flush();
            Logger.trace("Invoke: " + request.path + ": " + response.status);
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
                if (cookie.maxAge!=null)
                	c.setMaxAge(cookie.maxAge);
                minaResponse.addCookie(c);
            }
            if (!response.headers.containsKey("cache-control")) {
                minaResponse.setHeader("Cache-Control", "no-cache");
            }
            HttpHandler.writeResponse(session, minaRequest, minaResponse);
        }
    }
    private static ThreadLocal<SimpleDateFormat> formatter = new ThreadLocal<SimpleDateFormat>();

    public static SimpleDateFormat getHttpDateFormatter() {
        if (formatter.get() == null) {
            formatter.set(new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US));
            formatter.get().setTimeZone(TimeZone.getTimeZone("GMT"));
        }
        return formatter.get();
    }
}
