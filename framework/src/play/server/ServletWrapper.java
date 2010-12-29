package play.server;

import org.apache.commons.lang.StringUtils;
import play.Invoker;
import play.Invoker.InvocationContext;
import play.Logger;
import play.Play;
import play.PlayPlugin;
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
import java.text.ParseException;
import java.util.*;

/**
 * Servlet implementation.
 * Thanks to Lee Breisacher.
 */
public class ServletWrapper extends HttpServlet implements ServletContextListener {

    private volatile boolean routerInitializedWithContext = false;

    public void contextInitialized(ServletContextEvent e) {
        String appDir = e.getServletContext().getRealPath("/WEB-INF/application");
        File root = new File(appDir);
        final String playId = e.getServletContext().getInitParameter("play.id");
        if (StringUtils.isEmpty(playId)) {
            throw new UnexpectedException("Please define a play.id parameter in your web.xml file. Without that parameter, play! cannot start your application. Please add a context-param into the WEB-INF/web.xml file.");
        }
        // This is really important as we know this parameter already (we are running in a servlet container)
        Play.frameworkPath = root.getParentFile();
        Play.usePrecompiled = true;
        Play.init(root, playId);
        
        // Servlet 2.4 does not allow you to get the context path from the servletcontext...
        if (isGreaterThan(e.getServletContext(), 2, 4)) {
            loadRouter(e.getServletContext().getContextPath());
        }
    }

    public void contextDestroyed(ServletContextEvent e) {
        Play.stop();
    }

    @Override
    public void destroy() {
        Logger.trace("ServletWrapper>destroy");
        Play.stop();
    }

    private void loadRouter(String contextPath) {
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
            // Reload the rules, but this time with the context. Not really efficient through...
            // Servlet 2.4 does not allow you to get the context path from the servletcontext...
            loadRouter(httpServletRequest.getContextPath());
        }

        Logger.trace("ServletWrapper>service " + httpServletRequest.getRequestURI());
        Request request = null;
        try {
            Response response = new Response();
            response.out = new ByteArrayOutputStream();
            Response.current.set(response);
            request = parseRequest(httpServletRequest);
            Logger.trace("ServletWrapper>service, request: " + request);
            boolean raw = false;
            for (PlayPlugin plugin : Play.plugins) {
                if (plugin.rawInvocation(request, response)) {
                    raw = true;
                    break;
                }
            }
            if (raw) {
                copyResponse(Request.current(), Response.current(), httpServletRequest, httpServletResponse);
            } else {
                Invoker.invokeInThread(new ServletInvocation(request, response, httpServletRequest, httpServletResponse));
            }
        } catch (NotFound e) {
            Logger.trace("ServletWrapper>service, NotFound: " + e);
            serve404(httpServletRequest, httpServletResponse, e);
            return;
        } catch (RenderStatic e) {
            Logger.trace("ServletWrapper>service, RenderStatic: " + e);
            serveStatic(httpServletResponse, httpServletRequest, e);
            return;
        } catch (Throwable e) {
            throw new ServletException(e);
        }
    }

    public void serveStatic(HttpServletResponse servletResponse, HttpServletRequest servletRequest, RenderStatic renderStatic) throws IOException {

        VirtualFile file = Play.getVirtualFile(renderStatic.file);
        if (file == null || file.isDirectory() || !file.exists()) {
            serve404(servletRequest, servletResponse, new NotFound("The file " + renderStatic.file + " does not exist"));
        } else {
            servletResponse.setContentType(MimeTypes.getContentType(file.getName()));
            boolean raw = false;
            for (PlayPlugin plugin : Play.plugins) {
                if (plugin.serveStatic(file, Request.current(), Response.current())) {
                    raw = true;
                    break;
                }
            }
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
                    if (!isModified(etag, last, servletRequest)) {
                        servletResponse.setHeader("Etag", etag);
                        servletResponse.setStatus(304);
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

    public static boolean isModified(String etag, long last, HttpServletRequest request) {
        if (!(request.getHeader("If-None-Match") == null && request.getHeaders("If-Modified-Since") == null)) {
            return true;
        } else {
            String browserEtag = request.getHeader("If-None-Match");
            if (!browserEtag.equals(etag)) {
                return true;
            } else {
                try {
                    Date browserDate = Utils.getHttpDateFormatter().parse(request.getHeader("If-Modified-Since"));
                    if (browserDate.getTime() >= last) {
                        return false;
                    }
                } catch (ParseException ex) {
                    Logger.error("Can't parse date", ex);
                }
                return true;
            }
        }
    }

    public static Request parseRequest(HttpServletRequest httpServletRequest) throws Exception {
        Request request = new Http.Request();
        Request.current.set(request);
        URI uri = new URI(httpServletRequest.getRequestURI());
        request.method = httpServletRequest.getMethod().intern();
        request.path = uri.getPath();
        request.querystring = httpServletRequest.getQueryString() == null ? "" : httpServletRequest.getQueryString();
        Logger.trace("httpServletRequest.getContextPath(): " + httpServletRequest.getContextPath());
        Logger.trace("request.path: " + request.path + ", request.querystring: " + request.querystring);

        Router.routeOnlyStatic(request);

        if (httpServletRequest.getHeader("Content-Type") != null) {
            request.contentType = httpServletRequest.getHeader("Content-Type").split(";")[0].trim().toLowerCase().intern();
        } else {
            request.contentType = "text/html".intern();
        }

        if (httpServletRequest.getHeader("X-HTTP-Method-Override") != null) {
            request.method = httpServletRequest.getHeader("X-HTTP-Method-Override").intern();
        }

        request.body = httpServletRequest.getInputStream();
        request.secure = httpServletRequest.isSecure();

        request.url = uri.toString() + (httpServletRequest.getQueryString() == null ? "" : "?" + httpServletRequest.getQueryString());
        request.host = httpServletRequest.getHeader("host");
        if (request.host.contains(":")) {
            request.port = Integer.parseInt(request.host.split(":")[1]);
            request.domain = request.host.split(":")[0];
        } else {
            request.port = 80;
            request.domain = request.host;
        }

        request.remoteAddress = httpServletRequest.getRemoteAddr();

        if (Play.configuration.containsKey("XForwardedSupport") && httpServletRequest.getHeader("X-Forwarded-For") != null) {
            if (!Arrays.asList(Play.configuration.getProperty("XForwardedSupport", "127.0.0.1").split(",")).contains(request.remoteAddress)) {
                throw new RuntimeException("This proxy request is not authorized");
            } else {
                request.secure = ("https".equals(Play.configuration.get("XForwardedProto")) || "https".equals(httpServletRequest.getHeader("X-Forwarded-Proto")) || "on".equals(httpServletRequest.getHeader("X-Forwarded-Ssl")));
                if (Play.configuration.containsKey("XForwardedHost")) {
                    request.host = (String) Play.configuration.get("XForwardedHost");
                } else if (httpServletRequest.getHeader("X-Forwarded-Host") != null) {
                    request.host = httpServletRequest.getHeader("X-Forwarded-Host");
                }
                if (httpServletRequest.getHeader("X-Forwarded-For") != null) {
                    request.remoteAddress = httpServletRequest.getHeader("X-Forwarded-For");
                }
            }
        }
        
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
            request.headers.put(hd.name.toLowerCase(), hd);
        }

        request.resolveFormat();

        javax.servlet.http.Cookie[] cookies = httpServletRequest.getCookies();
        if (cookies != null) {
            for (javax.servlet.http.Cookie cookie : cookies) {
                Http.Cookie playCookie = new Http.Cookie();
                playCookie.name = cookie.getName();
                playCookie.path = cookie.getPath();
                playCookie.domain = cookie.getDomain();
                playCookie.secure = cookie.getSecure();
                playCookie.value = cookie.getValue();
                playCookie.maxAge = cookie.getMaxAge();
                request.cookies.put(playCookie.name, playCookie);
            }
        }

        request._init();

        return request;
    }

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
            servletResponse.getOutputStream().write(errorHtml.getBytes("utf-8"));
        } catch (Exception fex) {
            Logger.error(fex, "(utf-8 ?)");
        }
    }

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
                response.getOutputStream().write(errorHtml.getBytes("utf-8"));
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
        if (response.contentType != null) {
            servletResponse.setHeader("Content-Type", response.contentType + (response.contentType.startsWith("text/") ? "; charset=utf-8" : ""));
        } else {
            servletResponse.setHeader("Content-Type", "text/plain;charset=utf-8");
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
            servletResponse.addCookie(c);
        }

        // Content

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
                servletResponse.getOutputStream().write(content);
            } else {
                copyStream(servletResponse, new ByteArrayInputStream(new byte[0]));
            }
        }

    }

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
        }

        @Override
        public boolean init() {
            try {
                return super.init();
            } catch(NotFound e) {
                serve404(httpServletRequest, httpServletResponse, e);
                return false;
            } catch(RenderStatic r) {
                try {
                    serveStatic(httpServletResponse, httpServletRequest, r);
                } catch(IOException e) {
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
            return new InvocationContext(request.invokedMethod.getAnnotations(), request.invokedMethod.getDeclaringClass().getAnnotations());
        }
        
    }
}
