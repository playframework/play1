package play.server;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import play.Invoker;
import play.Invoker.InvocationContext;
import play.Logger;
import play.Play;
import play.data.binding.CachedBoundActionMethodArgs;
import play.data.validation.Validation;
import play.exceptions.PlayException;
import play.exceptions.UnexpectedException;
import play.libs.MimeTypes;
import play.mvc.ActionInvoker;
import play.mvc.Http;
import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.mvc.Router;
import play.mvc.Scope;
import play.mvc.results.NotFound;
import play.mvc.results.RenderStatic;
import play.templates.TemplateLoader;
import play.utils.HTTP;
import play.utils.Utils;
import play.vfs.VirtualFile;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.apache.commons.io.IOUtils.closeQuietly;

/**
 * Servlet implementation.
 * Thanks to Lee Breisacher.
 * Version of ServletWrapper using Jakarta Servlet API.
 */
public class JakartaServletWrapper extends HttpServlet implements ServletContextListener {

    public static final String IF_MODIFIED_SINCE = "If-Modified-Since";
    public static final String IF_NONE_MATCH = "If-None-Match";

    /**
     * Constant for accessing the underlying HttpServletRequest from Play's Request
     * in a Servlet based deployment.
     * <p>Sample usage:</p>
     * <p> {@code HttpServletRequest req = Request.current().args.get(ServletWrapper.SERVLET_REQ);}</p>
     */
    public static final String SERVLET_REQ = "__SERVLET_REQ";
    /**
     * Constant for accessing the underlying HttpServletResponse from Play's Request
     * in a Servlet based deployment.
     * <p>Sample usage:</p>
     * <p> {@code HttpServletResponse res = Request.current().args.get(ServletWrapper.SERVLET_RES);}</p>
     */
    public static final String SERVLET_RES = "__SERVLET_RES";

    private static boolean routerInitializedWithContext = false;
    
    
    private static final String X_HTTP_METHOD_OVERRIDE = "X-HTTP-Method-Override";

    private static final List<String> CLIENT_DISCONNECT_MESSAGES = List.of(
            "broken pipe",
            "connection reset by peer",
            "connection reset",
            "software caused connection abort",
            "premature eof",
            "stream closed",
            "socket closed",
            "reset by peer",
            "connection aborted"
    );

    /**
     * Define allowed methods that will be handled when defined in X-HTTP-Method-Override
     * You can define allowed method in
     * application.conf: <code>http.allowed.method.override=POST,PUT</code>
     */
    private static final Set<String> allowedHttpMethodOverride;
    static {
        allowedHttpMethodOverride = Stream.of(Play.configuration.getProperty("http.allowed.method.override", "").split(",")).collect(Collectors.toSet());
    }

    @Override
    public void contextInitialized(ServletContextEvent e) {
        Play.standalonePlayServer = false;
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        String appDir = System.getProperty("application.path", e.getServletContext().getRealPath("/WEB-INF/application"));
        File root = new File(appDir);
        String playId = System.getProperty("play.id", e.getServletContext().getInitParameter("play.id"));
        if (StringUtils.isEmpty(playId)) {
            throw new UnexpectedException("Please define a play.id parameter in your web.xml file. Without that parameter, play! cannot start your application. Please add a context-param into the WEB-INF/web.xml file.");
        }
        // This is really important as we know this parameter already (we are running in a servlet container)
        Play.frameworkPath = root.getParentFile();
        Play.usePrecompiled = true;
        Play.init(root, playId);
        Play.Mode mode = Play.Mode.valueOf(Play.configuration.getProperty("application.mode", "DEV").toUpperCase());
        if (mode.isDev()) {
            Logger.info("Forcing PROD mode because deploying as a war file.");
        }

        // Servlet 2.4 does not allow you to get the context path from the servletcontext...
        if (isGreaterThan(e.getServletContext(), 2, 4)) {
            loadRouter(e.getServletContext().getContextPath());
        }

        Thread.currentThread().setContextClassLoader(oldClassLoader);
    }

    @Override
    public void contextDestroyed(ServletContextEvent e) {
        Play.stop();
    }

    @Override
    public void destroy() {
        Logger.trace("ServletWrapper>destroy");
        Play.stop();
    }

    private static synchronized void loadRouter(String contextPath) {
        // Reload the rules, but this time with the context. Not really efficient through...
        // Servlet 2.4 does not allow you to get the context path from the servletcontext...
        if (routerInitializedWithContext) return;
        Play.ctxPath = contextPath;
        Router.load(contextPath);
        routerInitializedWithContext = true;
    }

    public static boolean isGreaterThan(ServletContext context, int majorVersion, int minorVersion) {
        int contextMajorVersion = context.getMajorVersion();
        int contextMinorVersion = context.getMinorVersion();
        return (contextMajorVersion > majorVersion) || (contextMajorVersion == majorVersion && contextMinorVersion > minorVersion);
    }

    @Override
    protected void service(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws ServletException, IOException {

        if (!routerInitializedWithContext) {
            loadRouter(httpServletRequest.getContextPath());
        }

        if (Logger.isTraceEnabled()) {
            Logger.trace("ServletWrapper>service " + httpServletRequest.getRequestURI());
        }

        Request request = null;
        try {
            Response response = new Response();
            response.out = new ByteArrayOutputStream();
            Response.current.set(response);
            request = parseRequest(httpServletRequest);

            if (Logger.isTraceEnabled()) {
                Logger.trace("ServletWrapper>service, request: " + request);
            }

            boolean raw = Play.pluginCollection.rawInvocation(request, response);
            if (raw) {
                copyResponse(Request.current(), Response.current(), httpServletRequest, httpServletResponse);
            } else {
                Invoker.invokeInThread(new ServletInvocation(request, response, httpServletRequest, httpServletResponse));
            }
        } catch (NotFound e) {
            if (Logger.isTraceEnabled()) {
                Logger.trace("ServletWrapper>service, NotFound: " + e);
            }
            serve404(httpServletRequest, httpServletResponse, e);
            return;
        } catch (RenderStatic e) {
            if (Logger.isTraceEnabled()) {
                Logger.trace("ServletWrapper>service, RenderStatic: " + e);
            }
            serveStatic(httpServletResponse, httpServletRequest, e);
            return;
        } catch(URISyntaxException e) {
            serve404(httpServletRequest, httpServletResponse, new NotFound(e.toString()));
            return;
        } catch (Throwable e) {
            throw new ServletException(e);
        } finally {
            Request.current.remove();
            Response.current.remove();
            Scope.Session.current.remove();
            Scope.Params.current.remove();
            Scope.Flash.current.remove();
            Scope.RenderArgs.current.remove();
            Scope.RouteArgs.current.remove();
            CachedBoundActionMethodArgs.clear();
        }
    }

    public void serveStatic(HttpServletResponse servletResponse, HttpServletRequest servletRequest, RenderStatic renderStatic) throws IOException {

        VirtualFile file = Play.getVirtualFile(renderStatic.file);
        if (file == null || file.isDirectory() || !file.exists()) {
            serve404(servletRequest, servletResponse, new NotFound("The file " + renderStatic.file + " does not exist"));
        } else {
            servletResponse.setContentType(MimeTypes.getContentType(file.getName()));
            boolean raw = Play.pluginCollection.serveStatic(file, Request.current(), Response.current());
            if (raw) {
                copyResponse(Request.current(), Response.current(), servletRequest, servletResponse);
            } else {
                if (Play.mode == Play.Mode.DEV) {
                    servletResponse.setHeader("Cache-Control", "no-cache");
                    servletResponse.setHeader("Content-Length", String.valueOf(file.length()));
                    if (!servletRequest.getMethod().equals("HEAD")) {
                        copyStream(servletResponse, file.inputstream());
                    } else {
                        copyStream(servletResponse, new ByteArrayInputStream(new byte[0]));
                    }
                } else {
                    long last = file.lastModified();
                    String etag = "\"" + last + "-" + file.hashCode() + "\"";
                    String lastDate = Utils.getHttpDateFormatter().format(new Date(last));
                    if (!isModified(etag, last, servletRequest)) {
                        servletResponse.setHeader("Etag", etag);
                        servletResponse.setStatus(304);
                    } else {
                        servletResponse.setHeader("Last-Modified", lastDate);
                        servletResponse.setHeader("Cache-Control", "max-age=" + Play.configuration.getProperty("http.cacheControl", "3600"));
                        servletResponse.setHeader("Etag", etag);
                        copyStream(servletResponse, file.inputstream());
                    }
                }
            }
        }
    }

    public static boolean isModified(String etag, long last, HttpServletRequest request) {
        String browserEtag = request.getHeader(IF_NONE_MATCH);
        String dateString = request.getHeader(IF_MODIFIED_SINCE);
        return HTTP.isModified(etag, last, browserEtag, dateString);
    }

    public static Request parseRequest(HttpServletRequest httpServletRequest) throws Exception {

        URI uri = new URI(httpServletRequest.getRequestURI());
        String method = httpServletRequest.getMethod().intern();
        String path = uri.getPath();
        String querystring = httpServletRequest.getQueryString() == null ? "" : httpServletRequest.getQueryString();

        if (Logger.isTraceEnabled()) {
            Logger.trace("httpServletRequest.getContextPath(): " + httpServletRequest.getContextPath());
            Logger.trace("request.path: " + path + ", request.querystring: " + querystring);
        }

        String contentType = null;
        if (httpServletRequest.getHeader("Content-Type") != null) {
            contentType = httpServletRequest.getHeader("Content-Type").split(";")[0].trim().toLowerCase().intern();
        } else {
            contentType = "text/html".intern();
        }

        if (httpServletRequest.getHeader(X_HTTP_METHOD_OVERRIDE) != null && allowedHttpMethodOverride
                .contains(httpServletRequest.getHeader(X_HTTP_METHOD_OVERRIDE).intern())) {
            method = httpServletRequest.getHeader(X_HTTP_METHOD_OVERRIDE).intern();
        }

        InputStream body = httpServletRequest.getInputStream();
        boolean secure = httpServletRequest.isSecure();

        String url = uri.toString() + (httpServletRequest.getQueryString() == null ? "" : "?" + httpServletRequest.getQueryString());
        String host = httpServletRequest.getHeader("host");
        int port = 0;
        String domain = null;
        if (host.contains(":")) {
            port = Integer.parseInt(host.split(":")[1]);
            domain = host.split(":")[0];
        } else {
            port = 80;
            domain = host;
        }

        String remoteAddress = httpServletRequest.getRemoteAddr();

        boolean isLoopback = host.matches("^127\\.0\\.0\\.1:?[0-9]*$");


        Request request = Request.createRequest(
                remoteAddress,
                method,
                path,
                querystring,
                contentType,
                body,
                url,
                host,
                isLoopback,
                port,
                domain,
                secure,
                getHeaders(httpServletRequest),
                getCookies(httpServletRequest));


        Request.current.set(request);
        Router.routeOnlyStatic(request);

        return request;
    }

    protected static Map<String, Http.Header> getHeaders(HttpServletRequest httpServletRequest) {
        Map<String, Http.Header> headers = new HashMap<>(16);

        Enumeration<String> headersNames = httpServletRequest.getHeaderNames();
        while (headersNames.hasMoreElements()) {
            Http.Header hd = new Http.Header();
            hd.name = headersNames.nextElement();
            hd.values = new ArrayList<>();
            Enumeration<String> enumValues = httpServletRequest.getHeaders(hd.name);
            while (enumValues.hasMoreElements()) {
                String value = enumValues.nextElement();
                hd.values.add(value);
            }
            headers.put(hd.name.toLowerCase(), hd);
        }

        return headers;
    }

    protected static Map<String, Http.Cookie> getCookies(HttpServletRequest httpServletRequest) {
        Map<String, Http.Cookie> cookies = new HashMap<>(16);
        Cookie[] cookiesViaServlet = httpServletRequest.getCookies();
        if (cookiesViaServlet != null) {
            for (Cookie cookie : cookiesViaServlet) {
                Http.Cookie playCookie = new Http.Cookie();
                playCookie.name = cookie.getName();
                playCookie.path = cookie.getPath();
                playCookie.domain = cookie.getDomain();
                playCookie.secure = cookie.getSecure();
                playCookie.value = cookie.getValue();
                playCookie.maxAge = cookie.getMaxAge();
                cookies.put(playCookie.name, playCookie);
            }
        }

        return cookies;
    }

    // Add a static helper for writing bytes to the response stream with client disconnect handling
    private static void writeToResponseStream(HttpServletResponse servletResponse, byte[] bytes) throws IOException {
        OutputStream os = null;
        try {
            os = servletResponse.getOutputStream();
            os.write(bytes);
            os.flush();
        } catch (IOException e) {
            if (isClientDisconnect(e)) {
                Logger.debug(e, "Client disconnected");
            } else {
                throw e;
            }
        }
    }

    // Check if the exception is related to a client disconnect
    private static boolean isClientDisconnect(Throwable t) {
        Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        while (t != null && seen.add(t)) {
            if (t instanceof java.nio.channels.ClosedChannelException) {
                return true;
            }
            String className = t.getClass().getName();
            if (className.contains("ClientAbortException") ||
                className.contains("EofException") ||
                className.contains("ConnectionClosedException")) {
                return true;
            }
            String msg = t.getMessage();
            if (msg != null) {
                String lower = msg.toLowerCase();
                for (String pattern : CLIENT_DISCONNECT_MESSAGES) {
                    if (lower.contains(pattern)) {
                        return true;
                    }
                }
            }
            t = t.getCause();
        }
        return false;
    }

    public void serve404(HttpServletRequest servletRequest, HttpServletResponse servletResponse, NotFound e) {
        if (Logger.isTraceEnabled()) {
            Logger.trace("404 -> %s %s (%s)", servletRequest.getMethod(), servletRequest.getRequestURI(), e.getMessage());
        }
        servletResponse.setStatus(404);
        servletResponse.setContentType("text/html");
        Map<String, Object> binding = new HashMap<>();
        binding.put("result", e);
        binding.put("session", Scope.Session.current());
        binding.put("request", Request.current());
        binding.put("flash", Scope.Flash.current());
        binding.put("params", Scope.Params.current());
        binding.put("play", new Play());
        try {
            binding.put("errors", Validation.errors());
        } catch (Exception ex) {
            Logger.error(ex, "Failed to bind errors");
        }
        String format = Request.current().format;
        servletResponse.setStatus(404);
        // Do we have an ajax request? If we have then we want to display some text even if it is html that is requested
        if ("XMLHttpRequest".equals(servletRequest.getHeader("X-Requested-With")) && (format == null || format.equals("html"))) {
            format = "txt";
        }
        if (format == null) {
            format = "txt";
        }
        servletResponse.setContentType(MimeTypes.getContentType("404." + format, "text/plain"));
        String errorHtml = TemplateLoader.load("errors/404." + format).render(binding);
        try {
            writeToResponseStream(servletResponse, errorHtml.getBytes(Response.current().encoding));
        } catch (Exception fex) {
            Logger.error(fex, "(encoding ?)");
        }
    }

    public void serve500(Exception e, HttpServletRequest request, HttpServletResponse response) {
        try {
            Map<String, Object> binding = new HashMap<>();
            if (!(e instanceof PlayException)) {
                e = new UnexpectedException(e);
            }
            // Flush all cookies (do not check sendOnError, match PlayHandler)
            try {
                Map<String, Http.Cookie> cookies = Response.current().cookies;
                for (Http.Cookie cookie : cookies.values()) {
                    Cookie c = new Cookie(cookie.name, cookie.value);
                    c.setSecure(cookie.secure);
                    c.setPath(cookie.path);
                    if (cookie.domain != null) {
                        c.setDomain(cookie.domain);
                    }
                    if (cookie.maxAge != null) {
                        c.setMaxAge(cookie.maxAge);
                    }
                    if (cookie.sameSite != null && !cookie.sameSite.isEmpty()) {
                        c.setAttribute("SameSite", cookie.sameSite);
                    }
                    // Set httpOnly if available (Servlet 3.0+)
                    try {
                        c.setHttpOnly(cookie.httpOnly);
                    } catch (NoSuchMethodError | UnsupportedOperationException ignored) {
                        // Ignore if not supported by the servlet container
                    }
                    response.addCookie(c);
                }
            } catch (Exception exx) {
                Logger.error(exx, "Failed to flush cookies");
            }
            binding.put("exception", e);
            binding.put("session", Scope.Session.current());
            binding.put("request", Request.current());
            binding.put("flash", Scope.Flash.current());
            binding.put("params", Scope.Params.current());
            binding.put("play", new Play());
            try {
                binding.put("errors", Validation.errors());
            } catch (Exception ex) {
                Logger.error(ex, "Failed to bind errors");
            }
            response.setStatus(500);
            String format = "html";
            if (Request.current() != null) {
                format = Request.current().format;
            }
            // Do we have an ajax request? If we have then we want to display some text even if it is html that is requested
            if ("XMLHttpRequest".equals(request.getHeader("X-Requested-With")) && (format == null || format.equals("html"))) {
                format = "txt";
            }
            if (format == null) {
                format = "txt";
            }
            response.setContentType(MimeTypes.getContentType("500." + format, "text/plain"));
            try {
                String errorHtml = TemplateLoader.load("errors/500." + format).render(binding);
                writeToResponseStream(response, errorHtml.getBytes(Response.current().encoding));
                Logger.error(e, "Internal Server Error (500)");
            } catch (Throwable ex) {
                Logger.error(e, "Internal Server Error (500)");
                Logger.error(ex, "Error during the 500 response generation");
                throw ex;
            }
        } catch (Throwable exxx) {
            if (exxx instanceof RuntimeException) {
                throw (RuntimeException) exxx;
            }
            throw new RuntimeException(exxx);
        }
    }

    public void copyResponse(Request request, Response response, HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws IOException {
        String encoding = Response.current().encoding;
        if (response.contentType != null) {
            servletResponse.setHeader("Content-Type", response.contentType + (response.contentType.startsWith("text/") ? "; charset="+encoding : ""));
        } else {
            servletResponse.setHeader("Content-Type", "text/plain;charset="+encoding);
        }

        servletResponse.setStatus(response.status);
        if (!response.headers.containsKey("cache-control")) {
            servletResponse.setHeader("Cache-Control", "no-cache");
        }
        Map<String, Http.Header> headers = response.headers;
        for (Map.Entry<String, Http.Header> entry : headers.entrySet()) {
            Http.Header hd = entry.getValue();
            String key = entry.getKey();
            for (String value : hd.values) {
                servletResponse.setHeader(key, value);
            }
        }

        Map<String, Http.Cookie> cookies = response.cookies;
        for (Http.Cookie cookie : cookies.values()) {
            Cookie c = new Cookie(cookie.name, cookie.value);
            c.setSecure(cookie.secure);
            c.setPath(cookie.path);
            if (cookie.domain != null) {
                c.setDomain(cookie.domain);
            }
            if (cookie.maxAge != null) {
                c.setMaxAge(cookie.maxAge);
            }
            if (cookie.sameSite != null && !cookie.sameSite.isEmpty()) {
                c.setAttribute("SameSite", cookie.sameSite);
            }
            // Set httpOnly if available (Servlet 3.0+)
            try {
                c.setHttpOnly(cookie.httpOnly);
            } catch (NoSuchMethodError | UnsupportedOperationException ignored) {
                // Ignore if not supported by the servlet container
            }
            servletResponse.addCookie(c);
        }

        // Content
        if (response.chunked) {
            servletResponse.setHeader("Transfer-Encoding", "chunked");
            // Actual chunk writing is handled by onWriteChunk.
            // We might need to copy the stream if response.direct is an InputStream and chunked.
            if (response.direct != null && response.direct instanceof InputStream) {
                copyStream(servletResponse, (InputStream) response.direct);
            }
            // Ensure response.out is handled by onWriteChunk if needed by application logic for chunking.
        } else {
            response.out.flush();
            if (response.direct != null && response.direct instanceof File) {
                File file = (File) response.direct;
                servletResponse.setHeader("Content-Length", String.valueOf(file.length()));
                if (!request.method.equals("HEAD")) {
                    copyStream(servletResponse, VirtualFile.open(file).inputstream());
                } else {
                    copyStream(servletResponse, new ByteArrayInputStream(new byte[0]));
                }
            } else if (response.direct != null && response.direct instanceof InputStream) {
                copyStream(servletResponse, (InputStream) response.direct);
            } else {
                byte[] content = response.out.toByteArray();
                servletResponse.setHeader("Content-Length", String.valueOf(content.length));
                if (!request.method.equals("HEAD")) {
                    writeToResponseStream(servletResponse, content);
                } else {
                    copyStream(servletResponse, new ByteArrayInputStream(new byte[0]));
                }
            }
        }
    }

    private void copyStream(HttpServletResponse servletResponse, InputStream is) throws IOException {
        if (servletResponse != null && is != null) {
            try {
                OutputStream os = servletResponse.getOutputStream();
                IOUtils.copyLarge(is, os);
                os.flush();
            } catch (IOException e) {
                if (isClientDisconnect(e)) {
                    Logger.debug(e, "Client disconnected");
                } else {
                    throw e;
                }
            } finally {
                closeQuietly(is);
            }
        }
    }

    public class ServletInvocation extends Invoker.DirectInvocation {

        private final Request request;
        private final Response response;
        private final HttpServletRequest httpServletRequest;
        private final HttpServletResponse httpServletResponse;

        public ServletInvocation(Request request, Response response, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
            this.httpServletRequest = httpServletRequest;
            this.httpServletResponse = httpServletResponse;
            this.request = request;
            this.response = response;
            request.args.put(JakartaServletWrapper.SERVLET_REQ, httpServletRequest);
            request.args.put(JakartaServletWrapper.SERVLET_RES, httpServletResponse);
        }

        @Override
        public boolean init() {
            try {
                return super.init();
            } catch (NotFound e) {
                serve404(httpServletRequest, httpServletResponse, e);
                return false;
            } catch (RenderStatic r) {
                try {
                    serveStatic(httpServletResponse, httpServletRequest, r);
                } catch (IOException e) {
                    throw new UnexpectedException(e);
                }
                return false;
            }
        }

        @Override
        public void run() {
            try {
                super.run();
            } catch (Exception e) {
                serve500(e, httpServletRequest, httpServletResponse);
                return;
            }
        }

        @Override
        public void execute() throws Exception {
            response.onWriteChunk(this::writeChunk);
            ActionInvoker.invoke(request, response);
            copyResponse(request, response, httpServletRequest, httpServletResponse);

        }

        private void writeChunk(Object chunk) {
            try {
                byte[] bytes;
                if (chunk instanceof byte[]) {
                    bytes = (byte[]) chunk;
                } else {
                    String message = chunk == null ? "" : chunk.toString();
                    bytes = message.getBytes(response.encoding);
                }
                writeToResponseStream(httpServletResponse, bytes);
            } catch (IOException e) {
                if (isClientDisconnect(e)) {
                    Logger.debug(e, "Client disconnected");
                } else {
                    throw new UnexpectedException(e);
                }
            }
        }

        @Override
        public InvocationContext getInvocationContext() {
            ActionInvoker.resolve(request);
            return new InvocationContext(Http.invocationType,
                    request.invokedMethod.getAnnotations(),
                    request.invokedMethod.getDeclaringClass().getAnnotations());
        }
    }
}

