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

  /**
   * sends a GET request to the application under tests.
   * @param url relative url such as <em>"/products/1234"</em>
   * @return the response 
   */
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

  /**
   * Sends a POST request to the application under tests.
   * @param url relative url such as <em>"/products/1234"</em>
   * @param contenttype content-type of the request
   * @param body posted data
   * @return the response
   */
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

  /**
   * Sends a PUT request to the application under tests.
   * @param url relative url such as <em>"/products/1234"</em>
   * @param contenttype content-type of the request
   * @param body data to send
   * @return the response
   */
  public static Response PUT(String url, String contenttype, String body) {
    Request request = newRequest();
    String path = "";
    String queryString = "";
    if (url.contains("?")) {
      path = url.substring(0, url.indexOf("?"));
      queryString = url.substring(url.indexOf("?") + 1);
    } else {
      path = url;
    }
    request.method = "PUT";
    request.contentType = contenttype;
    request.url = url;
    request.path = path;
    request.querystring = queryString;
    request.body = new ByteArrayInputStream(body.getBytes());
    Response response = newResponse();
    makeRequest(request, response);
    return response;
  }

  /**
   * Sends a DELETE request to the application under tests.
   * @param url relative url eg. <em>"/products/1234"</em>
   * @return the response
   */
  public static Response DELETE(String url) {
    Request request = newRequest();
    String path = "";
    String queryString = "";
    if (url.contains("?")) {
      path = url.substring(0, url.indexOf("?"));
      queryString = url.substring(url.indexOf("?") + 1);
    } else {
      path = url;
    }
    request.method = "DELETE";
    request.url = url;
    request.path = path;
    request.querystring = queryString;
    request.body = new ByteArrayInputStream(new byte[0]);
    Response response = newResponse();
    makeRequest(request, response);
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
    assertTrue("Response content does not match '" + pattern + "' : " + getContent(response), ok);
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
    assertNotNull("Response header " + headerName + " missing", response.headers.get(headerName.toLowerCase()));
    assertEquals("Response header " + headerName + " mismatch", value, response.headers.get(headerName.toLowerCase()));
  }

  /* TODO : check json syntax
  public static void assertValidJSON( Response response) {
  try {
  JSON.verify( getContent(response));
  } catch( JSONException jsEx ) {
  fail("invalid json response "+ jsEx.getMessage() );
  }
  }
   */

  /**
   * obtains the response body as a string
   * @param response server response
   * @return the response body as an <em>utf-8 string</em>
   */
  public static String getContent(Response response) {
    byte[] data = ((ByteArrayOutputStream) response.out).toByteArray();
    try {
      return new String(data, "utf-8");
    } catch (UnsupportedEncodingException ex) {
      throw new RuntimeException(ex);
    }
  }

  // Utils
  
  /**
   * pause execution
   * @param seconds seconds to sleep
   */
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
