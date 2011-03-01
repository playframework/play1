package play.server;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBufferInputStream;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.*;
import org.jboss.netty.handler.codec.http.*;
import org.jboss.netty.handler.codec.http.websocket.DefaultWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocket.WebSocketFrame;
import org.jboss.netty.handler.codec.http.websocket.WebSocketFrameDecoder;
import org.jboss.netty.handler.codec.http.websocket.WebSocketFrameEncoder;
import org.jboss.netty.handler.stream.ChunkedFile;
import org.jboss.netty.handler.stream.ChunkedStream;
import org.jboss.netty.handler.stream.ChunkedInput;
import org.jboss.netty.handler.stream.ChunkedWriteHandler;

import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.*;
import static org.jboss.netty.buffer.ChannelBuffers.*;

import play.Invoker;
import play.Invoker.InvocationContext;
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
import play.data.validation.Validation;
import play.libs.F.Action;
import play.libs.F.Promise;
import play.mvc.WebSocketInvoker;

public class PlayHandler extends SimpleChannelUpstreamHandler {

    /**
     * If true (the default), Play will send the HTTP header "Server: Play! Framework; ....".
     * This could be a security problem (old versions having publicly known security bugs), so you can
     * disable the header in application.conf: <code>http.exposePlayServer = false</code>
     */
    private final static String signature = "Play! Framework;" + Play.version + ";" + Play.mode.name().toLowerCase();
    private final static boolean exposePlayServer;

    static {
        exposePlayServer = !"false".equals(Play.configuration.getProperty("http.exposePlayServer"));
    }

    @Override
    public void messageReceived(final ChannelHandlerContext ctx, final MessageEvent e) throws Exception {
        Logger.trace("messageReceived: begin");
        final Object msg = e.getMessage();

        // Http request
        if (msg instanceof HttpRequest) {

            final HttpRequest nettyRequest = (HttpRequest) msg;

            // Websocket upgrade
            if (HttpHeaders.Values.UPGRADE.equalsIgnoreCase(nettyRequest.getHeader(CONNECTION)) && HttpHeaders.Values.WEBSOCKET.equalsIgnoreCase(nettyRequest.getHeader(HttpHeaders.Names.UPGRADE))) {
                websocketHandshake(ctx, nettyRequest, e);
                return;
            }

            // Plain old HttpRequest
            try {
                final Request request = parseRequest(ctx, nettyRequest);

                final Response response = new Response();
                Http.Response.current.set(response);

                // Buffered in memory output
                response.out = new ByteArrayOutputStream();

                // Direct output (will be set later)
                response.direct = null;

                // Streamed output (using response.writeChunk)
                response.onWriteChunk(new Action<Object>() {

                    public void invoke(Object result) {
                        writeChunk(request, response, ctx, nettyRequest, result);
                    }
                });

                // Raw invocation
                boolean raw = Play.pluginCollection.rawInvocation(request, response);
                if (raw) {
                    copyResponse(ctx, request, response, nettyRequest);
                } else {

                    // Deleguate to Play framework
                    Invoker.invoke(new NettyInvocation(request, response, ctx, nettyRequest, e));

                }

            } catch (Exception ex) {
                serve500(ex, ctx, nettyRequest);
            }
        }

        // Websocket frame
        if (msg instanceof WebSocketFrame) {
            WebSocketFrame frame = (WebSocketFrame) msg;
            websocketFrameReceived(ctx, frame);
        }

        Logger.trace("messageReceived: end");
    }
    private static final Map<String, RenderStatic> staticPathsCache = new HashMap<String, RenderStatic>();

    public class NettyInvocation extends Invoker.Invocation {

        private final ChannelHandlerContext ctx;
        private final Request request;
        private final Response response;
        private final HttpRequest nettyRequest;
        private final MessageEvent event;

        public NettyInvocation(Request request, Response response, ChannelHandlerContext ctx, HttpRequest nettyRequest, MessageEvent e) {
            this.ctx = ctx;
            this.request = request;
            this.response = response;
            this.nettyRequest = nettyRequest;
            this.event = e;
        }

        @Override
        public boolean init() {
            Logger.trace("init: begin");
            Request.current.set(request);
            Response.current.set(response);
            try {
                super.init();
                if (Play.mode == Play.Mode.PROD && staticPathsCache.containsKey(request.path)) {
                    RenderStatic rs = null;
                    synchronized (staticPathsCache) {
                        rs = staticPathsCache.get(request.path);
                    }
                    serveStatic(rs, ctx, request, response, nettyRequest, event);
                    Logger.trace("init: end false");
                    return false;
                }
                Router.routeOnlyStatic(request);
            } catch (NotFound nf) {
                serve404(nf, ctx, request, nettyRequest);
                Logger.trace("init: end false");
                return false;
            } catch (RenderStatic rs) {
                if (Play.mode == Play.Mode.PROD) {
                    synchronized (staticPathsCache) {
                        staticPathsCache.put(request.path, rs);
                    }
                }
                serveStatic(rs, ctx, request, response, nettyRequest, this.event);
                Logger.trace("init: end false");
                return false;
            }

            Logger.trace("init: end true");
            return true;
        }

        @Override
        public InvocationContext getInvocationContext() {
            ActionInvoker.resolve(request, response);
            return new InvocationContext(request.invokedMethod.getAnnotations(), request.invokedMethod.getDeclaringClass().getAnnotations());
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
            if (response.chunked) {
                closeChunked(request, response, ctx, nettyRequest);
            } else {
                copyResponse(ctx, request, response, nettyRequest);
            }
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

    public void copyResponse(ChannelHandlerContext ctx, Request request, Response response, HttpRequest nettyRequest) throws Exception {
        Logger.trace("copyResponse: begin");
        //response.out.flush();

        // Decide whether to close the connection or not.

        HttpResponse nettyResponse = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.valueOf(response.status));
        if (exposePlayServer) {
            nettyResponse.setHeader(SERVER, signature);
        }

        if (response.contentType != null) {
            nettyResponse.setHeader(CONTENT_TYPE, response.contentType + (response.contentType.startsWith("text/") && !response.contentType.contains("charset") ? "; charset=utf-8" : ""));
        } else {
            nettyResponse.setHeader(CONTENT_TYPE, "text/plain; charset=utf-8");
        }

        addToResponse(response, nettyResponse);

        final Object obj = response.direct;
        File file = null;
        ChunkedInput stream = null;
        InputStream is = null;
        if (obj instanceof File) {
            file = (File) obj;
        } else if (obj instanceof InputStream) {
            is = (InputStream) obj;
        } else if (obj instanceof ChunkedInput) {
            stream = (ChunkedInput) obj;
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
                    } catch (Throwable exx) {
                        try {
                            raf.close();
                        } catch (Throwable ex) { /* Left empty */ }
                        try {
                            ctx.getChannel().close();
                        } catch (Throwable ex) { /* Left empty */ }
                    }

                }
            } catch (Exception e) {
                throw e;
            }
        } else if (is != null) {
            ChannelFuture writeFuture = ctx.getChannel().write(nettyResponse);
            if (!nettyRequest.getMethod().equals(HttpMethod.HEAD) && !nettyResponse.getStatus().equals(HttpResponseStatus.NOT_MODIFIED)) {
                writeFuture = ctx.getChannel().write(new ChunkedStream(is));
            } else {
                is.close();
            }
            if (!keepAlive) {
                writeFuture.addListener(ChannelFutureListener.CLOSE);
            }
        } else if (stream != null) {
            ChannelFuture writeFuture = ctx.getChannel().write(nettyResponse);
            if (!nettyRequest.getMethod().equals(HttpMethod.HEAD) && !nettyResponse.getStatus().equals(HttpResponseStatus.NOT_MODIFIED)) {
                writeFuture = ctx.getChannel().write(stream);
            } else {
                stream.close();
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
        if (fullAddress.matches("/[0-9]+[.][0-9]+[.][0-9]+[.][0-9]+[:][0-9]+")) {
            fullAddress = fullAddress.substring(1);
            fullAddress = fullAddress.substring(0, fullAddress.indexOf(":"));
        } else if (fullAddress.matches(".*[%].*")) {
            fullAddress = fullAddress.substring(0, fullAddress.indexOf("%"));
        }
        return fullAddress;
    }

    public Request parseRequest(ChannelHandlerContext ctx, HttpRequest nettyRequest) throws Exception {
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
        try {
            e.getChannel().close();
        } catch (Exception ex) {
        }
    }

    public static void serve404(NotFound e, ChannelHandlerContext ctx, Request request, HttpRequest nettyRequest) {
        Logger.trace("serve404: begin");
        HttpResponse nettyResponse = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_FOUND);
        nettyResponse.setHeader(SERVER, signature);

        nettyResponse.setHeader(CONTENT_TYPE, "text/html");
        Map<String, Object> binding = getBindingForErrors(e, false);

        String format = Request.current().format;
        if (format == null) {
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
        if (exposePlayServer) {
            nettyResponse.setHeader(SERVER, signature);
        }

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
            if (format == null) {
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

    public void serveStatic(RenderStatic renderStatic, ChannelHandlerContext ctx, Request request, Response response, HttpRequest nettyRequest, MessageEvent e) {
        Logger.trace("serveStatic: begin");
        HttpResponse nettyResponse = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.valueOf(response.status));
        if (exposePlayServer) {
            nettyResponse.setHeader(SERVER, signature);
        }
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
                boolean raw = Play.pluginCollection.serveStatic(file, Request.current(), Response.current());
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
                            ChannelFuture writeFuture = ch.write(nettyResponse);

                            // Write the content.
                            if (!nettyRequest.getMethod().equals(HttpMethod.HEAD)) {
                                writeFuture = ch.write(new ChunkedFile(raf, 0, fileLength, 8192));
                            } else {
                                raf.close();
                            }

                            if (!keepAlive) {
                                // Close the connection when the whole content is written out.
                                writeFuture.addListener(ChannelFutureListener.CLOSE);
                            }
                        } catch (Throwable exx) {
                            try {
                                raf.close();
                            } catch (Throwable ex) { /* Left empty */ }
                            try {
                                ctx.getChannel().close();
                            } catch (Throwable ex) { /* Left empty */ }
                        }
                    }
                }

            }
        } catch (Throwable ez) {
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
    // ~~~~~~~~~~~ Chunked response
    final ChunkedWriteHandler chunkedWriteHandler = new ChunkedWriteHandler();

    static class LazyChunkedInput implements org.jboss.netty.handler.stream.ChunkedInput {

        private boolean closed = false;
        private ConcurrentLinkedQueue<Object> nextChunks = new ConcurrentLinkedQueue<Object>();

        public boolean hasNextChunk() throws Exception {
            return !nextChunks.isEmpty();
        }

        public Object nextChunk() throws Exception {
            if (nextChunks.isEmpty()) {
                return null;
            }
            return wrappedBuffer(((String) nextChunks.poll()).getBytes());
        }

        public boolean isEndOfInput() throws Exception {
            return closed && nextChunks.isEmpty();
        }

        public void close() throws Exception {
            if (!closed) {
                nextChunks.offer("0\r\n\r\n");
            }
            closed = true;
        }

        public void writeChunk(Object chunk) throws Exception {
            String message = chunk == null ? "" : chunk.toString();
            StringWriter writer = new StringWriter();
            Integer l = message.getBytes("utf-8").length + 2;
            writer.append(Integer.toHexString(l)).append("\r\n").append(message).append("\r\n\r\n");
            nextChunks.offer(writer.toString());
        }
    }

    public void writeChunk(Request playRequest, Response playResponse, ChannelHandlerContext ctx, HttpRequest nettyRequest, Object chunk) {
        try {
            if (playResponse.direct == null) {
                playResponse.setHeader("Transfer-Encoding", "chunked");
                playResponse.direct = new LazyChunkedInput();
                copyResponse(ctx, playRequest, playResponse, nettyRequest);
            }
            ((LazyChunkedInput) playResponse.direct).writeChunk(chunk);
            chunkedWriteHandler.resumeTransfer();
        } catch (Exception e) {
            throw new UnexpectedException(e);
        }
    }

    public void closeChunked(Request playRequest, Response playResponse, ChannelHandlerContext ctx, HttpRequest nettyRequest) {
        try {
            ((LazyChunkedInput) playResponse.direct).close();
            chunkedWriteHandler.resumeTransfer();
        } catch (Exception e) {
            throw new UnexpectedException(e);
        }
    }
    // ~~~~~~~~~~~ Websocket
    final static Map<ChannelHandlerContext, Http.Inbound> channels = new ConcurrentHashMap<ChannelHandlerContext, Http.Inbound>();

    private void websocketFrameReceived(final ChannelHandlerContext ctx, WebSocketFrame webSocketFrame) {
        Http.Inbound inbound = channels.get(ctx);
        if (webSocketFrame.isBinary()) {
            inbound._received(new Http.WebSocketFrame(webSocketFrame.getBinaryData().array()));
        } else {
            inbound._received(new Http.WebSocketFrame(webSocketFrame.getTextData()));
        }
    }

    private void websocketHandshake(final ChannelHandlerContext ctx, HttpRequest req, MessageEvent e) throws Exception {

        // Create the WebSocket handshake response.
        HttpResponse res = new DefaultHttpResponse(HttpVersion.HTTP_1_1, new HttpResponseStatus(101, "Web Socket Protocol Handshake"));
        res.addHeader(HttpHeaders.Names.UPGRADE, HttpHeaders.Values.WEBSOCKET);
        res.addHeader(CONNECTION, HttpHeaders.Values.UPGRADE);

        // Fill in the headers and contents depending on handshake method.
        if (req.containsHeader(SEC_WEBSOCKET_KEY1) && req.containsHeader(SEC_WEBSOCKET_KEY2)) {
            // New handshake method with a challenge:
            res.addHeader(SEC_WEBSOCKET_ORIGIN, req.getHeader(ORIGIN));
            res.addHeader(SEC_WEBSOCKET_LOCATION, "ws://" + req.getHeader(HttpHeaders.Names.HOST) + req.getUri());
            String protocol = req.getHeader(SEC_WEBSOCKET_PROTOCOL);
            if (protocol != null) {
                res.addHeader(SEC_WEBSOCKET_PROTOCOL, protocol);
            }

            // Calculate the answer of the challenge.
            String key1 = req.getHeader(SEC_WEBSOCKET_KEY1);
            String key2 = req.getHeader(SEC_WEBSOCKET_KEY2);
            int a = (int) (Long.parseLong(key1.replaceAll("[^0-9]", "")) / key1.replaceAll("[^ ]", "").length());
            int b = (int) (Long.parseLong(key2.replaceAll("[^0-9]", "")) / key2.replaceAll("[^ ]", "").length());
            long c = req.getContent().readLong();
            ChannelBuffer input = ChannelBuffers.buffer(16);
            input.writeInt(a);
            input.writeInt(b);
            input.writeLong(c);
            try {
                ChannelBuffer output = ChannelBuffers.wrappedBuffer(MessageDigest.getInstance("MD5").digest(input.array()));
                res.setContent(output);
            } catch (NoSuchAlgorithmException ex) {
                throw new UnexpectedException(ex);
            }
        } else {
            // Old handshake method with no challenge:
            res.addHeader(WEBSOCKET_ORIGIN, req.getHeader(ORIGIN));
            res.addHeader(WEBSOCKET_LOCATION, "ws://" + req.getHeader(HttpHeaders.Names.HOST) + req.getUri());
            String protocol = req.getHeader(WEBSOCKET_PROTOCOL);
            if (protocol != null) {
                res.addHeader(WEBSOCKET_PROTOCOL, protocol);
            }
        }

        // Keep the original request
        Http.Request request = parseRequest(ctx, req);

        // Route the websocket request
        request.method = "WS";
        Map<String, String> route = Router.route(request.method, request.path);
        if (!route.containsKey("action")) {
            // No route found to handle this websocket connection
            ctx.getChannel().write(new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_FOUND));
            return;
        }

        // Upgrade the connection and send the handshake response.
        ChannelPipeline p = ctx.getChannel().getPipeline();
        p.remove("aggregator");
        p.replace("decoder", "wsdecoder", new WebSocketFrameDecoder());

        // Connect
        ctx.getChannel().write(res);

        p.replace("encoder", "wsencoder", new WebSocketFrameEncoder());
        req.setMethod(new HttpMethod("WEBSOCKET"));

        // Inbound
        Http.Inbound inbound = new Http.Inbound() {

            @Override
            public boolean isOpen() {
                return ctx.getChannel().isOpen();
            }
        };
        channels.put(ctx, inbound);

        // Outbound
        Http.Outbound outbound = new Http.Outbound() {

            final List<ChannelFuture> writeFutures = Collections.synchronizedList(new ArrayList<ChannelFuture>());
            Promise<Void> closeTask;

            synchronized void writeAndClose(ChannelFuture writeFuture) {
                if (!writeFuture.isDone()) {
                    writeFutures.add(writeFuture);
                    writeFuture.addListener(new ChannelFutureListener() {

                        public void operationComplete(ChannelFuture cf) throws Exception {
                            writeFutures.remove(cf);
                            futureClose();
                        }
                    });
                }
            }

            void futureClose() {
                if (closeTask != null && writeFutures.isEmpty()) {
                    closeTask.invoke(null);
                }
            }

            @Override
            public void send(String data) {
                if (!isOpen()) {
                    throw new IllegalStateException("The outbound channel is closed");
                }
                writeAndClose(ctx.getChannel().write(new DefaultWebSocketFrame(data)));
            }

            @Override
            public void send(byte opcode, byte[] data, int offset, int length) {
                if (!isOpen()) {
                    throw new IllegalStateException("The outbound channel is closed");
                }
                writeAndClose(ctx.getChannel().write(new DefaultWebSocketFrame(opcode, wrappedBuffer(data, offset, length))));
            }

            @Override
            public synchronized boolean isOpen() {
                return ctx.getChannel().isOpen() && closeTask == null;
            }

            @Override
            public synchronized void close() {
                closeTask = new Promise<Void>();
                closeTask.onRedeem(new Action<Promise<Void>>() {

                    public void invoke(Promise<Void> completed) {
                        writeFutures.clear();
                        ctx.getChannel().disconnect();
                        closeTask = null;
                    }
                });
                futureClose();
            }
        };

        Invoker.invoke(new WebSocketInvocation(route, request, inbound, outbound, ctx, e));
    }

    @Override
    public void channelDisconnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        Http.Inbound inbound = channels.get(ctx);
        if (inbound != null) {
            inbound.close();
        }
        channels.remove(ctx);
    }

    public static class WebSocketInvocation extends Invoker.Invocation {

        Map<String, String> route;
        Http.Request request;
        Http.Inbound inbound;
        Http.Outbound outbound;
        ChannelHandlerContext ctx;
        MessageEvent e;

        public WebSocketInvocation(Map<String, String> route, Http.Request request, Http.Inbound inbound, Http.Outbound outbound, ChannelHandlerContext ctx, MessageEvent e) {
            this.route = route;
            this.request = request;
            this.inbound = inbound;
            this.outbound = outbound;
            this.ctx = ctx;
            this.e = e;
        }

        @Override
        public boolean init() {
            Http.Request.current.set(request);
            Http.Inbound.current.set(inbound);
            Http.Outbound.current.set(outbound);
            return super.init();
        }

        @Override
        public InvocationContext getInvocationContext() {
            WebSocketInvoker.resolve(request);
            return new InvocationContext(request.invokedMethod.getAnnotations(), request.invokedMethod.getDeclaringClass().getAnnotations());
        }

        @Override
        public void execute() throws Exception {
            WebSocketInvoker.invoke(request, inbound, outbound);
        }

        @Override
        public void onException(Throwable e) {
            Logger.error(e, "Internal Server Error in WebSocket (closing the socket) for request %s", request.method + " " + request.url);
            ctx.getChannel().close();
            super.onException(e);
        }

        @Override
        public void onSuccess() throws Exception {
            outbound.close();
            super.onSuccess();
        }
    }
}
