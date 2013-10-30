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
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.net.MalformedURLException;

import org.junit.Before;
import play.Invoker.InvocationContext;

import play.classloading.enhancers.ControllersEnhancer.ControllerInstrumentation;
import play.mvc.ActionInvoker;
import play.mvc.Http;
import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.mvc.Scope.RenderArgs;

import com.ning.http.multipart.FilePart;
import com.ning.http.multipart.MultipartRequestEntity;
import com.ning.http.multipart.Part;
import com.ning.http.multipart.StringPart;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeoutException;
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

    private static Map<String, Object> renderArgs = new HashMap<String, Object>();
    
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
     * @param url relative url such as <em>"/products/1234"</em>
     * @param followRedirect indicates if request have to follow redirection (status 302)
     * @return the response
     */
    public static Response GET(Object url, boolean followRedirect) {
        Response response = GET(url);
        if (Http.StatusCode.FOUND == response.status && followRedirect) {
            Http.Header redirectedTo = response.headers.get("Location");
            String location = redirectedTo.value();
            if(location.contains("http")){
            	java.net.URL redirectedUrl = null;
            	try {
            		redirectedUrl = new java.net.URL(redirectedTo.value());
            	} catch (MalformedURLException e) {
            		throw new RuntimeException(e);
            	}
            	response = GET(redirectedUrl.getPath());
            }
            else{
            	response = GET(location);
            }
        }
        return response;
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

    public static Response POST(Object url, Map<String, String> parameters) {
        return POST(newRequest(), url, parameters, new HashMap<String, File>());
    }

    public static Response POST(Request request, Object url, Map<String, String> parameters, Map<String, File> files) {
        List<Part> parts = new ArrayList<Part>();

        for (String key : parameters.keySet()) {
            final StringPart stringPart = new StringPart(key, parameters.get(key), request.encoding);
            parts.add(stringPart);
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
        if (savedCookies != null) request.cookies = savedCookies;
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
        final CountDownLatch actionCompleted = new CountDownLatch(1);
        TestEngine.functionalTestsExecutor.submit(new Invoker.Invocation() {

            @Override
            public void execute() throws Exception {
            	renderArgs.clear();
                ActionInvoker.invoke(request, response);
                
                if(RenderArgs.current().data != null) {
                	renderArgs.putAll(RenderArgs.current().data);
                }
            }

            @Override
            public void onSuccess() throws Exception {
                try {
                    super.onSuccess();
                } finally {
                    onActionCompleted();
                }
            }

            @Override
            public void onException(final Throwable e) {
                try {
                    super.onException(e);
                } finally {
                    onActionCompleted();
                }
            }

            private void onActionCompleted() {
                actionCompleted.countDown();
            }

            @Override
            public InvocationContext getInvocationContext() {
                ActionInvoker.resolve(request, response);
                return new InvocationContext(Http.invocationType,
                        request.invokedMethod.getAnnotations(),
                        request.invokedMethod.getDeclaringClass().getAnnotations());
            }

        });
        try {
            if (!actionCompleted.await(30, TimeUnit.SECONDS)) {
                throw new TimeoutException("Request did not complete in time");
            }
            if (savedCookies == null) {
                savedCookies = new HashMap<String, Http.Cookie>();
            }
            for(Map.Entry<String,Http.Cookie> e : response.cookies.entrySet()) {
                // If Max-Age is unset, browsers discard on exit; if
                // 0, they discard immediately.
                if(e.getValue().maxAge == null || e.getValue().maxAge > 0) {
                    savedCookies.put(e.getKey(), e.getValue());
                } else {
                    // cookies with maxAge zero still remove a previously existing cookie,
                    // like PLAY_FLASH.
                    savedCookies.remove(e.getKey());
                }
            }
            response.out.flush();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public static Response makeRequest(final Request request) {
        Response response = newResponse();
        makeRequest(request, response);

        if (response.status == 302) { // redirect
            // if Location-header is pressent, fix it to "look like" a functional-test-url
            Http.Header locationHeader = response.headers.get("Location");
            if (locationHeader != null) {
                String locationUrl = locationHeader.value();
                if (locationUrl.startsWith("http://localhost/")) {
                    locationHeader.values.clear();
                    locationHeader.values.add( locationUrl.substring(16));//skip 'http://localhost'
                }
            }
        }
        return response;
    }

    public static Response newResponse() {
        Response response = new Response();
        response.out = new ByteArrayOutputStream();
        return response;
    }

    public static Request newRequest() {
        Request request = Request.createRequest(
                null,
                "GET",
                "/",
                "",
                null,
                null,
                null,
                null,
                false,
                80,
                "localhost",
                false,
                null,
                null
        );
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
        String responseContentType = response.contentType;
        assertNotNull("Response contentType missing", responseContentType);
        assertTrue("Response contentType unmatched : '" + contentType + "' !~ '" + responseContentType + "'",
                responseContentType.startsWith(contentType));
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
            return new String(data, response.encoding);
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    public static Object renderArgs(String name) {
    	return renderArgs.get(name);
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
