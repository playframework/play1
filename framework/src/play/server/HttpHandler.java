package play.server;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.channels.FileChannel;
import java.util.Map;
import org.apache.asyncweb.common.DefaultCookie;
import org.apache.asyncweb.common.DefaultHttpResponse;
import org.apache.asyncweb.common.HttpHeaderConstants;
import org.apache.asyncweb.common.HttpRequest;
import org.apache.asyncweb.common.HttpResponseStatus;
import org.apache.asyncweb.common.MutableHttpResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.mina.common.IdleStatus;
import org.apache.mina.common.IoBuffer;
import org.apache.mina.common.IoFutureListener;
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.WriteFuture;
import play.Invoker;
import play.Play;
import play.mvc.ActionInvoker;
import play.mvc.Http;
import play.mvc.Http.Request;
import play.mvc.Http.Response;

public class HttpHandler implements IoHandler {
    private static Log log = LogFactory.getLog(HttpHandler.class);

    public void messageReceived(IoSession session, Object message) throws Exception {
        HttpRequest minaRequest = (HttpRequest) message;
        MutableHttpResponse minaResponse = new DefaultHttpResponse();
        URI uri = minaRequest.getRequestUri();
        if (uri.getPath().startsWith("/public/")) {
            log.info("Serve static: "+uri.getPath());
            serveStatic (session,minaRequest,minaResponse);
        } else {
            Invoker.invoke(new MinaInvocation(session, minaRequest, minaResponse));
        }
    }

    public void serveStatic (IoSession session,HttpRequest request, MutableHttpResponse response) throws IOException {
        URI uri = request.getRequestUri();
        File target = new File(Play.root+"/"+uri.getPath());
        if ( !target.exists() && !target.isFile())
            serve404(session,request, response);
        else {
            RandomAccessFile raf = new RandomAccessFile (target,"r");
            byte[] buffer = new byte[(int)raf.length()];
            raf.read(buffer);
            raf.close();
            response.setStatus(HttpResponseStatus.OK);
            response.setContent(IoBuffer.wrap(buffer));
            writeResponse(session, request, response);
        }
    }
    
    public static void serve404 (IoSession session, HttpRequest request, MutableHttpResponse response){
        response.setStatus(HttpResponseStatus.NOT_FOUND);
        response.setContent(IoBuffer.wrap("Page not found".getBytes()));
        writeResponse(session, request, response);
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
                log.info("File sent");
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
            log.error("Caught ", cause);
        }
        session.close();
    }
    
    class MinaInvocation extends Invoker.Invocation {
        private IoSession session;
        private HttpRequest minaRequest;
        private MutableHttpResponse minaResponse;

        public MinaInvocation(IoSession session, HttpRequest minaRequest, MutableHttpResponse minaResponse) {
            this.session = session;
            this.minaRequest = minaRequest;
            this.minaResponse = minaResponse;
        }
       
        
        public void execute() {
            URI uri = minaRequest.getRequestUri();
            String host = minaRequest.getHeader("Host");
            IoBuffer buffer = (IoBuffer) minaRequest.getContent();
            Request request = new Request();
            request.contentType = minaRequest.getHeader("Content-Type");
            request.method = minaRequest.getMethod().toString();
            request.body = buffer.asInputStream();
            request.domain = ((InetSocketAddress)session.getLocalAddress()).getHostName();
            request.port =  ((InetSocketAddress)session.getLocalAddress()).getPort();
            request.secure = false;
            request.path=uri.getPath();
            request.querystring=uri.getQuery()==null ? "" : uri.getQuery();
            
            Response response = new Response();            
            response.out = new ByteArrayOutputStream ();
            
            try {
                ActionInvoker.invoke(request, response);
                response.out.flush();
            } catch (Exception e) {
                // Gen 500 / 404
                log.error ("Invocation error: ",e);
            }
            log.info("Invoke: "+uri.getPath()+": "+response.status);
            if (response.status==404) {
                    HttpHandler.serve404(session, minaRequest, minaResponse);
                    return;
            }
            minaResponse.setContent(IoBuffer.wrap(((ByteArrayOutputStream)response.out).toByteArray()));
            if (response.contentType!=null)
                minaResponse.setHeader("Content-Type", response.contentType+(response.contentType.startsWith("text/")?"; charset=utf-8":""));
            else
                 minaResponse.setHeader("Content-Type", "text/plain;charset=utf-8");
            minaResponse.setStatus(HttpResponseStatus.forId(response.status));
            Map<String, Http.Header> headers = response.headers;
            for (String key : headers.keySet()) {
                Http.Header hd = headers.get((key));
                for (String value : hd.values)
                    minaResponse.addHeader(key, value);
            }

            Map<String,Http.Cookie> cookies = response.cookies;
            for (String key : cookies.keySet()) {
                Http.Cookie c = cookies.get(key);
                DefaultCookie m = new DefaultCookie (c.name);
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
