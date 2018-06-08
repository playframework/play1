package play.server;

import org.junit.Before;
import org.junit.Test;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletInputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.BufferedReader;
import java.io.IOException;
import java.security.Principal;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ServletWrapperTest {
    private String browserEtag;
    private String browserLastModified;
    private long lastModified;

    @Before
    public void setUp() {
        browserEtag = "\"1299752290000-1192808478\"";
        browserLastModified = "Thu, 10 Mar 2011 10:18:10 GMT";
        lastModified = 1299752290000L;
    }

    @Test
    public void isNotModifiedHTTP11ClientTest() {
        assertFalse(ServletWrapper.isModified(browserEtag, lastModified, new HttpServletStub(createHeaderMap())));
    }

    @Test
    public void isNotModifiedHTTP10ClientTest() {
        HashMap<String, String> headers = createHeaderMap();
        headers.remove(ServletWrapper.IF_NONE_MATCH);
        assertFalse(ServletWrapper.isModified(browserEtag, 0, new HttpServletStub(headers)));
        assertFalse(ServletWrapper.isModified(browserEtag, lastModified, new HttpServletStub(headers)));
    }

    @Test
    public void isModifiedHTTP10ClientTest() {
        HashMap<String, String> headers = createHeaderMap();
        headers.remove(ServletWrapper.IF_NONE_MATCH);
        assertTrue(ServletWrapper.isModified(browserEtag, Long.MAX_VALUE, new HttpServletStub(headers)));
    }

    @Test
    public void browserHasNoCache() {
        assertTrue(ServletWrapper.isModified(browserEtag, lastModified, new HttpServletStub(new HashMap<>())));
    }

    private HashMap<String, String> createHeaderMap() {
        HashMap<String, String> headers = new HashMap<>();
        headers.put(ServletWrapper.IF_MODIFIED_SINCE, browserLastModified);
        headers.put(ServletWrapper.IF_NONE_MATCH, browserEtag);
        return headers;
    }

    private static class HttpServletStub implements HttpServletRequest {
        private final HashMap<String, String> headers;

        public HttpServletStub(HashMap<String, String> headers) {
            this.headers = headers;
        }

        @Override
        public String getHeader(String key) {
            return headers.get(key);
        }

        @Override
        public String getAuthType() {
            throw new RuntimeException("Method not implemented");
        }

        @Override
        public String getContextPath() {
            throw new RuntimeException("Method not implemented");
        }

        @Override
        public Cookie[] getCookies() {
            throw new RuntimeException("Method not implemented");
        }

        @Override
        public long getDateHeader(String arg0) {
            throw new RuntimeException("Method not implemented");
        }

        @Override
        public Enumeration getHeaderNames() {
            throw new RuntimeException("Method not implemented");
        }

        @Override
        public Enumeration getHeaders(final String arg0) {

            return new Enumeration<Object>() {
                String element = headers.get(arg0);

                @Override
                public boolean hasMoreElements() {
                    return element != null;
                }

                @Override
                public Object nextElement() {
                    Object current = element;
                    current = null;
                    return current;
                }
            };
        }

        @Override
        public int getIntHeader(String arg0) {
            throw new RuntimeException("Method not implemented");
        }

        @Override
        public String getMethod() {
            throw new RuntimeException("Method not implemented");
        }

        @Override
        public String getPathInfo() {
            throw new RuntimeException("Method not implemented");
        }

        @Override
        public String getPathTranslated() {
            throw new RuntimeException("Method not implemented");
        }

        @Override
        public String getQueryString() {
            throw new RuntimeException("Method not implemented");
        }

        @Override
        public String getRemoteUser() {
            throw new RuntimeException("Method not implemented");
        }

        @Override
        public String getRequestURI() {
            throw new RuntimeException("Method not implemented");
        }

        @Override
        public StringBuffer getRequestURL() {
            throw new RuntimeException("Method not implemented");
        }

        @Override
        public String getRequestedSessionId() {
            throw new RuntimeException("Method not implemented");
        }

        @Override
        public String getServletPath() {
            throw new RuntimeException("Method not implemented");
        }

        @Override
        public HttpSession getSession() {
            throw new RuntimeException("Method not implemented");
        }

        @Override
        public HttpSession getSession(boolean arg0) {
            throw new RuntimeException("Method not implemented");
        }

        @Override
        public Principal getUserPrincipal() {
            throw new RuntimeException("Method not implemented");
        }

        @Override
        public boolean isRequestedSessionIdFromCookie() {
            throw new RuntimeException("Method not implemented");
        }

        @Override
        public boolean isRequestedSessionIdFromURL() {
            throw new RuntimeException("Method not implemented");
        }

        @Override
        public boolean isRequestedSessionIdFromUrl() {
            throw new RuntimeException("Method not implemented");
        }

        @Override
        public boolean isRequestedSessionIdValid() {
            throw new RuntimeException("Method not implemented");
        }

        @Override
        public boolean isUserInRole(String arg0) {
            throw new RuntimeException("Method not implemented");
        }

        @Override
        public Object getAttribute(String arg0) {
            throw new RuntimeException("Method not implemented");
        }

        @Override
        public Enumeration getAttributeNames() {
            throw new RuntimeException("Method not implemented");
        }

        @Override
        public String getCharacterEncoding() {
            throw new RuntimeException("Method not implemented");
        }

        @Override
        public int getContentLength() {
            throw new RuntimeException("Method not implemented");
        }

        @Override
        public String getContentType() {
            throw new RuntimeException("Method not implemented");
        }

        @Override
        public ServletInputStream getInputStream() throws IOException {
            throw new RuntimeException("Method not implemented");
        }

        @Override
        public String getLocalAddr() {
            throw new RuntimeException("Method not implemented");
        }

        @Override
        public String getLocalName() {
            throw new RuntimeException("Method not implemented");
        }

        @Override
        public int getLocalPort() {
            throw new RuntimeException("Method not implemented");
        }

        @Override
        public Locale getLocale() {
            throw new RuntimeException("Method not implemented");
        }

        @Override
        public Enumeration getLocales() {
            throw new RuntimeException("Method not implemented");
        }

        @Override
        public String getParameter(String arg0) {
            throw new RuntimeException("Method not implemented");
        }

        @Override
        public Map getParameterMap() {
            throw new RuntimeException("Method not implemented");
        }

        @Override
        public Enumeration getParameterNames() {
            throw new RuntimeException("Method not implemented");
        }

        @Override
        public String[] getParameterValues(String arg0) {
            throw new RuntimeException("Method not implemented");
        }

        @Override
        public String getProtocol() {
            throw new RuntimeException("Method not implemented");
        }

        @Override
        public BufferedReader getReader() throws IOException {
            throw new RuntimeException("Method not implemented");
        }

        @Override
        public String getRealPath(String arg0) {
            throw new RuntimeException("Method not implemented");
        }

        @Override
        public String getRemoteAddr() {
            throw new RuntimeException("Method not implemented");
        }

        @Override
        public String getRemoteHost() {
            throw new RuntimeException("Method not implemented");
        }

        @Override
        public int getRemotePort() {
            throw new RuntimeException("Method not implemented");
        }

        @Override
        public RequestDispatcher getRequestDispatcher(String arg0) {
            throw new RuntimeException("Method not implemented");
        }

        @Override
        public String getScheme() {
            throw new RuntimeException("Method not implemented");
        }

        @Override
        public String getServerName() {
            throw new RuntimeException("Method not implemented");
        }

        @Override
        public int getServerPort() {
            throw new RuntimeException("Method not implemented");
        }

        @Override
        public boolean isSecure() {
            throw new RuntimeException("Method not implemented");
        }

        @Override
        public void removeAttribute(String arg0) {
            throw new RuntimeException("Method not implemented");
        }

        @Override
        public void setAttribute(String arg0, Object arg1) {
            throw new RuntimeException("Method not implemented");
        }

        @Override
        public void setCharacterEncoding(String arg0) {
            throw new RuntimeException("Method not implemented");
        }

    }

}
