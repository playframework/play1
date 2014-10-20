package play.modules.grizzly;

import com.sun.grizzly.tcp.http11.GrizzlyAdapter;
import com.sun.grizzly.tcp.http11.GrizzlyRequest;
import com.sun.grizzly.tcp.http11.GrizzlyResponse;
import com.sun.grizzly.util.http.Cookie;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import play.Invoker;
import play.Logger;
import play.Play;
import play.PlayPlugin;
import play.data.validation.Validation;
import play.exceptions.PlayException;
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

public class PlayGrizzlyAdapter extends GrizzlyAdapter {

    public PlayGrizzlyAdapter(File application, String id, String ctx) {
        Play.forceProd = true;
        Play.ctxPath = ctx;
        Play.init(application, id);
    }

    // ------------
    @Override
    public void service(GrizzlyRequest grizzlyRequest, GrizzlyResponse grizzlyResponse) {
        Request request = null;
        try {
            Response response = new Response();
            response.out = new ByteArrayOutputStream();
            Response.current.set(response);
            request = parseRequest(grizzlyRequest);
            boolean raw = false;
            for (PlayPlugin plugin : Play.plugins) {
                if (plugin.rawInvocation(request, response)) {
                    raw = true;
                    break;
                }
            }
            if (raw) {
                copyResponse(Request.current(), Response.current(), grizzlyRequest, grizzlyResponse);
            } else {
                Invoker.invokeInThread(new GrizzlyInvocation(request, response, grizzlyRequest, grizzlyResponse));
            }
        } catch (NotFound e) {
            serve404(grizzlyRequest, grizzlyResponse, e);
            return;
        } catch (RenderStatic e) {
            serveStatic(grizzlyRequest, grizzlyResponse, e);
            return;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public void serveStatic(GrizzlyRequest grizzlyRequest, GrizzlyResponse grizzlyResponse, RenderStatic renderStatic) {
        VirtualFile file = Play.getVirtualFile(renderStatic.file);
        if (file == null || file.isDirectory() || !file.exists()) {
            serve404(grizzlyRequest, grizzlyResponse, new NotFound("The file " + renderStatic.file + " does not exist"));
        } else {
            grizzlyResponse.setContentType(MimeTypes.getContentType(file.getName()));
            boolean raw = false;
            for (PlayPlugin plugin : Play.plugins) {
                if (plugin.serveStatic(file, Request.current(), Response.current())) {
                    raw = true;
                    break;
                }
            }
            try {
                if (raw) {
                    copyResponse(Request.current(), Response.current(), grizzlyRequest, grizzlyResponse);
                } else {
                    if (Play.mode == Play.Mode.DEV) {
                        grizzlyResponse.setHeader("Cache-Control", "no-cache");
                        grizzlyResponse.setHeader("Content-Length", String.valueOf(file.length()));
                        if (!grizzlyRequest.getMethod().equals("HEAD")) {
                            copyStream(grizzlyResponse, file.inputstream());
                        } else {
                            copyStream(grizzlyResponse, new ByteArrayInputStream(new byte[0]));
                        }
                    } else {
                        long last = file.lastModified();
                        String etag = "\"" + last + "-" + file.hashCode() + "\"";
                        if (!isModified(etag, last, grizzlyRequest)) {
                            grizzlyResponse.setHeader("Etag", etag);
                            grizzlyResponse.setStatus(304);
                        } else {
                            grizzlyResponse.setHeader("Last-Modified", Utils.getHttpDateFormatter().format(new Date(last)));
                            grizzlyResponse.setHeader("Cache-Control", "max-age=" + Play.configuration.getProperty("http.cacheControl", "3600"));
                            grizzlyResponse.setHeader("Etag", etag);
                            copyStream(grizzlyResponse, file.inputstream());
                        }
                    }

                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static boolean isModified(String etag, long last, GrizzlyRequest request) {
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

    public static Request parseRequest(GrizzlyRequest grizzlyRequest) throws Exception {
        Request request = new Http.Request();
        Request.current.set(request);
        URI uri = new URI(grizzlyRequest.getRequestURI());
        request.method = grizzlyRequest.getMethod().intern();
        request.path = uri.getPath();
        request.querystring = grizzlyRequest.getQueryString() == null ? "" : grizzlyRequest.getQueryString();

        Router.routeOnlyStatic(request);

        if (grizzlyRequest.getHeader("Content-Type") != null) {
            request.contentType = grizzlyRequest.getHeader("Content-Type").split(";")[0].trim().toLowerCase().intern();
        } else {
            request.contentType = "text/html".intern();
        }

        if (grizzlyRequest.getHeader("X-HTTP-Method-Override") != null) {
            request.method = grizzlyRequest.getHeader("X-HTTP-Method-Override").intern();
        }

        request.body = grizzlyRequest.getInputStream();
        request.secure = grizzlyRequest.isSecure();

        request.url = uri.toString() + (grizzlyRequest.getQueryString() == null ? "" : "?" + grizzlyRequest.getQueryString());
        request.host = grizzlyRequest.getHeader("host");
        if (request.host.contains(":")) {
            request.port = Integer.parseInt(request.host.split(":")[1]);
            request.domain = request.host.split(":")[0];
        } else {
            request.port = 80;
            request.domain = request.host;
        }

        request.remoteAddress = grizzlyRequest.getRemoteAddr();

        if (Play.configuration.containsKey("XForwardedSupport") && grizzlyRequest.getHeader("X-Forwarded-For") != null) {
            if (!Arrays.asList(Play.configuration.getProperty("XForwardedSupport", "127.0.0.1").split(",")).contains(request.remoteAddress)) {
                throw new RuntimeException("This proxy request is not authorized");
            } else {
                request.secure = ("https".equals(Play.configuration.get("XForwardedProto")) || "https".equals(grizzlyRequest.getHeader("X-Forwarded-Proto")) || "on".equals(grizzlyRequest.getHeader("X-Forwarded-Ssl")));
                if (Play.configuration.containsKey("XForwardedHost")) {
                    request.host = (String) Play.configuration.get("XForwardedHost");
                } else if (grizzlyRequest.getHeader("X-Forwarded-Host") != null) {
                    request.host = grizzlyRequest.getHeader("X-Forwarded-Host");
                }
                if (grizzlyRequest.getHeader("X-Forwarded-For") != null) {
                    request.remoteAddress = grizzlyRequest.getHeader("X-Forwarded-For");
                }
            }
        }

        Enumeration headersNames = grizzlyRequest.getHeaderNames();
        while (headersNames.hasMoreElements()) {
            Http.Header hd = new Http.Header();
            hd.name = (String) headersNames.nextElement();
            hd.values = new ArrayList<String>();
            Enumeration enumValues = grizzlyRequest.getHeaders(hd.name);
            while (enumValues.hasMoreElements()) {
                String value = (String) enumValues.nextElement();
                hd.values.add(value);
            }
            request.headers.put(hd.name.toLowerCase(), hd);
        }

        request.resolveFormat();

        Cookie[] cookies = grizzlyRequest.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
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

    public void serve404(GrizzlyRequest request, GrizzlyResponse response, NotFound e) {
        Logger.warn("404 -> %s %s (%s)", request.getMethod(), request.getRequestURI(), e.getMessage());
        response.setStatus(404);
        response.setContentType("text/html");
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
        response.setStatus(404);
        // Do we have an ajax request? If we have then we want to display some text even if it is html that is requested
        if ("XMLHttpRequest".equals(request.getHeader("X-Requested-With")) && (format == null || format.equals("html"))) {
            format = "txt";
        }
        if (format == null) {
            format = "txt";
        }
        response.setContentType(MimeTypes.getContentType("404." + format, "text/plain"));
        String errorHtml = TemplateLoader.load("errors/404." + format).render(binding);
        try {
            response.getOutputStream().write(errorHtml.getBytes("utf-8"));
        } catch (Exception fex) {
            Logger.error(fex, "(utf-8 ?)");
        }
    }

    public void serve500(Exception e, GrizzlyRequest request, GrizzlyResponse response) {
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

    public void copyResponse(Request request, Response response, GrizzlyRequest grizzlyRequest, GrizzlyResponse grizzlyResponse) throws IOException {
        if (response.contentType != null) {
            grizzlyResponse.setHeader("Content-Type", response.contentType + (response.contentType.startsWith("text/") ? "; charset=utf-8" : ""));
        } else {
            grizzlyResponse.setHeader("Content-Type", "text/plain;charset=utf-8");
        }

        grizzlyResponse.setStatus(response.status);
        if (!response.headers.containsKey("cache-control")) {
            grizzlyResponse.setHeader("Cache-Control", "no-cache");
        }
        Map<String, Http.Header> headers = response.headers;
        for (Map.Entry<String, Http.Header> entry : headers.entrySet()) {
            Http.Header hd = entry.getValue();
            String key = entry.getKey();
            for (String value : hd.values) {
                grizzlyResponse.setHeader(key, value);
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
            grizzlyResponse.addCookie(c);
        }

        // Content

        response.out.flush();
        if (response.direct != null && response.direct instanceof File) {
            File file = (File) response.direct;
            grizzlyResponse.setHeader("Content-Length", String.valueOf(file.length()));
            if (!request.method.equals("HEAD")) {
                copyStream(grizzlyResponse, VirtualFile.open(file).inputstream());
            } else {
                copyStream(grizzlyResponse, new ByteArrayInputStream(new byte[0]));
            }
        } else if (response.direct != null && response.direct instanceof InputStream) {
            copyStream(grizzlyResponse, (InputStream) response.direct);
        } else {
            byte[] content = response.out.toByteArray();
            grizzlyResponse.setHeader("Content-Length", String.valueOf(content.length));
            if (!request.method.equals("HEAD")) {
                grizzlyResponse.getOutputStream().write(content);
            } else {
                copyStream(grizzlyResponse, new ByteArrayInputStream(new byte[0]));
            }
        }

    }

    private void copyStream(GrizzlyResponse grizzlyResponse, InputStream is) throws IOException {
        OutputStream os = grizzlyResponse.getOutputStream();
        try {
            IOUtils.copyLarge(is, os);
            os.flush();
        }
        finally {
            is.close();
        }
    }

    public class GrizzlyInvocation extends Invoker.DirectInvocation {

        private Request request;
        private Response response;
        private GrizzlyRequest grizzlyRequest;
        private GrizzlyResponse grizzlyResponse;

        public GrizzlyInvocation(Request request, Response response, GrizzlyRequest grizzlyRequest, GrizzlyResponse grizzlyResponse) {
            this.grizzlyRequest = grizzlyRequest;
            this.grizzlyResponse = grizzlyResponse;
            this.request = request;
            this.response = response;
        }

        @Override
        public void run() {
            try {
                super.run();
            } catch (Exception e) {
                serve500(e, grizzlyRequest, grizzlyResponse);
                return;
            }
        }

        @Override
        public void execute() throws Exception {
            ActionInvoker.invoke(request, response);
            copyResponse(request, response, grizzlyRequest, grizzlyResponse);
        }
    }
}
