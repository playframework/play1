package play.server;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;

import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;

import play.Invoker;
import play.Logger;
import play.Play;
import play.PlayPlugin;
import play.exceptions.PlayException;
import play.libs.MimeTypes;
import play.libs.Utils;
import play.mvc.ActionInvoker;
import play.mvc.Http;
import play.mvc.Router;
import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.mvc.results.NotFound;
import play.mvc.results.RenderStatic;
import play.templates.TemplateLoader;
import play.vfs.FileSystemFile;
import play.vfs.VirtualFile;

/**
 * Servlet implementation.
 * Thanks to Lee Breisacher.
 */
public class ServletWrapper extends HttpServlet {

    @Override
    public void init(ServletConfig config) throws ServletException {
        String appDir = appDir = config.getServletContext().getRealPath("/WEB-INF/application");
        File root = new File(appDir);
        Play.init(root, System.getProperty("play.id", ""));       
    }

    @Override
    public void destroy() {
        Logger.trace("ServletWrapper>destroy");
        Play.stop();
    }

    @Override
    protected void service(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws ServletException, IOException {
        Logger.trace("ServletWrapper>service " + httpServletRequest.getRequestURI());
        Request request = null;
        try {
            request = parseRequest(httpServletRequest);
            Logger.trace("ServletWrapper>service, request: " + request);
        } catch (NotFound e) {
            Logger.trace("ServletWrapper>service, NotFound: " + e);
            serve404(httpServletRequest, httpServletResponse, e);
            return;
        } catch (RenderStatic e) {
            Logger.trace("ServletWrapper>service, RenderStatic: " + e);
            serveStatic(httpServletResponse, httpServletRequest, e);
            return;
        } catch (Exception e) {
            throw new ServletException(e);
        }
        Response response = new Response();
        response.out = new ByteArrayOutputStream();
        Invoker.invokeInThread(new ServletInvocation(request, response, httpServletRequest, httpServletResponse));
    }

    public void serveStatic(HttpServletResponse servletResponse, HttpServletRequest servletRequest, RenderStatic renderStatic) throws IOException {  
        VirtualFile file = Play.getVirtualFile(renderStatic.file);
        servletResponse.setContentType(MimeTypes.getMimeType(file.getName()));
        if (file == null || file.isDirectory() || !file.exists()) {
            //serve404(session, minaResponse, minaRequest, new NotFound("The file " + renderStatic.file + " does not exist"));
        } else {
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
                    attachFile(servletResponse, file);
                } else {
                    long last = file.lastModified();
                    String etag = last + "-" + file.hashCode();
                    if (!isModified(etag, last, servletRequest)) {
                        servletResponse.setHeader("Etag", etag);
                        servletResponse.setStatus(304);
                    } else {
                        servletResponse.setHeader("Last-Modified", Utils.getHttpDateFormatter().format(new Date(last)));
                        servletResponse.setHeader("Cache-Control", "max-age=" + Play.configuration.getProperty("http.cacheControl", "3600"));
                        servletResponse.setHeader("Etag", etag);
                        attachFile(servletResponse, file);
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
        URI uri = new URI(httpServletRequest.getRequestURI());
        request.method = httpServletRequest.getMethod().intern();
        request.path = StringUtils.substringAfter(uri.getPath(), httpServletRequest.getContextPath());
        request.querystring = httpServletRequest.getQueryString() == null ? "" : httpServletRequest.getQueryString();
        Logger.trace("httpServletRequest.getContextPath(): " + httpServletRequest.getContextPath());
        Logger.trace("request.path: " + request.path + ", request.querystring: " + request.querystring);

        Router.routeOnlyStatic(request);

        if (httpServletRequest.getHeader("Content-Type") != null) {
            request.contentType = httpServletRequest.getHeader("Content-Type").split(";")[0].trim().toLowerCase().intern();
        } else {
            request.contentType = "text/html".intern();
        }

        request.body = httpServletRequest.getInputStream();
        request.secure = httpServletRequest.isSecure();

        request.url = uri.toString();
        request.host = httpServletRequest.getHeader("host");
        if (request.host.contains(":")) {
            request.port = Integer.parseInt(request.host.split(":")[1]);
            request.domain = request.host.split(":")[0];
        } else {
            request.port = 80;
            request.domain = request.host;
        }

        request.remoteAddress = httpServletRequest.getRemoteAddr();

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
            request.headers.put(hd.name, hd);
        }

        request.resolveFormat();

        javax.servlet.http.Cookie[] cookies = httpServletRequest.getCookies();
        if (cookies != null) {
            for (javax.servlet.http.Cookie cookie : cookies) {
                Http.Cookie playCookie = new Http.Cookie();
                playCookie.name = cookie.getName();
                playCookie.path = cookie.getPath();
                playCookie.secure = cookie.getSecure();
                playCookie.value = cookie.getValue();
                request.cookies.put(playCookie.name, playCookie);
            }
        }

        return request;
    }
    
    public void serve404(HttpServletRequest servletRequest, HttpServletResponse servletResponse, NotFound e) {
        Logger.warn("404 -> %s %s (%s)", servletRequest.getMethod(), servletRequest.getRequestURI(), e.getMessage());
        servletResponse.setStatus(404);
        servletResponse.setContentType("text/html");
        Map<String, Object> binding = new HashMap<String, Object>();
        binding.put("result", e);
        String errorHtml = TemplateLoader.load("errors/404.html").render(binding);
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
                for (String key : cookies.keySet()) {
                    Http.Cookie cookie = cookies.get(key);
                    if (cookie.sendOnError) {
                        Cookie c = new Cookie(cookie.name, cookie.value);
                        c.setSecure(cookie.secure);
                        c.setPath(cookie.path);
                        response.addCookie(c);
                    }
                }
            } catch (Exception exx) {
                // humm ?
            }
            binding.put("exception", e);
            boolean ajax = "XMLHttpRequest".equals(request.getHeader("X-Requested-With"));
            response.setStatus(500);
            if (ajax) {
                response.setContentType("text/plain");
            } else {
                response.setContentType("text/html");
            }
            try {
                String errorHtml = TemplateLoader.load("errors/500." + (ajax ? "txt" : "html")).render(binding);
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
        Map<String, Http.Header> headers = response.headers;
        for (String key : headers.keySet()) {
            Http.Header hd = headers.get((key));
            for (String value : hd.values) {
                servletResponse.addHeader(key, value);
            }
        }

        Map<String, Http.Cookie> cookies = response.cookies;
        for (String key : cookies.keySet()) {
            Http.Cookie cookie = cookies.get(key);
            Cookie c = new Cookie(cookie.name, cookie.value);
            c.setSecure(cookie.secure);
            c.setPath(cookie.path);
            servletResponse.addCookie(c);
        }
        if (!response.headers.containsKey("cache-control")) {
            servletResponse.setHeader("Cache-Control", "no-cache");
        }
        
        // Content
        
        response.out.flush();
        if(response.direct != null) {
            attachFile(servletResponse, new FileSystemFile(response.direct));
        } else {
            servletResponse.getOutputStream().write(((ByteArrayOutputStream) response.out).toByteArray());
        }
        
    }

    private void attachFile(HttpServletResponse servletResponse, VirtualFile file) throws IOException {
        InputStream is = file.inputstream();
        OutputStream os = servletResponse.getOutputStream();
        byte[] buffer = new byte[8096];
        int read = 0;
        while((read = is.read(buffer))>0) {
            os.write(buffer, 0, read);
        }      
        os.flush();
        is.close();
    }

    public class ServletInvocation extends Invoker.Invocation {

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
        
        public void doIt() {
            try {
                super.doIt();
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
    }
}
