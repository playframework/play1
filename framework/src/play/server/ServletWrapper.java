package play.server;

import org.apache.commons.lang.StringUtils;
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
import play.utils.Utils;
import play.vfs.VirtualFile;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URI;
import java.net.URLDecoder;
import java.text.ParseException;
import java.util.*;

/**
 * Servlet implementation.
 * Thanks to Lee Breisacher.
 */
public class ServletWrapper extends HttpServlet implements ServletContextListener {

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

    /*
     * ServletContextListener: Receives notification that the web application initialization process is starting.
     */
    public void contextInitialized(ServletContextEvent e) {
        // Set flags to make sure Play is aware.
        Play.standalonePlayServer = false;
        // Get real path to play-project/application
        String appDir = e.getServletContext().getRealPath("/WEB-INF/application");
        File root = new File(appDir);
        // Get servlet id from web.xml context-param
        final String playId = e.getServletContext().getInitParameter("play.id");
        if (StringUtils.isEmpty(playId)) {
            throw new UnexpectedException("Please define a play.id parameter in your web.xml file. Without that parameter, play! cannot start your application. Please add a context-param into the WEB-INF/web.xml file.");
        }
        // This is really important as we know this parameter already (we are running in a servlet container)
        Play.frameworkPath = root.getParentFile();
        // Additional flags to ensure to class reloading.
        Play.usePrecompiled = true;
        // Initialize Play stack.
        Play.init(root, playId);
        
        // Warn that Production mode has been forced (if Play configuration is in DEV mode).
        Play.Mode mode = Play.Mode.valueOf(Play.configuration.getProperty("application.mode", "DEV").toUpperCase());
        if (mode.isDev()) {
            Logger.info("Forcing PROD mode because deploying as a war file.");
        }

        // Servlet 2.4 or lower does not allow you to get the context path from the servletcontext...
        // If Major & Minor version of Servlet Container Spec exceeds 2.4, reload the Play router rules now with contextpath from ServletContext.
        if (isGreaterThan(e.getServletContext(), 2, 4)) {
            loadRouter(e.getServletContext().getContextPath());
        }
        
        // Servlet lifecycle continues in init() once, then service() for each incoming http request.
    }

    /*
     * ServletContextListener: Receives notification that the ServletContext is about to be shut down.
     */
    public void contextDestroyed(ServletContextEvent e) {
        Play.stop();
    }

    /*
     * GenericServlet: Called by the servlet container to indicate to a servlet that the servlet is being taken out of service. See Servlet#destroy. 
     */
    @Override
    public void destroy() {
        Logger.trace("ServletWrapper>destroy");
        Play.stop();
    }

    /*
     * Helper method: Reload router rules with a new contextpath.
     */
    private static synchronized void loadRouter(String contextPath) {
        // Reload the rules, but this time with the context. Not really efficient through...
        // Servlet 2.4 does not allow you to get the context path from the servletcontext...
        if (routerInitializedWithContext) return;  // Heri: This line seems redundant.
        Play.ctxPath = contextPath;
        Router.load(contextPath);
        routerInitializedWithContext = true;
    }

    /*
     * Helper method: Evaluate major & minor version numbers.
     */
    public static boolean isGreaterThan(ServletContext context, int majorVersion, int minorVersion) {
        int contextMajorVersion = context.getMajorVersion();
        int contextMinorVersion = context.getMinorVersion();
        return (contextMajorVersion > majorVersion) || (contextMajorVersion == majorVersion && contextMinorVersion > minorVersion);
    }

    /*
     * HttpServlet: Receives standard HTTP requests from the public service method and dispatches them to the doXXX methods defined in this class.
     */
    @Override
    protected void service(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws ServletException, IOException {

        // If Servlet spec version is 2.4 or lower, reload play router rules with contextpath from each incoming HttpServletRequest.
        if (!routerInitializedWithContext) {
            loadRouter(httpServletRequest.getContextPath());
        }

        // If Log tracing enabled, log requestURI of each incoming HttpServletRequest.
        if (Logger.isTraceEnabled()) {
            Logger.trace("ServletWrapper>service " + httpServletRequest.getRequestURI());
        }

        // play.mvc.Request & play.mvc.Response are needed to invoke Play actions.
        Request request = null;
        try {
            // Init an empty response:
            Response response = new Response();
            response.out = new ByteArrayOutputStream();
            Response.current.set(response);
            
            // Parse/copy HttpServletRequest into play.mvc.Request
            request = parseRequest(httpServletRequest);

            // If Log tracing enabled, log how each incoming HttpServletRequest has been converted into play.mvc.Request.
            if (Logger.isTraceEnabled()) {
                Logger.trace("ServletWrapper>service, request: " + request);
            }

            // Allow plugins a chance to intercept this request (via rawInvocation Hook).
            // Invoke on each enabled plugin, and return true immediately after one plugin.rawInvocation() succeeded.
            boolean raw = Play.pluginCollection.rawInvocation(request, response);
            if (raw) {
                // If a plugin intercepted this incoming play.mvc.Request (via rawInvocation hook), copy play.mvc.Response into HttpServletResponse. 
                copyResponse(Request.current(), Response.current(), httpServletRequest, httpServletResponse);
            } else {
                // If no plugin intercepted this incoming play.mvc.Request (via rawInvocation hook), then use the Play Invoker that calls controller actions.
                Invoker.invokeInThread(new ServletInvocation(request, response, httpServletRequest, httpServletResponse));
            }
            
        // Catch if play.mvc.results.NotFound exception (due to no route found/match):
        } catch (NotFound e) {
            if (Logger.isTraceEnabled()) {
                Logger.trace("ServletWrapper>service, NotFound: " + e);
            }
            // Respond with a pretty 404 Not-found page.
            serve404(httpServletRequest, httpServletResponse, e);
            return;
        // Catch if play.mvc.results.RenderStatic exception (due to static file requested):
        } catch (RenderStatic e) {
            if (Logger.isTraceEnabled()) {
                Logger.trace("ServletWrapper>service, RenderStatic: " + e);
            }
            // Respond with a binary output of the static file.
            serveStatic(httpServletResponse, httpServletRequest, e);
            return;
        // Catch all othe rruntime errors.
        } catch (Throwable e) {
            throw new ServletException(e);
        // Finally, clean-up play after each incoming Http request. (Request, Response, Scope, Cached Action-Arguments)
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

    /*
     * Helper method: Serve Static Files, such as images and js files.
     */
    public void serveStatic(HttpServletResponse servletResponse, HttpServletRequest servletRequest, RenderStatic renderStatic) throws IOException {

        // Use the Play Virtual File System (which is faster than disk i/o).
        VirtualFile file = Play.getVirtualFile(renderStatic.file);
        // If file not found, respond with a pretty 404 page (and http status header).
        if (file == null || file.isDirectory() || !file.exists()) {
            serve404(servletRequest, servletResponse, new NotFound("The file " + renderStatic.file + " does not exist"));
        // If file found, respond in a way that conforms to Http standards.
        } else {
            // Set Http MIME header.
            servletResponse.setContentType(MimeTypes.getContentType(file.getName()));
            // Allow plugins a chance to intercept this static request (via serveStatic Hook).
            boolean raw = Play.pluginCollection.serveStatic(file, Request.current(), Response.current());
            // If intercepted, copy play.mvc.Response into HttpServletResponse.
            if (raw) {
                copyResponse(Request.current(), Response.current(), servletRequest, servletResponse);
            // If not intercepted, respond in a way that depends DEV mode on Play configuration file.
            } else {
                // If developer mode, respond without HTTP etag-cache support (even if file unmodified).
                if (Play.mode == Play.Mode.DEV) {
                    servletResponse.setHeader("Cache-Control", "no-cache");
                    servletResponse.setHeader("Content-Length", String.valueOf(file.length()));
                    if (!servletRequest.getMethod().equals("HEAD")) {
                        copyStream(servletResponse, file.inputstream());
                    } else {
                        copyStream(servletResponse, new ByteArrayInputStream(new byte[0]));
                    }
                // If not developer mode, respond with HTTP etag-cache support (allows browser-cached copy if file unmodified).
                } else {
                    long last = file.lastModified();
                    String etag = "\"" + last + "-" + file.hashCode() + "\"";
                    // If Etag intact, respond with Http status header: 304 Unmodified.
                    if (!isModified(etag, last, servletRequest)) {
                        servletResponse.setHeader("Etag", etag);
                        servletResponse.setStatus(304);
                    // If Etag changed, respond with the new file (with new http etag header).
                    } else {
                        servletResponse.setHeader("Last-Modified", Utils.getHttpDateFormatter().format(new Date(last)));
                        servletResponse.setHeader("Cache-Control", "max-age=" + Play.configuration.getProperty("http.cacheControl", "3600"));
                        servletResponse.setHeader("Etag", etag);
                        copyStream(servletResponse, file.inputstream());
                    }
                }
            }
        }
    }
    
    /*
     * Helper method: Algorithm for HTTP Etag checking/matching.
     */
    public static boolean isModified(String etag, long last,
            HttpServletRequest request) {
        // See section 14.26 in rfc 2616 http://www.faqs.org/rfcs/rfc2616.html
        String browserEtag = request.getHeader(IF_NONE_MATCH);
        String dateString = request.getHeader(IF_MODIFIED_SINCE);
        if (browserEtag != null) {
            boolean etagMatches = browserEtag.equals(etag);
            if (!etagMatches) {
                return true;
            }
            if (dateString != null) {
                return !isValidTimeStamp(last, dateString);
            }
            return false;
        } else {
            if (dateString != null) {
                return !isValidTimeStamp(last, dateString);
            } else {
                return true;
            }
        }
    }

    /*
     * Helper method: Ensure timestamp in Etag string is valid.
     */
    private static boolean isValidTimeStamp(long last, String dateString) {
        try {
            long browserDate = Utils.getHttpDateFormatter().parse(dateString).getTime();
            return browserDate >= last;
        } catch (ParseException e) {
            Logger.error("Can't parse date", e);
            return false;
        }
    }

    /*
     * Wrapper method: Convert/parse/copy incoming HttpServletRequest into play.mcv.Request.
     */
    public static Request parseRequest(HttpServletRequest httpServletRequest) throws Exception {

        // Get URI.
        URI uri = new URI(httpServletRequest.getRequestURI());
        // Get HTTP Method (e.g. POST, GET)
        String method = httpServletRequest.getMethod().intern();
        // Get URL path (without domain/host). 
        String path = uri.getPath();
        // Get Query Strings (e.g. ?loc=sg&rand=1321)
        String querystring = httpServletRequest.getQueryString() == null ? "" : httpServletRequest.getQueryString();

        // Trace http request path and querystring.
        if (Logger.isTraceEnabled()) {
            Logger.trace("httpServletRequest.getContextPath(): " + httpServletRequest.getContextPath());
            Logger.trace("request.path: " + path + ", request.querystring: " + querystring);
        }

        // Get Content-Type from http header.
        String contentType = null;
        if (httpServletRequest.getHeader("Content-Type") != null) {
            contentType = httpServletRequest.getHeader("Content-Type").split(";")[0].trim().toLowerCase().intern();
        } else {
            contentType = "text/html".intern();  // Use faster String literals by calling intern().
        }

        // Get Method-Override from http header (commonly used by REST to encapsulate PUSH in a POST).
        if (httpServletRequest.getHeader("X-HTTP-Method-Override") != null) {
            method = httpServletRequest.getHeader("X-HTTP-Method-Override").intern();
        }

        // Get Request-Body.
        InputStream body = httpServletRequest.getInputStream();
        // Get HTTPS mode.
        boolean secure = httpServletRequest.isSecure();

        // Form proper url, domain & port, even if Servlet Container is cranky/weird.
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

        // Get Fully-qualified URI address.
        String remoteAddress = httpServletRequest.getRemoteAddr();

        // Get loopback: does host string in http header contain 127.0.0.1?
        boolean isLoopback = host.matches("^127\\.0\\.0\\.1:?[0-9]*$");


        // Create an immutable play.mvc.Request
        final Request request = Request.createRequest(
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


        // Set the Request object as the current global in Play.
        Request.current.set(request);
        
        // Throw RenderStatic Exception (if static file based on router rules) or NotFound Exception (if no match with router rules).
        // Does NOT do the actual invocation of controller actions.
        Router.routeOnlyStatic(request);

        return request;
    }

    /*
     * Helper method: Convert HttpServletRequest Enumeration of Headers, into a Map of play.mvc.Http.Header objects.
     */
    protected static Map<String, Http.Header> getHeaders(HttpServletRequest httpServletRequest) {
        Map<String, Http.Header> headers = new HashMap<String, Http.Header>(16);

        Enumeration headersNames = httpServletRequest.getHeaderNames();
        while (headersNames.hasMoreElements()) {
            Http.Header hd = new Http.Header();
            hd.name = (String) headersNames.nextElement();
            hd.values = new ArrayList<String>();
            Enumeration enumValues = httpServletRequest.getHeaders(hd.name);
            while (enumValues.hasMoreElements()) {
                String value = (String) enumValues.nextElement();
                hd.values.add(value);
            }
            headers.put(hd.name.toLowerCase(), hd);
        }

        return headers;
    }

    /*
     * Helper method: Convert HttpServletRequest Array of Cookies, into a Map of play.mvc.Http.Cookie
     */
    protected static Map<String, Http.Cookie> getCookies(HttpServletRequest httpServletRequest) {
        Map<String, Http.Cookie> cookies = new HashMap<String, Http.Cookie>(16);
        javax.servlet.http.Cookie[] cookiesViaServlet = httpServletRequest.getCookies();
        if (cookiesViaServlet != null) {
            for (javax.servlet.http.Cookie cookie : cookiesViaServlet) {
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

    /*
     * Helper Method: Respond with a pretty 404 Not-found html page, by using Play's TemplateLoader.
     */
    public void serve404(HttpServletRequest servletRequest, HttpServletResponse servletResponse, NotFound e) {
        Logger.warn("404 -> %s %s (%s)", servletRequest.getMethod(), servletRequest.getRequestURI(), e.getMessage());
        servletResponse.setStatus(404);
        servletResponse.setContentType("text/html");
        Map<String, Object> binding = new HashMap<String, Object>();
        binding.put("result", e);
        binding.put("session", Scope.Session.current());
        binding.put("request", Http.Request.current());
        binding.put("flash", Scope.Flash.current());
        binding.put("params", Scope.Params.current());
        binding.put("play", new Play());
        try {
            binding.put("errors", Validation.errors());
        } catch (Exception ex) {
            //
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
            servletResponse.getOutputStream().write(errorHtml.getBytes(Response.current().encoding));
        } catch (Exception fex) {
            Logger.error(fex, "(encoding ?)");
        }
    }

    /*
     * Helper Method: Respond with a pretty 500 Error html page, by using Play's TemplateLoader.
     * Some existing cookies that do not have sendOnError flag, will be flushed by this method.
     */
    public void serve500(Exception e, HttpServletRequest request, HttpServletResponse response) {
        try {
            Map<String, Object> binding = new HashMap<String, Object>();
            if (!(e instanceof PlayException)) {
                e = new play.exceptions.UnexpectedException(e);
            }
            // Flush some cookies
            try {
                Map<String, Http.Cookie> cookies = Response.current().cookies;
                for (Http.Cookie cookie : cookies.values()) {
                    if (cookie.sendOnError) {
                        Cookie c = new Cookie(cookie.name, cookie.value);
                        c.setSecure(cookie.secure);
                        c.setPath(cookie.path);
                        if (cookie.domain != null) {
                            c.setDomain(cookie.domain);
                        }
                        response.addCookie(c);
                    }
                }
            } catch (Exception exx) {
                // humm ?
            }
            binding.put("exception", e);
            binding.put("session", Scope.Session.current());
            binding.put("request", Http.Request.current());
            binding.put("flash", Scope.Flash.current());
            binding.put("params", Scope.Params.current());
            binding.put("play", new Play());
            try {
                binding.put("errors", Validation.errors());
            } catch (Exception ex) {
                //
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
                response.getOutputStream().write(errorHtml.getBytes(Response.current().encoding));
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

    /*
     * Wrapper method: Copy/clone play.mvc.Response into outgoing HttpServletResponse.
     */
    public void copyResponse(Request request, Response response, HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws IOException {
        // Get Response-Encoding (e.g. utf-8)
        String encoding = Response.current().encoding;
        if (response.contentType != null) {
            servletResponse.setHeader("Content-Type", response.contentType + (response.contentType.startsWith("text/") ? "; charset="+encoding : ""));
        } else {
            servletResponse.setHeader("Content-Type", "text/plain;charset="+encoding);
        }

        // Get Response-Status (e.g. 202 OK)
        servletResponse.setStatus(response.status);
        // Force no-cache response header if cache-control not specified by play.mvc.Response.
        if (!response.headers.containsKey("cache-control")) {
            servletResponse.setHeader("Cache-Control", "no-cache");
        }
        // Get Response-Headers
        Map<String, Http.Header> headers = response.headers;
        for (Map.Entry<String, Http.Header> entry : headers.entrySet()) {
            Http.Header hd = entry.getValue();
            String key = entry.getKey();
            for (String value : hd.values) {
                servletResponse.setHeader(key, value);
            }
        }

        // Get Response Cookies
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
            servletResponse.addCookie(c);
        }

        // Content

        // Flush the response outputstream, just in case.
        response.out.flush();
        // You may respond with a File, by setting play.mvc.Response.direct (in controller actions).
        if (response.direct != null && response.direct instanceof File) {
            File file = (File) response.direct;
            servletResponse.setHeader("Content-Length", String.valueOf(file.length()));
            if (!request.method.equals("HEAD")) {
                // Get Response-Body.
                copyStream(servletResponse, VirtualFile.open(file).inputstream());
            } else {
                // Asks for the response identical to the one that would correspond to a GET request, but without the response body.
                // This is useful for retrieving meta-information written in response headers, without having to transport the entire content.
                copyStream(servletResponse, new ByteArrayInputStream(new byte[0]));
            }
        // You may respond with a InputStream, by setting play.mvc.Response.direct (in controller actions).
        } else if (response.direct != null && response.direct instanceof InputStream) {
            copyStream(servletResponse, (InputStream) response.direct);
        // The below is the standard, whereby we calculate the Content-Length, 
        } else {
            byte[] content = response.out.toByteArray();
            servletResponse.setHeader("Content-Length", String.valueOf(content.length));
            if (!request.method.equals("HEAD")) {
                // Get Response-Body.
                servletResponse.getOutputStream().write(content);
            } else {
                // Asks for the response identical to the one that would correspond to a GET request, but without the response body.
                // This is useful for retrieving meta-information written in response headers, without having to transport the entire content.
                copyStream(servletResponse, new ByteArrayInputStream(new byte[0]));
            }
        }

    }

    /*
     * Helper method: Copy OutputStream to an InputStream synchronously.
     */
    private void copyStream(HttpServletResponse servletResponse, InputStream is) throws IOException {
        OutputStream os = servletResponse.getOutputStream();
        byte[] buffer = new byte[8096];
        int read = 0;
        while ((read = is.read(buffer)) > 0) {
            os.write(buffer, 0, read);
        }
        os.flush();
        is.close();
    }

    /*
     * Wrapper Class: Play Invoker must be passed in an Invocation Object (Java Multi-threaded Runnable). So we subclass.
     * Nothing special about this class, other than it is not generated by Netty side, but by ourselves.
     */
    public class ServletInvocation extends Invoker.DirectInvocation {

        private Request request;
        private Response response;
        private HttpServletRequest httpServletRequest;
        private HttpServletResponse httpServletResponse;

        public ServletInvocation(Request request, Response response, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
            this.httpServletRequest = httpServletRequest;
            this.httpServletResponse = httpServletResponse;
            this.request = request;
            this.response = response;
            request.args.put(ServletWrapper.SERVLET_REQ, httpServletRequest);
            request.args.put(ServletWrapper.SERVLET_RES, httpServletResponse);
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
            ActionInvoker.invoke(request, response);
            copyResponse(request, response, httpServletRequest, httpServletResponse);
        }

        @Override
        public InvocationContext getInvocationContext() {
            ActionInvoker.resolve(request, response);
            return new InvocationContext(Http.invocationType,
                    request.invokedMethod.getAnnotations(),
                    request.invokedMethod.getDeclaringClass().getAnnotations());
        }
    }
}
