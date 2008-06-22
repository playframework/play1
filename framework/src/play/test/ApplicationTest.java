package play.test;

import static org.junit.Assert.*;

import java.io.UnsupportedEncodingException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import play.Invoker;
import play.classloading.enhancers.ControllersEnhancer.ControllerInstrumentation;
import play.exceptions.UnexpectedException;
import play.mvc.ActionInvoker;
import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.mvc.results.Result;

public abstract class ApplicationTest {

    // Requests
    public static Response GET(String url) {
        Request request = newRequest();
        String path = "";
        String queryString = "";
        if (url.contains("?")) {
            path = url.substring(0, url.indexOf("?"));
            queryString = url.substring(url.indexOf("?") + 1);
        } else {
            path = url;
        }
        request.method = "GET";
        request.url = url;
        request.path = path;
        request.querystring = queryString;
        request.body = new ByteArrayInputStream(new byte[0]);
        //
        Response response = newResponse();
        //
        makeRequest(request, response);
        //
        return response;
    }

    public static void makeRequest(final Request request, final Response response) {
        inPlay(new Invoker.Invocation() {

            @Override
            public void execute() throws Exception {
                ActionInvoker.invoke(request, response);
                response.out.flush();
            }
        });
    }
    
    public static void inPlay(Invoker.Invocation invocation) {
        Invoker.invokeInThread(invocation);
    }
    
    public static Result invokeController(final ControllerInvocation invocation) {
        try {
            Invoker.invokeInThread(new Invoker.Invocation() {
                public void execute() throws Exception {
                    invocation.run();
                }
            });
        } catch(UnexpectedException r) {
            if(r.getCause() instanceof Result) {
                return (Result)r.getCause();
            }            
        }
        return null;
    }

    public static Response newResponse() {
        Response response = new Response();
        response.out = new ByteArrayOutputStream();
        return response;
    }

    public static Request newRequest() {
        Request request = new Request();
        request.domain = "test.playframework.org";
        request.port = 80;
        request.method = "GET";
        request.path = "/";
        request.querystring = "";
        return request;
    }
    
    // Assertions
    
    public static void assertIsOk(Response response) {
        assertEquals((Object) 200, response.status);
    }
    
    public static void assertIsNotFound(Response response) {
        assertEquals((Object) 404, response.status);
    }
    
    public static void assertContentEquals(String content, Response response) {
        assertEquals(content, getContent(response));
    }
    
    public static String getContent(Response response) {
        byte[] data = ((ByteArrayOutputStream)response.out).toByteArray();
        try {
            return new String(data, "utf-8");
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    // Utils
    
    public void sleep(int seconds) {
        try {
            Thread.sleep(seconds * 1000);
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    // Some classes
       
    public abstract static class ControllerInvocation {

        public abstract void execute();
        
        public void run() {
            ControllerInstrumentation.initActionCall();
            execute();            
        }
        
    }
    
}
