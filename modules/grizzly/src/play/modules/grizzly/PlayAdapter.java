package play.modules.grizzly;

import com.sun.grizzly.tcp.FileOutputBuffer;
import com.sun.grizzly.tcp.OutputBuffer;
import com.sun.grizzly.tcp.http11.GrizzlyAdapter;
import com.sun.grizzly.tcp.http11.GrizzlyRequest;
import com.sun.grizzly.tcp.http11.GrizzlyResponse;
import com.sun.grizzly.util.buf.ByteChunk;
import com.sun.grizzly.util.http.Cookie;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import play.Logger;
import play.Play;
import play.PlayPlugin;
import play.data.validation.Validation;
import play.exceptions.PlayException;
import play.libs.MimeTypes;
import play.mvc.Http;
import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.mvc.Scope;
import play.mvc.results.NotFound;
import play.mvc.results.RenderStatic;
import play.templates.TemplateLoader;
import play.utils.Utils;
import play.vfs.VirtualFile;

public class PlayAdapter extends GrizzlyAdapter {

    static ThreadLocal<GrizzlyResponse> localResponse = new ThreadLocal<GrizzlyResponse>();

    @Override
    public void service(GrizzlyRequest grizzlyRequest, GrizzlyResponse grizzlyResponse) {
        try {
            PlayAdapter.localResponse.set(grizzlyResponse);
            Response response = new Response();
            Response.current.set(response);
            response.out = new ByteArrayOutputStream();
            parseRequest(grizzlyRequest);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public static void copyResponse() throws Exception {
        GrizzlyResponse grizzlyResponse = PlayAdapter.localResponse.get();
        Request request = Request.current();
        Response response = Response.current();
        response.out.flush();
        Logger.trace("Invoke: " + request.path + ": " + response.status);
        if (response.contentType != null) {
            grizzlyResponse.setHeader("Content-Type", response.contentType + (response.contentType.startsWith("text/") && !response.contentType.contains("charset") ? "; charset=utf-8" : ""));
        } else {
            grizzlyResponse.setHeader("Content-Type", "text/plain; charset=utf-8");
        }
        grizzlyResponse.setStatus(response.status);
        Map<String, Http.Header> headers = response.headers;
        for (Map.Entry<String, Http.Header> entry : headers.entrySet()) {
            Http.Header hd = entry.getValue();
            for (String value : hd.values) {
                grizzlyResponse.addHeader(entry.getKey(), value);
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
        if (!response.headers.containsKey("cache-control") && !response.headers.containsKey("Cache-Control")) {
            grizzlyResponse.setHeader("Cache-Control", "no-cache");
        }
        if ((response.direct != null) && response.direct.isFile()) {
            grizzlyResponse.setHeader("Content-Length", "" + response.direct.length());
            FileInputStream fis = new FileInputStream(response.direct);
            com.sun.grizzly.tcp.Response res = grizzlyResponse.getResponse();
            OutputBuffer outputBuffer = res.getOutputBuffer();
            long length = response.direct.length();

            try {
                if ((outputBuffer instanceof FileOutputBuffer) && ((FileOutputBuffer) outputBuffer).isSupportFileSend()) {
                    res.flush();
                    long nWrite = 0;
                    while (nWrite < length) {
                        try {
                            nWrite += ((FileOutputBuffer) outputBuffer).sendFile(fis.getChannel(), nWrite, length - nWrite);
                        } catch (IOException e) {
                            // probably a broken pipe
                            break;
                        }
                    }
                } else {
                    byte b[] = new byte[8192];
                    ByteChunk chunk = new ByteChunk();
                    int rd = 0;
                    while ((rd = fis.read(b)) > 0) {
                        chunk.setBytes(b, 0, rd);
                        res.doWrite(chunk);
                    }
                }
            } catch (Exception e) {
                throw e;
            } finally {
                if (fis != null) {
                    try {
                        fis.close();
                    } catch (Exception e) {
                    }
                }
            }

        } else {
            grizzlyResponse.getOutputStream().write(response.out.toByteArray());
        }
    }

    public static Request parseRequest(GrizzlyRequest grizzlyRequest) throws Exception {
        URI uri = new URI(grizzlyRequest.getRequestURI());
        Request request = new Request();
        Http.Request.current.set(request);

        request.remoteAddress = grizzlyRequest.getRemoteAddr();
        request.method = grizzlyRequest.getMethod().intern();
        request.path = uri.getPath();
        request.querystring = grizzlyRequest.getQueryString() == null ? "" : grizzlyRequest.getQueryString();

        if (grizzlyRequest.getHeader("Content-Type") != null) {
            request.contentType = grizzlyRequest.getHeader("Content-Type").split(";")[0].trim().toLowerCase().intern();
        } else {
            request.contentType = "text/html".intern();
        }

        request.body = grizzlyRequest.getInputStream();

        request.url = uri.toString();
        request.host = grizzlyRequest.getHeader("host");

        if (request.host.contains(":")) {
            request.port = Integer.parseInt(request.host.split(":")[1]);
            request.domain = request.host.split(":")[0];
        } else {
            request.port = 80;
            request.domain = request.host;
        }

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

        for (Enumeration<String> it = grizzlyRequest.getHeaderNames(); it.hasMoreElements();) {
            String key = it.nextElement();
            Http.Header hd = new Http.Header();
            hd.name = key.toLowerCase();
            hd.values = new ArrayList<String>();
            for (Enumeration<String> itv = grizzlyRequest.getHeaders(key); itv.hasMoreElements();) {
                hd.values.add(itv.nextElement());
            }
            request.headers.put(hd.name, hd);
        }
        request.resolveFormat();

        if (grizzlyRequest.getCookies() != null) {
            for (Cookie cookie : grizzlyRequest.getCookies()) {
                Http.Cookie playCookie = new Http.Cookie();
                playCookie.name = cookie.getName();
                playCookie.path = cookie.getPath();
                playCookie.domain = cookie.getDomain();
                playCookie.secure = cookie.getSecure();
                playCookie.value = cookie.getValue();
                request.cookies.put(playCookie.name, playCookie);
            }
        }

        request._init();

        return request;
    }

    public static void serve404(NotFound e) throws Exception {
        GrizzlyResponse grizzlyResponse = PlayAdapter.localResponse.get();
        Request request = Request.current();
        Response response = Response.current();

        grizzlyResponse.setStatus(404);
        grizzlyResponse.setContentType("text/html");
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
        if ("XMLHttpRequest".equals(request.headers.get("x-requested-with")) && (format == null || format.equals("html"))) {
            format = "txt";
        }
        if (format == null) {
            format = "txt";
        }
        grizzlyResponse.setContentType(MimeTypes.getContentType("xxx." + format, "text/plain"));
        String errorHtml = TemplateLoader.load("errors/404." + format).render(binding);
        try {
            grizzlyResponse.getOutputStream().write(errorHtml.getBytes("utf-8"));
        } catch (UnsupportedEncodingException fex) {
            Logger.error(fex, "(utf-8 ?)");
        }
    }

    public static void serve500(Exception e) {
        GrizzlyResponse grizzlyResponse = PlayAdapter.localResponse.get();
        Request request = Request.current();
        Response response = Response.current();

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
                        grizzlyResponse.addCookie(c);
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
            String format = Request.current().format;
            grizzlyResponse.setStatus(500);
            if ("XMLHttpRequest".equals(request.headers.get("x-requested-with")) && (format == null || format.equals("html"))) {
                format = "txt";
            }
            if (format == null) {
                format = "txt";
            }
            grizzlyResponse.setContentType(MimeTypes.getContentType("xxx." + format, "text/plain"));
            try {
                String errorHtml = TemplateLoader.load("errors/500." + format).render(binding);
                grizzlyResponse.getOutputStream().write(errorHtml.getBytes("utf-8"));
                Logger.error(e, "Internal Server Error (500) for request %s", request.method + " " + request.url);
            } catch (Throwable ex) {
                Logger.error(e, "Internal Server Error (500) for request %s", request.method + " " + request.url);
                Logger.error(ex, "Error during the 500 response generation");
                try {
                    grizzlyResponse.getOutputStream().write("Internal Error (check logs)".getBytes("utf-8"));
                } catch (UnsupportedEncodingException fex) {
                    Logger.error(fex, "(utf-8 ?)");
                }
            }
        } catch (Throwable exxx) {
            try {
                grizzlyResponse.getOutputStream().write("Internal Error (check logs)".getBytes("utf-8"));
            } catch (Exception fex) {
                Logger.error(fex, "(utf-8 ?)");
            }
            if (exxx instanceof RuntimeException) {
                throw (RuntimeException) exxx;
            }
            throw new RuntimeException(exxx);
        }
    }

    public static void serveStatic(RenderStatic renderStatic) {
        GrizzlyResponse grizzlyResponse = PlayAdapter.localResponse.get();
        Request request = Request.current();
        Response response = Response.current();

        try {
            VirtualFile file = Play.getVirtualFile(renderStatic.file);
            if (file != null && file.exists() && file.isDirectory()) {
                file = file.child("index.html");
                if (file != null) {
                    renderStatic.file = file.relativePath();
                }
            }
            if ((file == null || !file.exists())) {
                serve404(new NotFound("The file " + renderStatic.file + " does not exist"));
            } else {
                boolean raw = false;
                for (PlayPlugin plugin : Play.plugins) {
                    if (plugin.serveStatic(file, Request.current(), Response.current())) {
                        raw = true;
                        break;
                    }
                }
                if (raw) {
                    copyResponse();
                } else {
                    if (Play.mode == Play.Mode.DEV) {
                        grizzlyResponse.setHeader("Cache-Control", "no-cache");
                    } else {
                        String maxAge = Play.configuration.getProperty("http.cacheControl", "3600");
                        if (maxAge.equals("0")) {
                            grizzlyResponse.setHeader("Cache-Control", "no-cache");
                        } else {
                            grizzlyResponse.setHeader("Cache-Control", "max-age=" + maxAge);
                        }
                    }
                    boolean useEtag = Play.configuration.getProperty("http.useETag", "true").equals("true");
                    long last = file.lastModified();
                    String etag = last + "-" + file.hashCode();
                    grizzlyResponse.setHeader("Last-Modified", Utils.getHttpDateFormatter().format(new Date(last)));
                    if (useEtag) {
                        grizzlyResponse.setHeader("Etag", etag);
                    }
                    grizzlyResponse.setStatus(200);
                    grizzlyResponse.setHeader("Content-Type", MimeTypes.getContentType(file.getName()));
                    grizzlyResponse.setHeader("Content-Length", "" + file.length());

                    com.sun.grizzly.tcp.Response res = grizzlyResponse.getResponse();
                    OutputBuffer outputBuffer = res.getOutputBuffer();
                    FileInputStream fis = (FileInputStream) file.inputstream();
                    long length = file.length();

                    try {
                        if ((outputBuffer instanceof FileOutputBuffer) && ((FileOutputBuffer) outputBuffer).isSupportFileSend()) {
                            res.flush();
                            long nWrite = 0;
                            while (nWrite < length) {
                                try {
                                    nWrite += ((FileOutputBuffer) outputBuffer).sendFile(fis.getChannel(), nWrite, length - nWrite);
                                } catch (IOException e) {
                                    // probably a broken pipe
                                    break;
                                }
                            }
                        } else {
                            byte b[] = new byte[8192];
                            ByteChunk chunk = new ByteChunk();
                            int rd = 0;
                            while ((rd = fis.read(b)) > 0) {
                                chunk.setBytes(b, 0, rd);
                                res.doWrite(chunk);
                            }
                        }
                    } catch (Exception e) {
                        throw e;
                    } finally {
                        if (fis != null) {
                            try {
                                fis.close();
                            } catch (Exception e) {
                            }
                        }
                    }

                }
            }
        } catch (Exception e) {
            Logger.error(e, "serveStatic");
            try {
                grizzlyResponse.getOutputStream().write("Internal Error (check logs)".getBytes("utf-8"));
            } catch (Exception ex) {
                //
            }
        }
    }
}
