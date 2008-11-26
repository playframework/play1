package play.test;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.regex.Pattern;
import org.junit.After;
import org.junit.Before;
import play.Invoker.Invocation;
import play.classloading.enhancers.ControllersEnhancer.ControllerInstrumentation;
import play.mvc.ActionInvoker;
import play.mvc.Http.Request;
import play.mvc.Http.Response;

public abstract class ApplicationTest extends org.junit.Assert {

    @Before
    public void before() {
        Invocation.before();
    }

    @After
    public void after() {
        Invocation.after();
        Invocation._finally();
    }

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

    public static Response POST(String url, String contenttype, String body) {
        Request request = newRequest();
        String path = "";
        String queryString = "";
        if (url.contains("?")) {
            path = url.substring(0, url.indexOf("?"));
            queryString = url.substring(url.indexOf("?") + 1);
        } else {
            path = url;
        }
        request.method = "POST";
        request.contentType = contenttype;
        request.url = url;
        request.path = path;
        request.querystring = queryString;
        request.body = new ByteArrayInputStream(body.getBytes());
        //
        Response response = newResponse();
        //
        makeRequest(request, response);
        //
        return response;
    }

    public static void makeRequest(final Request request, final Response response) {
        ActionInvoker.invoke(request, response);
        try {
            response.out.flush();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
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
        assertStatus(200, response);
    }

    public static void assertIsNotFound(Response response) {
        assertStatus(404, response);
    }

    public static void assertStatus(int status, Response response) {
        assertEquals("Response status ", (Object) status, response.status);
    }

    public static void assertContentEquals(String content, Response response) {
        assertEquals(content, getContent(response));
    }

    public static void assertContentMatch(String pattern, Response response) {
        Pattern ptn = Pattern.compile(pattern);
        boolean ok = ptn.matcher(getContent(response)).find();
        assertTrue("Response content does not match '" + pattern + "' : " + getContent(response), ok);
    }

    public static void assertCharset(String charset, Response response) {
        int pos=response.contentType.indexOf("charset=")+8;
        String responseCharset = (  pos > 7 ) ? response.contentType.substring(pos).toLowerCase() : "";
        assertEquals("Response charset", charset.toLowerCase(), responseCharset);
    }

    public static void assertContentType(String contentType, Response response) {
        assertTrue("Response contentType unmatched : '" + contentType + "' !~ '" + response.contentType + "'",
                response.contentType.startsWith(contentType));
    }

    public static String getContent(Response response) {
        byte[] data = ((ByteArrayOutputStream) response.out).toByteArray();
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
        } catch (Exception e) {
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
