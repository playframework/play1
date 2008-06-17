package play.test;

import static org.junit.Assert.*;

import java.io.UnsupportedEncodingException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import play.Invoker;
import play.Play;
import play.mvc.ActionInvoker;
import play.mvc.Http.Request;
import play.mvc.Http.Response;

public abstract class ApplicationTest {
    // Application management
    public static void start(File root) {
        Play.init(root);
    }

    public static void stop() {
        Play.stop();
    }
    // Requests
    public static Response GET(String url) throws Exception {
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
        invoke(request, response);
        //
        return response;
    }

    public static void invoke(final Request request, final Response response) throws Exception {
        Invoker.invokeInThread(new Invoker.Invocation() {

            @Override
            public void execute() throws Exception {
                ActionInvoker.invoke(request, response);
                response.out.flush();
            }
        });

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
    
}
