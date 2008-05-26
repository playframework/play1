package play.server;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import play.Invoker;
import play.Logger;
import play.Play;
import play.mvc.ActionInvoker;
import play.mvc.Http.Header;
import play.mvc.Http.Request;
import play.mvc.Http.Response;

public class Server {
    
    public static void main(String[] args) throws Exception {   
        
        File root = new File(System.getProperty("application.path"));
        Play.init(root);
        
        // HttpServer
        HttpServer httpServer = HttpServer.create(new InetSocketAddress("localhost", 9000), 0);
        httpServer.start();   
        httpServer.createContext("/", new Handler());
        
        Logger.info("play! is listening on 9000...");
        
    }

    static class Handler implements HttpHandler {

        public void handle(HttpExchange http) throws IOException {
            
            // Request
            Request request = new Request();
            String uri = http.getRequestURI().toString();
            if(uri.indexOf("?")>-1) {
                request.path = uri.substring(0, uri.indexOf("?"));
                request.querystring = uri.substring(uri.indexOf("?")+1, uri.length());
            } else {
                request.path = uri;
                request.querystring = "";
            }
            request.contentType = http.getRequestHeaders().getFirst("Content-Type");
            request.method = http.getRequestMethod();
            request.body = http.getRequestBody();
            request.domain = http.getLocalAddress().getHostName();
            request.port = http.getLocalAddress().getPort();
            request.secure = false;
            
            // Response
            Response response = new Response();
            response.out = new ResponseStream(response, http);

            try {
                Invoker.invokeInThread(new RequestInvocation(request, response));
            } catch (Throwable ex) {
                response.status = 500;
                response.contentType = "text/plain";
                ex.printStackTrace(new PrintStream(response.out));                
            } finally {
                try {
                    response.out.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
            
        }
        
    }
    
    static class ResponseStream extends OutputStream { 
        
        HttpExchange http;
        Response response;
        OutputStream out;
        
        public ResponseStream(Response response, HttpExchange http) {
            this.http = http;
            this.response = response;            
        }

        @Override
        public void write(int b) throws IOException {
            if(out == null) {
                flushHeaders();
                http.sendResponseHeaders(response.status, 0L);
                out = http.getResponseBody();                 
            }
            out.write(b);
        }

        @Override
        public void close() throws IOException {            
            super.close();
            if(out == null) {
                flushHeaders();
                http.sendResponseHeaders(response.status, 0L);
                out = http.getResponseBody();
            }
            out.flush();
            out.close();
        }
        
        void flushHeaders() {
            Headers headers = http.getResponseHeaders();
            if(response.contentType != null) {
                headers.set("Content-Type", response.contentType+(response.contentType.startsWith("text/")?"; charset=utf-8":""));
            }
            for(Header h : response.headers.values()) {
                for(String v : h.values) {
                    headers.add(h.name, v);
                }
            }
        }
       
    }
    
    static class RequestInvocation extends Invoker.Invocation {
        
        Request request;
        Response response;
        
        public RequestInvocation(Request request, Response response) {
            this.request = request;
            this.response = response;
        }

        @Override
        public void execute() {
            ActionInvoker.invoke(request, response);               
        }
        
    }
}
