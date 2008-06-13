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
            //attachFile(session, response, target);
            if (Play.configuration.getProperty("mode", "dev").equals("dev")) {
                response.setHeader("Cache-Control", "no-cache");
                attachFile(session, response, target);
            } else {
                long last = target.lastModified();
                String etag = last+"-"+target.hashCode();
                if (!isModified(etag, last, request)) {
                    response.setHeader("Etag",etag);
                    response.setStatus(HttpResponseStatus.NOT_MODIFIED);
                } else {
                    response.setHeader("Last-Modified", formatter.format(new Date(last)));
                    response.setHeader("Cache-Control","max-age=3600");
                    response.setHeader("Etag",etag);
                    attachFile(session, response, target);
                }
            }
            writeResponse(session, request, response);
        }
    }

    public static void attachFile (IoSession session, MutableHttpResponse response, File target) throws IOException {
        RandomAccessFile raf = new RandomAccessFile(target, "r");
        response.setStatus(HttpResponseStatus.OK);
        session.setAttribute("file", raf);
        response.setHeader(HttpHeaderConstants.KEY_CONTENT_LENGTH, ""+raf.length());
        response.setHeader(HttpHeaderConstants.KEY_TRANSFER_CODING, "pppp");
    }
    
    public static boolean isModified (String etag, long last, HttpRequest request) {
       if (! (request.getHeaders().containsKey("If-None-Match") && request.getHeaders().containsKey("If-Modified-Since")))
           return true;
       else {
           String browserEtag = request.getHeader("If-None-Match");
           if (!browserEtag.equals(etag))
               return true;
           else {
                try {
                    Date browserDate = formatter.parse(request.getHeader("If-Modified-Since"));
                    if (browserDate.getTime()>=last)
                        return false;
                } catch (ParseException ex) {
                    Logger.error("Can't parse date", ex);
                }
                return true;
           }
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
    		if (session.getAttribute("file")!=null) {
    			FileChannel channel = ((RandomAccessFile) session.getAttribute("file")).getChannel();
    			WriteFuture future = session.write(channel);
    			future.addListener(new IoFutureListener<IoFuture> () {
					public void operationComplete(IoFuture future) {
						RandomAccessFile raf = (RandomAccessFile) future.getSession().getAttribute("file");
						future.getSession().removeAttribute("file");
						try {
							raf.close();
						} catch (IOException e) {
							Logger.debug(e);
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
            for(String key : cookies.keySet()) {
                Http.Cookie cookie = cookies.get(key);
                DefaultCookie c = new DefaultCookie(cookie.name,cookie.value);
                c.setSecure(cookie.secure);
                c.setPath(cookie.path);
                minaResponse.addCookie(c);
            }
            if (!response.headers.containsKey("cache-control"))
                minaResponse.setHeader("Cache-Control", "no-cache");
            HttpHandler.writeResponse(session, minaRequest, minaResponse);
        }
    }
    
    public static SimpleDateFormat formatter = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);
    static {
        formatter.setTimeZone(TimeZone.getTimeZone("GMT"));
    }
}
