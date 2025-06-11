package play.server;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import java.lang.reflect.Method;
import java.nio.channels.ClosedChannelException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;

/**
 * Test cases for the JakartaServletWrapper class
 */
public class JakartaServletWrapperTest {

    private JakartaServletWrapper wrapper;
    private Method isClientDisconnectMethod;

    @Before
    public void setUp() throws Exception {
        // Create an instance of JakartaServletWrapper
        wrapper = new JakartaServletWrapper();

        // Get access to the private isClientDisconnect method using reflection
        isClientDisconnectMethod = JakartaServletWrapper.class.getDeclaredMethod("isClientDisconnect", Throwable.class);
        isClientDisconnectMethod.setAccessible(true);
    }

    /**
     * Helper method to invoke the private isClientDisconnect method
     */
    private boolean invokeIsClientDisconnect(Throwable t) throws Exception {
        return (boolean) isClientDisconnectMethod.invoke(wrapper, t);
    }

    /**
     * Test that ClosedChannelException is correctly identified as a client disconnect
     */
    @Test
    public void testClosedChannelException() throws Exception {
        assertTrue(invokeIsClientDisconnect(new ClosedChannelException()));
    }

    /**
     * Test that exceptions with class names containing specific patterns are identified as client disconnects
     */
    @Test
    public void testClassNamePatterns() throws Exception {
        // Test ClientAbortException
        Exception clientAbortException = new MockClientAbortException();
        assertTrue(invokeIsClientDisconnect(clientAbortException));

        // Test EofException
        Exception eofException = new MockEofException();
        assertTrue(invokeIsClientDisconnect(eofException));

        // Test ConnectionClosedException
        Exception connectionClosedException = new MockConnectionClosedException();
        assertTrue(invokeIsClientDisconnect(connectionClosedException));
    }

    /**
     * Test that exceptions with messages containing specific patterns are identified as client disconnects
     */
    @Test
    public void testMessagePatterns() throws Exception {
        // Test various message patterns
        assertTrue(invokeIsClientDisconnect(new Exception("Connection reset by peer")));
        assertTrue(invokeIsClientDisconnect(new Exception("Connection reset")));
        assertTrue(invokeIsClientDisconnect(new Exception("Software caused connection abort")));
        assertTrue(invokeIsClientDisconnect(new Exception("Premature EOF")));
        assertTrue(invokeIsClientDisconnect(new Exception("Stream closed")));
        assertTrue(invokeIsClientDisconnect(new Exception("Socket closed")));
        assertTrue(invokeIsClientDisconnect(new Exception("Reset by peer")));
        assertTrue(invokeIsClientDisconnect(new Exception("Connection aborted")));
    }

    /**
     * Test that nested exceptions (cause chain) are correctly identified
     */
    @Test
    public void testNestedExceptions() throws Exception {
        // Create a nested exception with a client disconnect cause
        Exception rootCause = new ClosedChannelException();
        Exception middleCause = new Exception("Some middle exception", rootCause);
        Exception topException = new Exception("Top level exception", middleCause);

        assertTrue(invokeIsClientDisconnect(topException));
    }

    /**
     * Test that circular references in exception chains are handled correctly
     */
    @Test
    public void testCircularExceptionReferences() throws Exception {
        // Create exceptions with circular references
        Exception exception1 = new Exception("Exception 1");
        Exception exception2 = new Exception("Exception 2", exception1);
        try {
            // This is a hack to create a circular reference
            java.lang.reflect.Field causeField = Throwable.class.getDeclaredField("cause");
            causeField.setAccessible(true);
            causeField.set(exception1, exception2);
        } catch (Exception e) {
            // If we can't create a circular reference, just use a normal chain
            exception1.initCause(exception2);
        }

        // The method should handle circular references without infinite loops
        assertFalse(invokeIsClientDisconnect(exception1));
    }

    /**
     * Test that exceptions that should not be identified as client disconnects return false
     */
    @Test
    public void testNonClientDisconnectExceptions() throws Exception {
        // Test with a regular exception
        assertFalse(invokeIsClientDisconnect(new Exception("Some random exception")));

        // Test with null
        assertFalse(invokeIsClientDisconnect(null));

        // Test with an exception with a null message
        Exception exceptionWithNullMessage = new Exception() {
            @Override
            public String getMessage() {
                return null;
            }
        };
        assertFalse(invokeIsClientDisconnect(exceptionWithNullMessage));
    }

    /**
     * Test the getCookies method
     */
    @Test
    public void testGetCookies() {
        // Mock HttpServletRequest
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);

        // Test case: No cookies
        Mockito.when(request.getCookies()).thenReturn(null);
        assertTrue(JakartaServletWrapper.getCookies(request).isEmpty());

        // Test case: With cookies
        jakarta.servlet.http.Cookie cookie1 = new jakarta.servlet.http.Cookie("name1", "value1");
        cookie1.setPath("/path1");
        cookie1.setDomain("domain1");
        cookie1.setSecure(true);
        cookie1.setMaxAge(100);

        jakarta.servlet.http.Cookie cookie2 = new jakarta.servlet.http.Cookie("name2", "value2");

        Mockito.when(request.getCookies()).thenReturn(new jakarta.servlet.http.Cookie[]{cookie1, cookie2});

        Map<String, play.mvc.Http.Cookie> cookies = JakartaServletWrapper.getCookies(request);
        assertEquals(2, cookies.size());

        // Verify first cookie
        play.mvc.Http.Cookie playCookie1 = cookies.get("name1");
        assertEquals("name1", playCookie1.name);
        assertEquals("value1", playCookie1.value);
        assertEquals("/path1", playCookie1.path);
        assertEquals("domain1", playCookie1.domain);
        assertTrue(playCookie1.secure);
        assertEquals(Integer.valueOf(100), playCookie1.maxAge);

        // Verify second cookie
        play.mvc.Http.Cookie playCookie2 = cookies.get("name2");
        assertEquals("name2", playCookie2.name);
        assertEquals("value2", playCookie2.value);
    }

    /**
     * Test the getHeaders method
     */
    @Test
    public void testGetHeaders() {
        // Mock HttpServletRequest
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);

        // Mock header names enumeration
        Enumeration<String> headerNames = Collections.enumeration(Arrays.asList("Content-Type", "User-Agent"));
        Mockito.when(request.getHeaderNames()).thenReturn(headerNames);

        // Mock header values
        Mockito.when(request.getHeaders("Content-Type")).thenReturn(
                Collections.enumeration(Arrays.asList("text/html", "application/xhtml+xml")));
        Mockito.when(request.getHeaders("User-Agent")).thenReturn(
                Collections.enumeration(Arrays.asList("Mozilla/5.0")));

        // Get headers
        Map<String, play.mvc.Http.Header> headers = JakartaServletWrapper.getHeaders(request);

        // Verify headers
        assertEquals(2, headers.size());

        // Verify Content-Type header
        play.mvc.Http.Header contentTypeHeader = headers.get("content-type");
        assertEquals("Content-Type", contentTypeHeader.name);
        assertEquals(2, contentTypeHeader.values.size());
        assertEquals("text/html", contentTypeHeader.values.get(0));
        assertEquals("application/xhtml+xml", contentTypeHeader.values.get(1));

        // Verify User-Agent header
        play.mvc.Http.Header userAgentHeader = headers.get("user-agent");
        assertEquals("User-Agent", userAgentHeader.name);
        assertEquals(1, userAgentHeader.values.size());
        assertEquals("Mozilla/5.0", userAgentHeader.values.get(0));
    }

    /**
     * Test the copyStream method with null inputs
     */
    @Test
    public void testCopyStreamWithNullInputs() throws Exception {
        // Get access to the private copyStream method using reflection
        Method copyStreamMethod = JakartaServletWrapper.class.getDeclaredMethod("copyStream",
                HttpServletResponse.class, InputStream.class);
        copyStreamMethod.setAccessible(true);

        // Test with null response
        copyStreamMethod.invoke(wrapper, null, new ByteArrayInputStream(new byte[0]));

        // Test with null input stream
        HttpServletResponse mockResponse = Mockito.mock(HttpServletResponse.class);
        copyStreamMethod.invoke(wrapper, mockResponse, null);

        // Test with both null
        copyStreamMethod.invoke(wrapper, null, null);

        // No exceptions should be thrown
    }

    /**
     * Test the copyStream method with valid inputs
     */
    @Test
    public void testCopyStreamWithValidInputs() throws Exception {
        // Get access to the private copyStream method using reflection
        Method copyStreamMethod = JakartaServletWrapper.class.getDeclaredMethod("copyStream",
                HttpServletResponse.class, InputStream.class);
        copyStreamMethod.setAccessible(true);

        // Create test data
        byte[] testData = "Test data for copyStream".getBytes();
        InputStream inputStream = new ByteArrayInputStream(testData);

        // Mock HttpServletResponse and ServletOutputStream
        HttpServletResponse mockResponse = Mockito.mock(HttpServletResponse.class);
        ServletOutputStream mockOutputStream = Mockito.mock(ServletOutputStream.class);
        Mockito.when(mockResponse.getOutputStream()).thenReturn(mockOutputStream);

        // Call the method
        copyStreamMethod.invoke(wrapper, mockResponse, inputStream);

        // Verify that getOutputStream was called
        Mockito.verify(mockResponse).getOutputStream();
    }

    /**
     * Test the copyStream method with client disconnect IOException
     */
    @Test
    public void testCopyStreamWithClientDisconnectIOException() throws Exception {
        // Get access to the private copyStream method using reflection
        Method copyStreamMethod = JakartaServletWrapper.class.getDeclaredMethod("copyStream",
                HttpServletResponse.class, InputStream.class);
        copyStreamMethod.setAccessible(true);

        // Create test data
        byte[] testData = "Test data for copyStream".getBytes();
        InputStream inputStream = new ByteArrayInputStream(testData);

        // Mock HttpServletResponse and ServletOutputStream that throws a client disconnect IOException
        HttpServletResponse mockResponse = Mockito.mock(HttpServletResponse.class);
        ServletOutputStream mockOutputStream = Mockito.mock(ServletOutputStream.class);
        Mockito.when(mockResponse.getOutputStream()).thenReturn(mockOutputStream);

        // Use a client disconnect message that will be recognized by isClientDisconnect
        IOException clientDisconnectException = new IOException("Connection reset by peer");
        Mockito.doThrow(clientDisconnectException).when(mockOutputStream).flush();

        // Call the method - should not throw exception because it's caught internally
        copyStreamMethod.invoke(wrapper, mockResponse, inputStream);
    }

    private void assertServletCookieAttributes(
            play.mvc.Http.Cookie playCookie,
            java.util.function.Consumer<jakarta.servlet.http.Cookie> extraAssertions
    ) throws Exception {
        play.mvc.Http.Response response = new play.mvc.Http.Response();
        response.cookies.put(playCookie.name, playCookie);
        response.status = 200;
        response.contentType = "text/plain";
        response.out = new java.io.ByteArrayOutputStream();
        response.encoding = "UTF-8";

        play.mvc.Http.Request request = new play.mvc.Http.Request();
        request.method = "GET";

        HttpServletRequest servletRequest = Mockito.mock(HttpServletRequest.class);
        HttpServletResponse servletResponse = Mockito.mock(HttpServletResponse.class);

        java.io.ByteArrayOutputStream servletOut = new java.io.ByteArrayOutputStream();
        ServletOutputStream servletOutputStream = new ServletOutputStream() {
            @Override
            public void write(int b) throws IOException {
                servletOut.write(b);
            }
            @Override
            public boolean isReady() { return true; }
            @Override
            public void setWriteListener(jakarta.servlet.WriteListener writeListener) { }
        };
        Mockito.when(servletResponse.getOutputStream()).thenReturn(servletOutputStream);

        final java.util.List<jakarta.servlet.http.Cookie> servletCookies = new java.util.ArrayList<>();
        Mockito.doAnswer(invocation -> {
            jakarta.servlet.http.Cookie c = invocation.getArgument(0);
            servletCookies.add(c);
            return null;
        }).when(servletResponse).addCookie(Mockito.any(jakarta.servlet.http.Cookie.class));

        play.mvc.Http.Response.current.set(response);
        play.mvc.Http.Request.current.set(request);

        new JakartaServletWrapper().copyResponse(request, response, servletRequest, servletResponse);

        boolean found = false;
        for (jakarta.servlet.http.Cookie c : servletCookies) {
            if (playCookie.name.equals(c.getName())) {
                found = true;
                extraAssertions.accept(c);
            }
        }
        assertTrue(playCookie.name + " cookie should be present in servlet response", found);
    }

    @Test
    public void testSameSiteCookieAttribute() throws Exception {
        play.mvc.Http.Cookie playCookie = new play.mvc.Http.Cookie();
        playCookie.name = "samesiteTest";
        playCookie.value = "testValue";
        playCookie.path = "/";
        playCookie.domain = null;
        playCookie.secure = false;
        playCookie.maxAge = 123;
        playCookie.sameSite = "Strict";

        assertServletCookieAttributes(playCookie, c -> {
            assertEquals("Strict", c.getAttribute("SameSite"));
        });
    }

    @Test
    public void testHttpOnlyCookieAttribute() throws Exception {
        play.mvc.Http.Cookie playCookieTrue = new play.mvc.Http.Cookie();
        playCookieTrue.name = "httpOnlyTestTrue";
        playCookieTrue.value = "testValueTrue";
        playCookieTrue.path = "/";
        playCookieTrue.domain = null;
        playCookieTrue.secure = false;
        playCookieTrue.maxAge = 123;
        playCookieTrue.httpOnly = true;

        play.mvc.Http.Cookie playCookieFalse = new play.mvc.Http.Cookie();
        playCookieFalse.name = "httpOnlyTestFalse";
        playCookieFalse.value = "testValueFalse";
        playCookieFalse.path = "/";
        playCookieFalse.domain = null;
        playCookieFalse.secure = false;
        playCookieFalse.maxAge = 456;
        playCookieFalse.httpOnly = false;

        assertServletCookieAttributes(playCookieTrue, c -> {
            try {
                assertTrue(c.isHttpOnly());
            } catch (NoSuchMethodError | UnsupportedOperationException ignored) {}
        });
        assertServletCookieAttributes(playCookieFalse, c -> {
            try {
                assertFalse(c.isHttpOnly());
            } catch (NoSuchMethodError | UnsupportedOperationException ignored) {}
        });
    }

    // Mock exception classes for testing class name patterns
    private static class MockClientAbortException extends Exception {
        // Class name contains "ClientAbortException"
    }

    private static class MockEofException extends Exception {
        // Class name contains "EofException"
    }

    private static class MockConnectionClosedException extends Exception {
        // Class name contains "ConnectionClosedException"
    }
}

