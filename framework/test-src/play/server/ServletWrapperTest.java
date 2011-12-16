package play.server;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletInputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.junit.Before;
import org.junit.Test;

public class ServletWrapperTest {
	private String browserEtag;
	private String browserLastModified;

	@Before
	public void setUp() {
		browserEtag = "\"1299752290000-1192808478\"";
		browserLastModified = "Thu, 10 Mar 2011 10:18:10 GMT";
	}

	@Test
	public void isNotModifiedHTTP11ClientTest() {
		assertFalse(ServletWrapper.isModified(browserEtag, 0,
				new HttpServletStub(createHeaderMap())));
	}

	@Test
	public void isNotModifiedHTTP10ClientTest() {
		HashMap<String, String> headers = createHeaderMap();
		headers.remove(ServletWrapper.IF_NONE_MATCH);
		assertFalse(ServletWrapper.isModified(browserEtag, 0,
				new HttpServletStub(headers)));
	}

	@Test
	public void isModifiedHTTP10ClientTest() {
		HashMap<String, String> headers = createHeaderMap();
		headers.remove(ServletWrapper.IF_NONE_MATCH);
		assertTrue(ServletWrapper.isModified(browserEtag, Long.MAX_VALUE,
				new HttpServletStub(headers)));
	}

	@Test
	public void browserHasNoCache() {
		assertTrue(ServletWrapper.isModified(browserEtag, 0,
				new HttpServletStub(new HashMap<String, String>())));
	}

	private HashMap<String, String> createHeaderMap() {
		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(ServletWrapper.IF_MODIFIED_SINCE, browserLastModified);
		headers.put(ServletWrapper.IF_NONE_MATCH, browserEtag);
		return headers;
	}

	private static class HttpServletStub implements HttpServletRequest {
		private final HashMap<String, String> headers;

		public HttpServletStub(HashMap<String, String> headers) {
			this.headers = headers;
		}

		public String getHeader(String key) {
			return headers.get(key);
		}

		public String getAuthType() {
			throw new RuntimeException("Method not implemented");
		}

		public String getContextPath() {
			throw new RuntimeException("Method not implemented");
		}

		public Cookie[] getCookies() {
			throw new RuntimeException("Method not implemented");
		}

		public long getDateHeader(String arg0) {
			throw new RuntimeException("Method not implemented");
		}

		public Enumeration getHeaderNames() {
			throw new RuntimeException("Method not implemented");
		}

		public Enumeration getHeaders(final String arg0) {
			
			return new Enumeration<Object>() {
				String element = headers.get(arg0);

				public boolean hasMoreElements() {
					return element != null;
				}

				public Object nextElement() {
					Object current = element;
					current = null;
					return current;
				}
			}; 
		}

		public int getIntHeader(String arg0) {
			throw new RuntimeException("Method not implemented");
		}

		public String getMethod() {
			throw new RuntimeException("Method not implemented");
		}

		public String getPathInfo() {
			throw new RuntimeException("Method not implemented");
		}

		public String getPathTranslated() {
			throw new RuntimeException("Method not implemented");
		}

		public String getQueryString() {
			throw new RuntimeException("Method not implemented");
		}

		public String getRemoteUser() {
			throw new RuntimeException("Method not implemented");
		}

		public String getRequestURI() {
			throw new RuntimeException("Method not implemented");
		}

		public StringBuffer getRequestURL() {
			throw new RuntimeException("Method not implemented");
		}

		public String getRequestedSessionId() {
			throw new RuntimeException("Method not implemented");
		}

		public String getServletPath() {
			throw new RuntimeException("Method not implemented");
		}

		public HttpSession getSession() {
			throw new RuntimeException("Method not implemented");
		}

		public HttpSession getSession(boolean arg0) {
			throw new RuntimeException("Method not implemented");
		}

		public Principal getUserPrincipal() {
			throw new RuntimeException("Method not implemented");
		}

		public boolean isRequestedSessionIdFromCookie() {
			throw new RuntimeException("Method not implemented");
		}

		public boolean isRequestedSessionIdFromURL() {
			throw new RuntimeException("Method not implemented");
		}

		public boolean isRequestedSessionIdFromUrl() {
			throw new RuntimeException("Method not implemented");
		}

		public boolean isRequestedSessionIdValid() {
			throw new RuntimeException("Method not implemented");
		}

		public boolean isUserInRole(String arg0) {
			throw new RuntimeException("Method not implemented");
		}

		public Object getAttribute(String arg0) {
			throw new RuntimeException("Method not implemented");
		}

		public Enumeration getAttributeNames() {
			throw new RuntimeException("Method not implemented");
		}

		public String getCharacterEncoding() {
			throw new RuntimeException("Method not implemented");
		}

		public int getContentLength() {
			throw new RuntimeException("Method not implemented");
		}

		public String getContentType() {
			throw new RuntimeException("Method not implemented");
		}

		public ServletInputStream getInputStream() throws IOException {
			throw new RuntimeException("Method not implemented");
		}

		public String getLocalAddr() {
			throw new RuntimeException("Method not implemented");
		}

		public String getLocalName() {
			throw new RuntimeException("Method not implemented");
		}

		public int getLocalPort() {
			throw new RuntimeException("Method not implemented");
		}

		public Locale getLocale() {
			throw new RuntimeException("Method not implemented");
		}

		public Enumeration getLocales() {
			throw new RuntimeException("Method not implemented");
		}

		public String getParameter(String arg0) {
			throw new RuntimeException("Method not implemented");
		}

		public Map getParameterMap() {
			throw new RuntimeException("Method not implemented");
		}

		public Enumeration getParameterNames() {
			throw new RuntimeException("Method not implemented");
		}

		public String[] getParameterValues(String arg0) {
			throw new RuntimeException("Method not implemented");
		}

		public String getProtocol() {
			throw new RuntimeException("Method not implemented");
		}

		public BufferedReader getReader() throws IOException {
			throw new RuntimeException("Method not implemented");
		}

		public String getRealPath(String arg0) {
			throw new RuntimeException("Method not implemented");
		}

		public String getRemoteAddr() {
			throw new RuntimeException("Method not implemented");
		}

		public String getRemoteHost() {
			throw new RuntimeException("Method not implemented");
		}

		public int getRemotePort() {
			throw new RuntimeException("Method not implemented");
		}

		public RequestDispatcher getRequestDispatcher(String arg0) {
			throw new RuntimeException("Method not implemented");
		}

		public String getScheme() {
			throw new RuntimeException("Method not implemented");
		}

		public String getServerName() {
			throw new RuntimeException("Method not implemented");
		}

		public int getServerPort() {
			throw new RuntimeException("Method not implemented");
		}

		public boolean isSecure() {
			throw new RuntimeException("Method not implemented");
		}

		public void removeAttribute(String arg0) {
			throw new RuntimeException("Method not implemented");
		}

		public void setAttribute(String arg0, Object arg1) {
			throw new RuntimeException("Method not implemented");
		}

		public void setCharacterEncoding(String arg0)
				throws UnsupportedEncodingException {
			throw new RuntimeException("Method not implemented");
		}

	}

}
