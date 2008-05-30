package play.server;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.Map;
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
import org.apache.mina.common.IoFutureListener;
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.WriteFuture;
import play.Invoker;
import play.Logger;
import play.Play;
import play.exceptions.PlayException;
import play.mvc.ActionInvoker;
import play.mvc.Http;
import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.templates.TemplateLoader;

public class HttpHandler implements IoHandler {

    public void messageReceived(IoSession session, Object message) throws Exception {
        HttpRequest minaRequest = (HttpRequest) message;
        MutableHttpResponse minaResponse = new DefaultHttpResponse();
        URI uri = minaRequest.getRequestUri();
        if (uri.getPath().startsWith("/public/")) {
            Logger.debug("Serve static: " + uri.getPath());
            serveStatic(session, minaRequest, minaResponse);
        } else {
            Invoker.invoke(new MinaInvocation(session, minaRequest, minaResponse));
        }
    }

    public void serveStatic(IoSession session, HttpRequest request, MutableHttpResponse response) throws IOException {
        URI uri = request.getRequestUri();
        File target = new File(Play.applicationPath + "/" + uri.getPath());
        if (!target.exists() && !target.isFile()) {
            serve404(session, request, response);
        } else {
            RandomAccessFile raf = new RandomAccessFile(target, "r");
            byte[] buffer = new byte[(int) raf.length()];
            raf.read(buffer);
            raf.close();
            response.setStatus(HttpResponseStatus.OK);
            response.setContent(IoBuffer.wrap(buffer));
            writeResponse(session, request, response);
        }
    }

    public static void serve404(IoSession session, HttpRequest request, MutableHttpResponse response) {
        response.setStatus(HttpResponseStatus.NOT_FOUND);
        response.setContent(IoBuffer.wrap("Page not found".getBytes()));
        writeResponse(session, request, response);
    }

    public static void serve500(Exception e, IoSession session, HttpRequest request, MutableHttpResponse response) {
        Map<String, Object> binding = new HashMap<String, Object>();
        if(!(e instanceof PlayException)) {
            e = new play.exceptions.UnexpectedException(e);
        }
        binding.put("exception", e);
        response.setStatus(HttpResponseStatus.forId(500));
        response.setContentType("text/html");
        try {
            String errorHtml = TemplateLoader.load("templates/error.html").render(binding);
            response.setContent(IoBuffer.wrap(errorHtml.getBytes("utf-8")));
            writeResponse(session, request, response);
            e.printStackTrace();
        } catch (Exception ex) {
            ex.printStackTrace();
            try {
                response.setContent(IoBuffer.wrap("Internal Error (check logs)".getBytes("utf-8")));
                writeResponse(session, request, response);
            } catch (UnsupportedEncodingException fex) {
                fex.printStackTrace();
            }
        }
    }

    public static void writeResponse(IoSession session, HttpRequest req, MutableHttpResponse res) {
        res.normalize(req);
        WriteFuture future = session.write(res);
        if (!HttpHeaderConstants.VALUE_KEEP_ALIVE.equalsIgnoreCase(res.getHeader(HttpHeaderConstants.KEY_CONNECTION))) {
            future.addListener(IoFutureListener.CLOSE);
        }
    }

    public void messageSent(IoSession session, Object message) throws Exception {
        if (message instanceof DefaultHttpResponse) {
            if (session.getAttribute("channel") != null) {
                FileChannel channel = (FileChannel) session.getAttribute("channel");
                session.removeAttribute("channel");
                session.write(channel);
                Logger.debug("File sent");
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
            Logger.error("Caught ", cause);
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
            URI uri = minaRequest.getRequestUri();
            IoBuffer buffer = (IoBuffer) minaRequest.getContent();
            Request request = new Request();
            if (minaRequest.getHeader("Content-Type") != null) {
                request.contentType = minaRequest.getHeader("Content-Type").split(";")[0].trim().toLowerCase();
            } else {
                request.contentType = "text/html";
            }
            request.method = minaRequest.getMethod().toString();
            if (minaRequest.getFileContent()==null)
            	request.body = buffer.asInputStream();
            else
            	request.body = new FileInputStream (minaRequest.getFileContent());
            request.domain = ((InetSocketAddress) session.getLocalAddress()).getHostName();
            request.port = ((InetSocketAddress) session.getLocalAddress()).getPort();
            request.secure = false;
            request.path = uri.getPath();
            request.querystring = uri.getQuery() == null ? "" : uri.getQuery();

            for (String key : minaRequest.getHeaders().keySet()) {
                Http.Header hd = new Http.Header();
                hd.name = key.toLowerCase();
                hd.values = minaRequest.getHeaders().get(key);
                request.headers.put(hd.name, hd);
            }

            for (Cookie cookie : minaRequest.getCookies()) {
                Http.Cookie playCookie = new Http.Cookie();
                playCookie.domain=cookie.getDomain();
                playCookie.name=cookie.getName();
                playCookie.path=cookie.getPath();
                playCookie.secure=cookie.isSecure();
                playCookie.value=cookie.getValue();
                request.cookies.put(playCookie.name, playCookie);
            }
            
            Response response = new Response();
            response.out = new ByteArrayOutputStream();

            ActionInvoker.invoke(request, response);
            response.out.flush();
            Logger.debug("Invoke: " + uri.getPath() + ": " + response.status);
            if (response.status == 404) {
                HttpHandler.serve404(session, minaRequest, minaResponse);
                return;
            }
            minaResponse.setContent(IoBuffer.wrap(((ByteArrayOutputStream) response.out).toByteArray()));
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
                Http.Cookie c = cookies.get(key);
                DefaultCookie m = new DefaultCookie(c.name);
                m.setDomain(c.domain);
                m.setPath(c.path);
                m.setSecure(c.secure);
                m.setValue(c.value);
                minaResponse.addCookie(m);
            }
            HttpHandler.writeResponse(session, minaRequest, minaResponse);
        }
    }
}
