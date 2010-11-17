package play.server;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBufferInputStream;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.*;
import org.jboss.netty.handler.codec.http.*;
import org.jboss.netty.handler.stream.ChunkedFile;
import org.jboss.netty.handler.stream.ChunkedStream;
import play.Invoker;
import play.Logger;
import play.Play;
import play.PlayPlugin;

import play.exceptions.PlayException;
import play.exceptions.UnexpectedException;
import play.i18n.Messages;
import play.libs.MimeTypes;
import play.mvc.ActionInvoker;
import play.mvc.Http;
import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.mvc.Router;
import play.mvc.Scope;
import play.mvc.results.NotFound;
import play.mvc.results.RenderStatic;
import play.templates.JavaExtensions;
import play.templates.TemplateLoader;
import play.utils.Utils;
import play.vfs.VirtualFile;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.ParseException;
import java.util.*;
import javax.mail.internet.InternetAddress;

import play.data.validation.Validation;


import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.*;

public class PlayHandler extends SimpleChannelUpstreamHandler {

    private final static String signature = "Play! Framework;" + Play.version + ";" + Play.mode.name().toLowerCase();

    public Request processRequest(Request request) {
        return request;
    }


    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        Logger.trace("messageReceived: begin");

        final Object msg = e.getMessage();
        if (msg instanceof HttpRequest) {
            final HttpRequest nettyRequest = (HttpRequest) msg;
            try {
                Request request = parseRequest(ctx, nettyRequest);
                request = processRequest(request);

                final Response response = new Response();

                Http.Response.current.set(response);
                response.out = new ByteArrayOutputStream();
                boolean raw = false;
                for (PlayPlugin plugin : Play.plugins) {
                    if (plugin.rawInvocation(request, response)) {
                        raw = true;
                        break;
                    }
                }
                if (raw) {
                    copyResponse(ctx, request, response, nettyRequest);
                } else {
                    Invoker.invoke(new NettyInvocation(request, response, ctx, nettyRequest, e));
                }

            } catch (Exception ex) {
                serve500(ex, ctx, nettyRequest);
            }
        }
        Logger.trace("messageReceived: end");
    }

    private static Map<String, RenderStatic> staticPathsCache = new HashMap<String, RenderStatic>();

    public class NettyInvocation extends Invoker.Invocation {

        private final ChannelHandlerContext ctx;
        private final Request request;
        private final Response response;
        private final HttpRequest nettyRequest;
        private final MessageEvent e;

        public NettyInvocation(Request request, Response response, ChannelHandlerContext ctx, HttpRequest nettyRequest, MessageEvent e) {
            this.ctx = ctx;
            this.request = request;
            this.response = response;
            this.nettyRequest = nettyRequest;
            this.e = e;
        }

        @Override
        public boolean init() {
            Logger.trace("init: begin");
            Request.current.set(request);
            Response.current.set(response);
            super.init();
            if (Play.mode == Play.Mode.PROD && staticPathsCache.containsKey(request.path)) {
                RenderStatic rs = null;
                synchronized (staticPathsCache) {
                    rs = staticPathsCache.get(request.path);
                }
                serveStatic(rs, ctx, request, response, nettyRequest, e);
                Logger.trace("init: end false");
                return false;
            }
            try {
                Router.routeOnlyStatic(request);
            } catch (NotFound e) {
                serve404(e, ctx, request, nettyRequest);
                Logger.trace("init: end false");
                return false;
            } catch (RenderStatic e) {
                if (Play.mode == Play.Mode.PROD) {
                    synchronized (staticPathsCache) {
                        staticPathsCache.put(request.path, e);
                    }
                }
                serveStatic(e, ctx, request, response, nettyRequest, this.e);
                Logger.trace("init: end false");
                return false;
            }
            Logger.trace("init: end true");
            return true;
        }

        @Override
        public void run() {
            try {
                Logger.trace("run: begin");
                super.run();
            } catch (Exception e) {
                serve500(e, ctx, nettyRequest);
            }
            Logger.trace("run: end");
        }

        @Override
        public void execute() throws Exception {
            if (!ctx.getChannel().isConnected()) {
                try {
                    ctx.getChannel().close();
                } catch (Throwable e) {
                    // Ignore
                }
                return;
            }

            // Check the exceeded size before re rendering so we can render the error if the size is exceeded
            saveExceededSizeError(nettyRequest, request, response);
            ActionInvoker.invoke(request, response);
        }

        @Override
        public void onSuccess() throws Exception {
            super.onSuccess();
            copyResponse(ctx, request, response, nettyRequest);
            Logger.trace("execute: end");
        }
    }

    void saveExceededSizeError(HttpRequest nettyRequest, Request request, Response response) {

        String warning = nettyRequest.getHeader(HttpHeaders.Names.WARNING);
        String length = nettyRequest.getHeader(HttpHeaders.Names.CONTENT_LENGTH);
        if (warning != null) {
            Logger.trace("saveExceededSizeError: begin");
            try {
                StringBuilder error = new StringBuilder();
                error.append("\u0000");
                // Cannot put warning which is play.netty.content.length.exceeded
                // as Key as it will result error when printing error
                error.append("play.netty.maxContentLength");
                error.append(":");
                String size = null;
                try {
                    size = JavaExtensions.formatSize(Long.parseLong(length));
                } catch (Exception e) {
                    size = length + " bytes";
                }
                error.append(Messages.get(warning, size));
                error.append("\u0001");
                error.append(size);
                error.append("\u0000");
                if (request.cookies.get(Scope.COOKIE_PREFIX + "_ERRORS") != null && request.cookies.get(Scope.COOKIE_PREFIX + "_ERRORS").value != null) {
                    error.append(request.cookies.get(Scope.COOKIE_PREFIX + "_ERRORS").value);
                }
                String errorData = URLEncoder.encode(error.toString(), "utf-8");
                Http.Cookie c = new Http.Cookie();
                c.value = errorData;
                c.name = Scope.COOKIE_PREFIX + "_ERRORS";
                request.cookies.put(Scope.COOKIE_PREFIX + "_ERRORS", c);
                Logger.trace("saveExceededSizeError: end");
            } catch (Exception e) {
                throw new UnexpectedException("Error serialization problem", e);
            }
        }
    }

    protected static void addToResponse(Response response, HttpResponse nettyResponse) {
        Map<String, Http.Header> headers = response.headers;
        for (Map.Entry<String, Http.Header> entry : headers.entrySet()) {
            Http.Header hd = entry.getValue();
            for (String value : hd.values) {
                nettyResponse.setHeader(entry.getKey(), value);
            }
        }
        Map<String, Http.Cookie> cookies = response.cookies;

        for (Http.Cookie cookie : cookies.values()) {
            CookieEncoder encoder = new CookieEncoder(true);
            Cookie c = new DefaultCookie(cookie.name, cookie.value);
            c.setSecure(cookie.secure);
            c.setPath(cookie.path);
            if (cookie.domain != null) {
                c.setDomain(cookie.domain);
            }
            if (cookie.maxAge != null) {
                c.setMaxAge(cookie.maxAge);
            }
            c.setHttpOnly(cookie.httpOnly);
            encoder.addCookie(c);
            nettyResponse.addHeader(SET_COOKIE, encoder.encode());
        }

        if (!response.headers.containsKey(CACHE_CONTROL)) {
            nettyResponse.setHeader(CACHE_CONTROL, "no-cache");
        }

    }

    protected static void writeResponse(ChannelHandlerContext ctx, Response response, HttpResponse nettyResponse, HttpRequest nettyRequest) {
        Logger.trace("writeResponse: begin");
        byte[] content = null;

        final boolean keepAlive = isKeepAlive(nettyRequest);
        if (nettyRequest.getMethod().equals(HttpMethod.HEAD)) {
            content = new byte[0];
        } else {
            content = response.out.toByteArray();
        }

        ChannelBuffer buf = ChannelBuffers.copiedBuffer(content);
        nettyResponse.setContent(buf);

        if (keepAlive) {
            // Add 'Content-Length' header only for a keep-alive connection.
            Logger.trace("writeResponse: content length [" + response.out.size() + "]");
            setContentLength(nettyResponse, response.out.size());
        }

        ChannelFuture f = ctx.getChannel().write(nettyResponse);

        // Decide whether to close the connection or not.
        if (!keepAlive) {
            // Close the connection when the whole content is written out.
            f.addListener(ChannelFutureListener.CLOSE);
        }
        Logger.trace("writeResponse: end");
    }

    public static void copyResponse(ChannelHandlerContext ctx, Request request, Response response, HttpRequest nettyRequest) throws Exception {
        Logger.trace("copyResponse: begin");
        //response.out.flush();

        // Decide whether to close the connection or not.

        HttpResponse nettyResponse = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.valueOf(response.status));
        nettyResponse.setHeader(SERVER, signature);

        if (response.contentType != null) {
            nettyResponse.setHeader(CONTENT_TYPE, response.contentType + (response.contentType.startsWith("text/") && !response.contentType.contains("charset") ? "; charset=utf-8" : ""));
        } else {
            nettyResponse.setHeader(CONTENT_TYPE, "text/plain; charset=utf-8");
        }

        addToResponse(response, nettyResponse);

        final Object obj = response.direct;
        File file = null;
        InputStream is = null;
        if (obj instanceof File) {
            file = (File) obj;
        } else if (obj instanceof InputStream) {
            is = (InputStream) obj;
        }

        final boolean keepAlive = isKeepAlive(nettyRequest);
        if (file != null && file.isFile()) {
            try {
                nettyResponse = addEtag(nettyRequest, nettyResponse, file);
                if (nettyResponse.getStatus().equals(HttpResponseStatus.NOT_MODIFIED)) {

                    Channel ch = ctx.getChannel();

                    // Write the initial line and the header.
                    ChannelFuture writeFuture = ch.write(nettyResponse);

                    if (!keepAlive) {
                        // Close the connection when the whole content is written out.
                        writeFuture.addListener(ChannelFutureListener.CLOSE);
                    }
                } else {
                    nettyResponse.setHeader(CONTENT_TYPE, MimeTypes.getContentType(file.getName(), "text/plain"));
                    final RandomAccessFile raf = new RandomAccessFile(file, "r");
                    try {
                        long fileLength = raf.length();

                        if (keepAlive) {
                            // Add 'Content-Length' header only for a keep-alive connection.
                            Logger.trace("file length is [" + fileLength + "]");
                            setContentLength(nettyResponse, fileLength);
                        }

                        Channel ch = ctx.getChannel();

                        // Write the initial line and the header.
                        ChannelFuture writeFuture = ch.write(nettyResponse);

                        // Write the content.
                        // If it is not a HEAD
                        if (!nettyRequest.getMethod().equals(HttpMethod.HEAD)) {
                            writeFuture = ch.write(new ChunkedFile(raf, 0, fileLength, 8192));
                        } else {
                            raf.close();
                        }
                        if (!keepAlive) {
                            // Close the connection when the whole content is written out.
                            writeFuture.addListener(ChannelFutureListener.CLOSE);
                        }
                    } catch(Exception e) {
                        try {
                            raf.close();
                            ctx.getChannel().close();
                        } catch(Exception ex) {
                            //
                        }
                    }

                }
            } catch (Exception e) {
                throw e;
            }
        } else if (is != null) {
            ChannelFuture writeFuture = ctx.getChannel().write(nettyResponse);
            if (!nettyRequest.getMethod().equals(HttpMethod.HEAD) && !nettyResponse.getStatus().equals(HttpResponseStatus.NOT_MODIFIED)) {
                writeFuture = ctx.getChannel().write(new ChunkedStream(is));
            }
            if (!keepAlive) {
                writeFuture.addListener(ChannelFutureListener.CLOSE);
            }
        } else {
            writeResponse(ctx, response, nettyResponse, nettyRequest);
        }
        Logger.trace("copyResponse: end");
    }

    static String getRemoteIPAddress(ChannelHandlerContext ctx) {
        String fullAddress = ((InetSocketAddress) ctx.getChannel().getRemoteAddress()).getAddress().getHostAddress();
        // Address resolves to /x.x.x.x:zzzz we only want x.x.x.x
        if (fullAddress.startsWith("/")) {
            fullAddress = fullAddress.substring(1);
        }
        int i = fullAddress.indexOf(":");
        if (i != -1) {
            fullAddress = fullAddress.substring(0, i);
        }
        return fullAddress;
    }

    public static Request parseRequest(ChannelHandlerContext ctx, HttpRequest nettyRequest) throws Exception {
        Logger.trace("parseRequest: begin");
        Logger.trace("parseRequest: URI = " + nettyRequest.getUri());
        int index = nettyRequest.getUri().indexOf("?");
        String querystring = "";
        
        String uri = nettyRequest.getUri();
        // Remove domain and port from URI if it's present.
        if (uri.startsWith("http://") || uri.startsWith("https://")) {
            // Begins searching / after 9th character (last / of https://)
            uri = uri.substring(uri.indexOf("/", 9));
        }
        
        String path = URLDecoder.decode(uri, "UTF-8");
        if (index != -1) {
            path = URLDecoder.decode(uri.substring(0, index), "UTF-8");
            querystring = uri.substring(index + 1);
        }

        final Request request = new Request();

        request.remoteAddress = getRemoteIPAddress(ctx);
        request.method = nettyRequest.getMethod().getName();
        request.path = path;
        request.querystring = querystring;
        final String contentType = nettyRequest.getHeader(CONTENT_TYPE);
        if (contentType != null) {
            request.contentType = contentType.split(";")[0].trim().toLowerCase();
        } else {
            request.contentType = "text/html";
        }

        if (nettyRequest.getHeader("X-HTTP-Method-Override") != null) {
            request.method = nettyRequest.getHeader("X-HTTP-Method-Override").intern();
        }

        ChannelBuffer b = nettyRequest.getContent();
        if (b instanceof FileChannelBuffer) {
            FileChannelBuffer buffer = (FileChannelBuffer) b;
            // An error occurred
            Integer max = Integer.valueOf(Play.configuration.getProperty("play.netty.maxContentLength", "-1"));

            request.body = buffer.getInputStream();
            if (!(max == -1 || request.body.available() < max)) {
                request.body = new ByteArrayInputStream(new byte[0]);
            }

        } else {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            IOUtils.copy(new ChannelBufferInputStream(b), out);
            byte[] n = out.toByteArray();
            request.body = new ByteArrayInputStream(n);
        }

        request.url = uri;
        request.host = nettyRequest.getHeader(HOST);
        request.isLoopback = ((InetSocketAddress) ctx.getChannel().getRemoteAddress()).getAddress().isLoopbackAddress() && request.host.matches("^127\\.0\\.0\\.1:?[0-9]*$");

        if (request.host == null) {
            request.host = "";
            request.port = 80;
            request.domain = "";
        } else {
            if (request.host.contains(":")) {
                final String[] host = request.host.split(":");
                request.port = Integer.parseInt(host[1]);
                request.domain = host[0];
            } else {
                request.port = 80;
                request.domain = request.host;
            }
        }

        if (Play.configuration.containsKey("XForwardedSupport") && nettyRequest.getHeader("X-Forwarded-For") != null) {
            if (!Arrays.asList(Play.configuration.getProperty("XForwardedSupport", "127.0.0.1").split(",")).contains(request.remoteAddress)) {
                throw new RuntimeException("This proxy request is not authorized: " + request.remoteAddress);
            } else {
                request.secure = ("https".equals(Play.configuration.get("XForwardedProto")) || "https".equals(nettyRequest.getHeader("X-Forwarded-Proto")) || "on".equals(nettyRequest.getHeader("X-Forwarded-Ssl")));
                if (Play.configuration.containsKey("XForwardedHost")) {
                    request.host = (String) Play.configuration.get("XForwardedHost");
                } else if (nettyRequest.getHeader("X-Forwarded-Host") != null) {
                    request.host = nettyRequest.getHeader("X-Forwarded-Host");
                }
                if (nettyRequest.getHeader("X-Forwarded-For") != null) {
                    request.remoteAddress = nettyRequest.getHeader("X-Forwarded-For");
                }
            }
        }


        addToRequest(nettyRequest, request);

        request.resolveFormat();

        request._init();

        Logger.trace("parseRequest: end");
        return request;
    }

    protected static void addToRequest(HttpRequest nettyRequest, Request request) {
        for (String key : nettyRequest.getHeaderNames()) {
            Http.Header hd = new Http.Header();
            hd.name = key.toLowerCase();
            hd.values = new ArrayList<String>();
            for (String next : nettyRequest.getHeaders(key)) {
                hd.values.add(next);
            }
            request.headers.put(hd.name, hd);
        }

        String value = nettyRequest.getHeader(COOKIE);
        if (value != null) {
            Set<Cookie> cookies = new CookieDecoder().decode(value);
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    Http.Cookie playCookie = new Http.Cookie();
                    playCookie.name = cookie.getName();
                    playCookie.path = cookie.getPath();
                    playCookie.domain = cookie.getDomain();
                    playCookie.secure = cookie.isSecure();
                    playCookie.value = cookie.getValue();
                    playCookie.httpOnly = cookie.isHttpOnly();
                    request.cookies.put(playCookie.name, playCookie);
                }
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
        try { e.getChannel().close(); } catch(Exception ex) { } 
    }

    public static void serve404(NotFound e, ChannelHandlerContext ctx, Request request, HttpRequest nettyRequest) {
        Logger.trace("serve404: begin");
        HttpResponse nettyResponse = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_FOUND);
        nettyResponse.setHeader(SERVER, signature);

        nettyResponse.setHeader(CONTENT_TYPE, "text/html");
        Map<String, Object> binding = getBindingForErrors(e, false);

        String format = Request.current().format;
        if (format == null || ("XMLHttpRequest".equals(request.headers.get("x-requested-with")) && "html".equals(format))) {
            format = "txt";
        }
        nettyResponse.setHeader(CONTENT_TYPE, (MimeTypes.getContentType("404." + format, "text/plain")));


        String errorHtml = TemplateLoader.load("errors/404." + format).render(binding);
        try {
            ChannelBuffer buf = ChannelBuffers.copiedBuffer(errorHtml.getBytes("utf-8"));
            nettyResponse.setContent(buf);
            ChannelFuture writeFuture = ctx.getChannel().write(nettyResponse);
            writeFuture.addListener(ChannelFutureListener.CLOSE);
        } catch (UnsupportedEncodingException fex) {
            Logger.error(fex, "(utf-8 ?)");
        }
        Logger.trace("serve404: end");
    }

    protected static Map<String, Object> getBindingForErrors(Exception e, boolean isError) {

        Map<String, Object> binding = new HashMap<String, Object>();
        if (!isError) {
            binding.put("result", e);
        } else {
            binding.put("exception", e);
        }
        binding.put("session", Scope.Session.current());
        binding.put("request", Http.Request.current());
        binding.put("flash", Scope.Flash.current());
        binding.put("params", Scope.Params.current());
        binding.put("play", new Play());
        try {
            binding.put("errors", Validation.errors());
        } catch (Exception ex) {
            //Logger.error(ex, "Error when getting Validation errors");
        }

        return binding;
    }

    // TODO: add request and response as parameter
    public static void serve500(Exception e, ChannelHandlerContext ctx, HttpRequest nettyRequest) {
        Logger.trace("serve500: begin");
        HttpResponse nettyResponse = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.INTERNAL_SERVER_ERROR);
        nettyResponse.setHeader(SERVER, signature);

        Request request = Request.current();
        Response response = Response.current();

        try {
            if (!(e instanceof PlayException)) {
                e = new play.exceptions.UnexpectedException(e);
            }

            // Flush some cookies
            try {

                Map<String, Http.Cookie> cookies = response.cookies;
                for (Http.Cookie cookie : cookies.values()) {
                    CookieEncoder encoder = new CookieEncoder(true);
                    Cookie c = new DefaultCookie(cookie.name, cookie.value);
                    c.setSecure(cookie.secure);
                    c.setPath(cookie.path);
                    if (cookie.domain != null) {
                        c.setDomain(cookie.domain);
                    }
                    if (cookie.maxAge != null) {
                        c.setMaxAge(cookie.maxAge);
                    }
                    c.setHttpOnly(cookie.httpOnly);
                    encoder.addCookie(c);
                    nettyResponse.addHeader(SET_COOKIE, encoder.encode());
                }

            } catch (Exception exx) {
                Logger.error(e, "Trying to flush cookies");
                // humm ?
            }
            Map<String, Object> binding = getBindingForErrors(e, true);

            String format = request.format;
            if (format == null || ("XMLHttpRequest".equals(request.headers.get("x-requested-with")) && "html".equals(format))) {
                format = "txt";
            }

            nettyResponse.setHeader("Content-Type", (MimeTypes.getContentType("500." + format, "text/plain")));
            try {
                String errorHtml = TemplateLoader.load("errors/500." + format).render(binding);

                ChannelBuffer buf = ChannelBuffers.copiedBuffer(errorHtml.getBytes("utf-8"));
                nettyResponse.setContent(buf);
                ChannelFuture writeFuture = ctx.getChannel().write(nettyResponse);
                writeFuture.addListener(ChannelFutureListener.CLOSE);
                Logger.error(e, "Internal Server Error (500) for request %s", request.method + " " + request.url);
            } catch (Throwable ex) {
                Logger.error(e, "Internal Server Error (500) for request %s", request.method + " " + request.url);
                Logger.error(ex, "Error during the 500 response generation");
                try {
                    ChannelBuffer buf = ChannelBuffers.copiedBuffer("Internal Error (check logs)".getBytes("utf-8"));
                    nettyResponse.setContent(buf);
                    ChannelFuture writeFuture = ctx.getChannel().write(nettyResponse);
                    writeFuture.addListener(ChannelFutureListener.CLOSE);
                } catch (UnsupportedEncodingException fex) {
                    Logger.error(fex, "(utf-8 ?)");
                }
            }
        } catch (Throwable exxx) {
            try {
                ChannelBuffer buf = ChannelBuffers.copiedBuffer("Internal Error (check logs)".getBytes("utf-8"));
                nettyResponse.setContent(buf);
                ChannelFuture writeFuture = ctx.getChannel().write(nettyResponse);
                writeFuture.addListener(ChannelFutureListener.CLOSE);
            } catch (Exception fex) {
                Logger.error(fex, "(utf-8 ?)");
            }
            if (exxx instanceof RuntimeException) {
                throw (RuntimeException) exxx;
            }
            throw new RuntimeException(exxx);
        }
        Logger.trace("serve500: end");
    }

    public static void serveStatic(RenderStatic renderStatic, ChannelHandlerContext ctx, Request request, Response response, HttpRequest nettyRequest, MessageEvent e) {
        Logger.trace("serveStatic: begin");
        HttpResponse nettyResponse = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.valueOf(response.status));
        nettyResponse.setHeader("Server", signature);
        try {
            VirtualFile file = Play.getVirtualFile(renderStatic.file);
            if (file != null && file.exists() && file.isDirectory()) {
                file = file.child("index.html");
                if (file != null) {
                    renderStatic.file = file.relativePath();
                }
            }
            if ((file == null || !file.exists())) {
                serve404(new NotFound("The file " + renderStatic.file + " does not exist"), ctx, request, nettyRequest);
            } else {
                boolean raw = false;
                for (PlayPlugin plugin : Play.plugins) {
                    if (plugin.serveStatic(file, Request.current(), Response.current())) {
                        raw = true;
                        break;
                    }
                }
                if (raw) {
                    copyResponse(ctx, request, response, nettyRequest);
                } else {
                    final File localFile = file.getRealFile();
                    final boolean keepAlive = isKeepAlive(nettyRequest);
                    nettyResponse = addEtag(nettyRequest, nettyResponse, localFile);

                    if (nettyResponse.getStatus().equals(HttpResponseStatus.NOT_MODIFIED)) {

                        Channel ch = e.getChannel();

                        // Write the initial line and the header.
                        ChannelFuture writeFuture = ch.write(nettyResponse);
                        if (!keepAlive) {
                            // Write the content.
                            writeFuture.addListener(ChannelFutureListener.CLOSE);
                        }
                    } else {

                        final RandomAccessFile raf = new RandomAccessFile(localFile, "r");
                        try {
                            long fileLength = raf.length();

                            Logger.trace("keep alive " + keepAlive);
                            Logger.trace("content type " + (MimeTypes.getContentType(localFile.getName(), "text/plain")));

                            if (keepAlive && !nettyResponse.getStatus().equals(HttpResponseStatus.NOT_MODIFIED)) {
                                // Add 'Content-Length' header only for a keep-alive connection.
                                Logger.trace("file length " + fileLength);
                                setContentLength(nettyResponse, fileLength);
                            }

                            nettyResponse.setHeader(CONTENT_TYPE, (MimeTypes.getContentType(localFile.getName(), "text/plain")));

                            Channel ch = e.getChannel();

                            // Write the initial line and the header.
                            ch.write(nettyResponse);

                            // Write the content.
                            ChannelFuture writeFuture = ch.write(new ChunkedFile(raf, 0, fileLength, 8192));

                            if (!keepAlive) {
                                // Close the connection when the whole content is written out.
                                writeFuture.addListener(ChannelFutureListener.CLOSE);
                            }
                        } catch(Exception ee) {
                            try {
                                raf.close();
                                ctx.getChannel().close();
                            } catch(Exception ex) {
                                //
                            }
                        }
                    }
                }

            }
        } catch (Exception ez) {
            Logger.error(ez, "serveStatic for request %s", request.method + " " + request.url);
            try {
                ChannelBuffer buf = ChannelBuffers.copiedBuffer("Internal Error (check logs)".getBytes("utf-8"));
                nettyResponse.setContent(buf);
                ChannelFuture future = ctx.getChannel().write(nettyResponse);
                future.addListener(ChannelFutureListener.CLOSE);
            } catch (Exception ex) {
                Logger.error(ez, "serveStatic for request %s", request.method + " " + request.url);
            }
        }
        Logger.trace("serveStatic: end");
    }

    public static boolean isModified(String etag, long last, HttpRequest nettyRequest) {

        if (nettyRequest.containsHeader(IF_NONE_MATCH)) {
            final String browserEtag = nettyRequest.getHeader(IF_NONE_MATCH);
            if (browserEtag.equals(etag)) {
                return false;
            }
            return true;
        }

        if (nettyRequest.containsHeader(IF_MODIFIED_SINCE)) {
            final String ifModifiedSince = nettyRequest.getHeader(IF_MODIFIED_SINCE);

            if (!StringUtils.isEmpty(ifModifiedSince)) {
                try {
                    Date browserDate = Utils.getHttpDateFormatter().parse(ifModifiedSince);
                    if (browserDate.getTime() >= last) {
                        return false;
                    }
                } catch (ParseException ex) {
                    Logger.warn("Can't parse HTTP date", ex);
                }
                return true;
            }
        }
        return true;
    }

    private static HttpResponse addEtag(HttpRequest nettyRequest, HttpResponse httpResponse, File file) {
        if (Play.mode == Play.Mode.DEV) {
            httpResponse.setHeader(CACHE_CONTROL, "no-cache");
        } else {
            String maxAge = Play.configuration.getProperty("http.cacheControl", "3600");
            if (maxAge.equals("0")) {
                httpResponse.setHeader(CACHE_CONTROL, "no-cache");
            } else {
                httpResponse.setHeader(CACHE_CONTROL, "max-age=" + maxAge);
            }
        }
        boolean useEtag = Play.configuration.getProperty("http.useETag", "true").equals("true");
        long last = file.lastModified();
        final String etag = "\"" + last + "-" + file.hashCode() + "\"";
        if (!isModified(etag, last, nettyRequest)) {
            if (nettyRequest.getMethod().equals(HttpMethod.GET)) {
                httpResponse.setStatus(HttpResponseStatus.NOT_MODIFIED);
            }
            if (useEtag) {
                httpResponse.setHeader(ETAG, etag);
            }

        } else {
            httpResponse.setHeader(LAST_MODIFIED, Utils.getHttpDateFormatter().format(new Date(last)));
            if (useEtag) {
                httpResponse.setHeader(ETAG, etag);
            }
        }
        return httpResponse;
    }

    public static boolean isKeepAlive(HttpMessage message) {
        return HttpHeaders.isKeepAlive(message) && message.getProtocolVersion().equals(HttpVersion.HTTP_1_1);
    }

    public static void setContentLength(HttpMessage message, long contentLength) {
        message.setHeader(HttpHeaders.Names.CONTENT_LENGTH, String.valueOf(contentLength));
    }
}
