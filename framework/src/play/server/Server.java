package play.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import play.Invoker;
import play.Logger;
import play.Play;
import play.mvc.ActionInvoker;
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
            request.method = http.getRequestMethod();
            request.body = http.getRequestBody();
            
            // Response
            Response response = new Response();
            response.out = http.getResponseBody();

            Invoker.invoke(new PlayInvocation(request, response));
            
            http.close();
        }
        
    }
    
    static class PlayInvocation extends Thread {
        
        Request request;
        Response response;
        
        public PlayInvocation(Request request, Response response) {
            this.request = request;
            this.response = response;
        }

        @Override
        public void run() {
            ActionInvoker.invoke(request, response);
        }
        
    }
    
}
