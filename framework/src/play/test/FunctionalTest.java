package play.test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.junit.Before;
import play.Invoker.InvocationContext;

import play.classloading.enhancers.ControllersEnhancer.ControllerInstrumentation;
import play.mvc.ActionInvoker;
import play.mvc.Http;
import play.mvc.Http.Request;
import play.mvc.Http.Response;

import com.ning.http.multipart.FilePart;
import com.ning.http.multipart.MultipartRequestEntity;
import com.ning.http.multipart.Part;
import com.ning.http.multipart.StringPart;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import play.Invoker;
import play.mvc.Controller;
import play.mvc.Router.ActionDefinition;

/**
 * Application tests support
 */
public abstract class FunctionalTest extends BaseTest {

    public static final String APPLICATION_X_WWW_FORM_URLENCODED = "application/x-www-form-urlencoded";
    public static final String MULTIPART_FORM_DATA = "multipart/form-data";

    private static Map<String, Http.Cookie> savedCookies; // cookies stored between calls

    @Before
    public void clearCookies(){
        savedCookies = null;
    }

    // Requests
    public static Response GET(Object url) {
        return GET(newRequest(), url);
    }

    /**
     * sends a GET request to the application under tests.
     * @param request
     * @param url relative url such as <em>"/products/1234"</em>
     * @return the response
     */
    public static Response GET(Request request, Object url) {
        String path = "";
        String queryString = "";
        String turl = url.toString();
        if (turl.contains("?")) {
            path = turl.substring(0, turl.indexOf("?"));
            queryString = turl.substring(turl.indexOf("?") + 1);
        } else {
            path = turl;
        }
        request.method = "GET";
        request.url = turl;
        request.path = path;
        request.querystring = queryString;
        request.body = new ByteArrayInputStream(new byte[0]);
        if (savedCookies != null) request.cookies = savedCookies;
        return makeRequest(request);
    }

    // convenience methods
    public static Response POST(Object url) {
        return POST(url, APPLICATION_X_WWW_FORM_URLENCODED, "");
    }

    public static Response POST(Request request, Object url) {
        return POST(request, url, APPLICATION_X_WWW_FORM_URLENCODED, "");
    }

    public static Response POST(Object url, String contenttype, String body) {
        return POST(newRequest(), url, contenttype, body);
    }

    public static Response POST(Request request, Object url, String contenttype, String body) {
        return POST(request, url, contenttype, new ByteArrayInputStream(body.getBytes()));
    }

    public static Response POST(Object url, String contenttype, InputStream body) {
        return POST(newRequest(), url, contenttype, body);
    }

    /**
     * Sends a POST request to the application under tests.
     * @param request
     * @param url relative url such as <em>"/products/1234"</em>
     * @param contenttype content-type of the request
     * @param body posted data
     * @return the response
     */
    public static Response POST(Request request, Object url, String contenttype, InputStream body) {
        String path = "";
        String queryString = "";
        String turl = url.toString();
        if (turl.contains("?")) {
            path = turl.substring(0, turl.indexOf("?"));
            queryString = turl.substring(turl.indexOf("?") + 1);
        } else {
            path = turl;
        }
        request.method = "POST";
        request.contentType = contenttype;
        request.url = turl;
        request.path = path;
        request.querystring = queryString;
        request.body = body;
        if (savedCookies != null) request.cookies = savedCookies;
        return makeRequest(request);
    }

    /**
     * Sends a POST request to the application under tests as a multipart form. Designed for file upload testing.
     * @param url relative url such as <em>"/products/1234"</em>
     * @param parameters map of parameters to be posted
     * @param files map containing files to be uploaded
     * @return the response
     */
    public static Response POST(Object url, Map<String, String> parameters, Map<String, File> files) {
        return POST(newRequest(), url, parameters, files);
    }

    public static Response POST(Request request, Object url, Map<String, String> parameters, Map<String, File> files) {
        List<Part> parts = new ArrayList<Part>();

        for (String key : parameters.keySet()) {
            parts.add(new StringPart(key, parameters.get(key)));
        }

        for (String key : files.keySet()) {
            Part filePart;
            try {
                filePart = new FilePart(key, files.get(key));
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
            parts.add(filePart);
        }

        MultipartRequestEntity requestEntity = new MultipartRequestEntity(parts.toArray(new Part[]{}), null); 
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            requestEntity.writeRequest(baos);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        InputStream body = new ByteArrayInputStream(baos.toByteArray());
        String contentType = requestEntity.getContentType();
        Http.Header header = new Http.Header();
        header.name = "content-type";
        header.values = Arrays.asList(new String[]{contentType});
        request.headers.put("content-type", header);
        return POST(request, url, MULTIPART_FORM_DATA, body);
    }

    public static Response PUT(Object url, String contenttype, String body) {
        return PUT(newRequest(), url, contenttype, body);
    }

    /**
     * Sends a PUT request to the application under tests.
     * @param request
     * @param url relative url such as <em>"/products/1234"</em>
     * @param contenttype content-type of the request
     * @param body data to send
     * @return the response
     */
    public static Response PUT(Request request, Object url, String contenttype, String body) {
        String path = "";
        String queryString = "";
        String turl = url.toString();
        if (turl.contains("?")) {
            path = turl.substring(0, turl.indexOf("?"));
            queryString = turl.substring(turl.indexOf("?") + 1);
        } else {
            path = turl;
        }
        request.method = "PUT";
        request.contentType = contenttype;
        request.url = turl;
        request.path = path;
        request.querystring = queryString;
        request.body = new ByteArrayInputStream(body.getBytes());
        return makeRequest(request);
    }

    public static Response DELETE(String url) {
        return DELETE(newRequest(), url);
    }

    /**
     * Sends a DELETE request to the application under tests.
     * @param request
     * @param url relative url eg. <em>"/products/1234"</em>
     * @return the response
     */
    public static Response DELETE(Request request, Object url) {
        String path = "";
        String queryString = "";
        String turl = url.toString();
        if (turl.contains("?")) {
            path = turl.substring(0, turl.indexOf("?"));
            queryString = turl.substring(turl.indexOf("?") + 1);
        } else {
            path = turl;
        }
        request.method = "DELETE";
        request.url = turl;
        request.path = path;
        request.querystring = queryString;
        if (savedCookies != null) request.cookies = savedCookies;
        request.body = new ByteArrayInputStream(new byte[0]);
        return makeRequest(request);
    }

    public static void makeRequest(final Request request, final Response response) {
        final Future invocationResult = TestEngine.functionalTestsExecutor.submit(new Invoker.Invocation() {

            @Override
            public void execute() throws Exception {                
                ActionInvoker.invoke(request, response);
            }

            @Override
            public InvocationContext getInvocationContext() {
                ActionInvoker.resolve(request, response);
                return new InvocationContext(request.invokedMethod.getAnnotations(), request.invokedMethod.getDeclaringClass().getAnnotations());
            }

        });
        try {
            invocationResult.get(30, TimeUnit.SECONDS);
            if (savedCookies == null) {
                savedCookies = response.cookies;
            } else {
                savedCookies.putAll(response.cookies);
            }
            response.out.flush();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public static Response makeRequest(final Request request) {
        Response response = newResponse();
        makeRequest(request, response);
        return response;
    }

    public static Response newResponse() {
        Response response = new Response();
        response.out = new ByteArrayOutputStream();
        return response;
    }

    public static Request newRequest() {
        Request request = new Request();
        request.domain = "localhost";
        request.port = 80;
        request.method = "GET";
        request.path = "/";
        request.querystring = "";
        return request;
    }

    // Assertions
    /**
     * Asserts a <em>2OO Success</em> response
     * @param response server response
     */
    public static void assertIsOk(Response response) {
        assertStatus(200, response);
    }

    /**
     * Asserts a <em>404 (not found)</em> response
     * @param response server response
     */
    public static void assertIsNotFound(Response response) {
        assertStatus(404, response);
    }

    /**
     * Asserts response status code
     * @param status expected HTTP response code
     * @param response server response
     */
    public static void assertStatus(int status, Response response) {
        assertEquals("Response status ", (Object) status, response.status);
    }

    /**
     * Exact equality assertion on response body
     * @param content expected body content
     * @param response server response
     */
    public static void assertContentEquals(String content, Response response) {
        assertEquals(content, getContent(response));
    }

    /**
     * Asserts response body matched a pattern or contains some text.
     * @param pattern a regular expression pattern or a regular text, ( which must be escaped using Pattern.quote)
     * @param response server response
     */
    public static void assertContentMatch(String pattern, Response response) {
        Pattern ptn = Pattern.compile(pattern);
        boolean ok = ptn.matcher(getContent(response)).find();
        assertTrue("Response content does not match '" + pattern + "'", ok);
    }

    /**
     * Verify response charset encoding, as returned by the server in the Content-Type header.
     * Be aware that if no charset is returned, assertion will fail.
     * @param charset expected charset encoding such as "utf-8" or "iso8859-1".
     * @param response server response
     */
    public static void assertCharset(String charset, Response response) {
        int pos = response.contentType.indexOf("charset=") + 8;
        String responseCharset = (pos > 7) ? response.contentType.substring(pos).toLowerCase() : "";
        assertEquals("Response charset", charset.toLowerCase(), responseCharset);
    }

    /**
     * Verify the response content-type
     * @param contentType expected content-type without any charset extension, such as "text/html"
     * @param response server response
     */
    public static void assertContentType(String contentType, Response response) {
        assertTrue("Response contentType unmatched : '" + contentType + "' !~ '" + response.contentType + "'",
                response.contentType.startsWith(contentType));
    }

    /**
     * Exact equality assertion on a response header value
     * @param headerName header to verify. case-insensitive
     * @param value expected header value
     * @param response server response
     */
    public static void assertHeaderEquals(String headerName, String value, Response response) {
        assertNotNull("Response header " + headerName + " missing", response.headers.get(headerName));
        assertEquals("Response header " + headerName + " mismatch", value, response.headers.get(headerName).value());
    }

    /**
     * obtains the response body as a string
     * @param response server response
     * @return the response body as an <em>utf-8 string</em>
     */
    public static String getContent(Response response) {
        byte[] data = response.out.toByteArray();
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

    protected static URL reverse() {
        ControllerInstrumentation.stopActionCall();
        ActionDefinition actionDefinition = new ActionDefinition();
        Controller._currentReverse.set(actionDefinition);
        return new URL(actionDefinition);
    }

    public static class URL {

        ActionDefinition actionDefinition;

        URL(ActionDefinition actionDefinition) {
            this.actionDefinition = actionDefinition;
        }

        @Override
        public String toString() {
            return actionDefinition.url;
        }

    }

}
