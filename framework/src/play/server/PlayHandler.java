package play.server;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.TooLongFrameException;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpChunkedInput;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpMessage;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import io.netty.handler.codec.http.cookie.ServerCookieEncoder;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import io.netty.handler.stream.ChunkedInput;
import io.netty.handler.stream.ChunkedStream;
import play.Invoker;
import play.Invoker.InvocationContext;
import play.Logger;
import play.Play;
import play.data.binding.CachedBoundActionMethodArgs;
import play.data.validation.Validation;
import play.exceptions.PlayException;
import play.exceptions.UnexpectedException;
import play.i18n.Messages;
import play.libs.F.Promise;
import play.libs.MimeTypes;
import play.mvc.*;
import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.mvc.results.NotFound;
import play.mvc.results.RenderStatic;
import play.templates.JavaExtensions;
import play.templates.TemplateLoader;
import play.utils.HTTP;
import play.utils.Utils;
import play.vfs.VirtualFile;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;
import java.util.stream.Stream;


import static io.netty.buffer.Unpooled.wrappedBuffer;
import static io.netty.handler.codec.http.HttpHeaderNames.*;

public class PlayHandler extends SimpleChannelInboundHandler<Object> {
    
    
    private static final String X_HTTP_METHOD_OVERRIDE = "X-HTTP-Method-Override";

    /**
     * If true (the default), Play will send the HTTP header "Server: Play!
     * Framework; ....". This could be a security problem (old versions having
     * publicly known security bugs), so you can disable the header in
     * application.conf: <code>http.exposePlayServer = false</code>
     */
    private static final String signature = "Play! Framework;" + Play.version + ";" + Play.mode.name().toLowerCase();
    private static final boolean exposePlayServer;

    /**
     * The Pipeline is given for a PlayHandler
     */
    public final Map<String, ChannelHandler> pipelines = new HashMap<>();

    private WebSocketServerHandshaker handshaker;
    
    /**
     * Define allowed methods that will be handled when defined in X-HTTP-Method-Override
     * You can define allowed method in
     * application.conf: <code>http.allowed.method.override=POST,PUT</code>
     */
    private static final Set<String> allowedHttpMethodOverride;

    static {
        exposePlayServer = !"false".equals(Play.configuration.getProperty("http.exposePlayServer"));
        allowedHttpMethodOverride = Stream.of(Play.configuration.getProperty("http.allowed.method.override", "").split(",")).collect(Collectors.toSet());
    }

    @Override
    public void channelRead0(final ChannelHandlerContext ctx, Object msg) throws Exception {
        if (Logger.isTraceEnabled()) {
            Logger.trace("messageReceived: begin");
        }

        // Http request
        if (msg instanceof HttpRequest nettyRequest) {

            // Websocket upgrade
            if (HttpHeaderValues.WEBSOCKET.toString().equalsIgnoreCase(nettyRequest.headers().get(HttpHeaderNames.UPGRADE))) {
                websocketHandshake(ctx, nettyRequest);
                return;
            }

            // Plain old HttpRequest
            try {
                // Reset request object and response object for the current
                // thread.
                Http.Request.current.set(new Http.Request());

                final Response response = new Response();
                Http.Response.current.set(response);

                final Request request = parseRequest(ctx, nettyRequest);

                // Buffered in memory output
                response.out = new ByteArrayOutputStream();

                // Direct output (will be set later)
                response.direct = null;

                // Streamed output (using response.writeChunk)
                response.onWriteChunk(result -> writeChunk(request, response, ctx, nettyRequest, result));

                // Raw invocation
                boolean raw = Play.pluginCollection.rawInvocation(request, response);
                if (raw) {
                    copyResponse(ctx, request, response, nettyRequest);
                } else {

                    // Delegate to Play framework
                    Invoker.invoke(new NettyInvocation(request, response, ctx, nettyRequest));

                }

            } catch (Exception ex) {
                Logger.warn(ex, "Exception on request. serving 500 back");
                serve500(ex, ctx, nettyRequest);
            }
        }

        // Websocket frame
        if (msg instanceof WebSocketFrame frame) {
            websocketFrameReceived(ctx, frame);
        }

        if (Logger.isTraceEnabled()) {
            Logger.trace("messageReceived: end");
        }
    }

    private static final Map<String, RenderStatic> staticPathsCache = new HashMap<>();

    public class NettyInvocation extends Invoker.Invocation {

        private final ChannelHandlerContext ctx;
        private final Request request;
        private final Response response;
        private final HttpRequest nettyRequest;

        public NettyInvocation(Request request, Response response, ChannelHandlerContext ctx, HttpRequest nettyRequest) {
            this.ctx = ctx;
            this.request = request;
            this.response = response;
            this.nettyRequest = nettyRequest;
        }

        @Override
        public boolean init() {
            Thread.currentThread().setContextClassLoader(Play.classloader);
            if (Logger.isTraceEnabled()) {
                Logger.trace("init: begin");
            }

            Request.current.set(request);
            Response.current.set(response);

            Scope.Params.current.set(request.params);
            Scope.RenderArgs.current.remove();
            Scope.RouteArgs.current.remove();
            Scope.Session.current.remove();
            Scope.Flash.current.remove();
            CachedBoundActionMethodArgs.init();

            try {
                if (Play.mode == Play.Mode.DEV) {
                    Router.detectChanges(Play.ctxPath);
                }
                if (Play.mode == Play.Mode.PROD
                        && staticPathsCache.containsKey(request.domain + " " + request.method + " " + request.path)) {
                    RenderStatic rs = null;
                    synchronized (staticPathsCache) {
                        rs = staticPathsCache.get(request.domain + " " + request.method + " " + request.path);
                    }
                    serveStatic(rs, ctx, request, response, nettyRequest);
                    if (Logger.isTraceEnabled()) {
                        Logger.trace("init: end false");
                    }
                    return false;
                }
                Router.routeOnlyStatic(request);
                super.init();
            } catch (NotFound nf) {
                serve404(nf, ctx, request, nettyRequest);
                if (Logger.isTraceEnabled()) {
                    Logger.trace("init: end false");
                }
                return false;
            } catch (RenderStatic rs) {
                if (Play.mode == Play.Mode.PROD) {
                    synchronized (staticPathsCache) {
                        staticPathsCache.put(request.domain + " " + request.method + " " + request.path, rs);
                    }
                }
                serveStatic(rs, ctx, request, response, nettyRequest);
                if (Logger.isTraceEnabled()) {
                    Logger.trace("init: end false");
                }
                return false;
            }

            if (Logger.isTraceEnabled()) {
                Logger.trace("init: end true");
            }
            return true;
        }

        @Override
        public InvocationContext getInvocationContext() {
            ActionInvoker.resolve(request);
            return new InvocationContext(Http.invocationType, request.invokedMethod.getAnnotations(),
                    request.invokedMethod.getDeclaringClass().getAnnotations());
        }

        @Override
        public void run() {
            try {
                if (Logger.isTraceEnabled()) {
                    Logger.trace("run: begin");
                }
                super.run();
            } catch (Exception e) {
                serve500(e, ctx, nettyRequest);
            }
            if (Logger.isTraceEnabled()) {
                Logger.trace("run: end");
            }
        }

        @Override
        public void execute() throws Exception {
            if (!ctx.channel().isActive()) {
                try {
                    ctx.channel().close();
                } catch (Throwable e) {
                    // Ignore
                }
                return;
            }

            // Check the exceeded size before re rendering so we can render the
            // error if the size is exceeded
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
            if (Logger.isTraceEnabled()) {
                Logger.trace("execute: end");
            }
        }
    }

    void saveExceededSizeError(HttpRequest nettyRequest, Request request, Response response) {

        String warning = nettyRequest.headers().get(HttpHeaderNames.WARNING);
        String length = nettyRequest.headers().get(HttpHeaderNames.CONTENT_LENGTH);
        if (warning != null) {
            if (Logger.isTraceEnabled()) {
                Logger.trace("saveExceededSizeError: begin");
            }

            try {
                StringBuilder error = new StringBuilder();
                error.append("\u0000");
                // Cannot put warning which is
                // play.netty.content.length.exceeded
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
                if (request.cookies.get(Scope.COOKIE_PREFIX + "_ERRORS") != null
                        && request.cookies.get(Scope.COOKIE_PREFIX + "_ERRORS").value != null) {
                    error.append(request.cookies.get(Scope.COOKIE_PREFIX + "_ERRORS").value);
                }
                String errorData = URLEncoder.encode(error.toString(), StandardCharsets.UTF_8);
                Http.Cookie c = new Http.Cookie();
                c.value = errorData;
                c.name = Scope.COOKIE_PREFIX + "_ERRORS";
                request.cookies.put(Scope.COOKIE_PREFIX + "_ERRORS", c);
                if (Logger.isTraceEnabled()) {
                    Logger.trace("saveExceededSizeError: end");
                }
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
                nettyResponse.headers().add(entry.getKey(), value);
            }
        }

        nettyResponse.headers().set(DATE, Utils.getHttpDateFormatter().format(new Date()));

        Map<String, Http.Cookie> cookies = response.cookies;

        for (Http.Cookie cookie : cookies.values()) {
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
            nettyResponse.headers().add(SET_COOKIE, ServerCookieEncoder.STRICT.encode(c));
        }

        if (!response.headers.containsKey(CACHE_CONTROL) && !response.headers.containsKey(EXPIRES)
                && !(response.direct instanceof File)) {
            nettyResponse.headers().set(CACHE_CONTROL, "no-cache");
        }

    }

    protected static void writeResponse(ChannelHandlerContext ctx, Response response, FullHttpResponse nettyResponse,
            HttpRequest nettyRequest) {
        if (Logger.isTraceEnabled()) {
            Logger.trace("writeResponse: begin");
        }

        byte[] content = null;

        boolean keepAlive = isKeepAlive(nettyRequest);
        if (nettyRequest.method().equals(HttpMethod.HEAD)) {
            content = new byte[0];
        } else {
            content = response.out.toByteArray();
        }

        nettyResponse.content().writeBytes(content);

        if (!nettyResponse.status().equals(HttpResponseStatus.NOT_MODIFIED)) {
            if (Logger.isTraceEnabled()) {
                Logger.trace("writeResponse: content length [" + response.out.size() + "]");
            }
            setContentLength(nettyResponse, response.out.size());
        }

        ChannelFuture f = null;
        if (ctx.channel().isActive()) {
            f = ctx.channel().writeAndFlush(nettyResponse);
        } else {
            Logger.debug("Try to write on a closed channel[keepAlive:%s]: Remote host may have closed the connection",
                    String.valueOf(keepAlive));
        }

        // Decide whether to close the connection or not.
        if (f != null && !keepAlive) {
            // Close the connection when the whole content is written out.
            f.addListener(ChannelFutureListener.CLOSE);
        }
        if (Logger.isTraceEnabled()) {
            Logger.trace("writeResponse: end");
        }
    }

    public void copyResponse(ChannelHandlerContext ctx, Request request, Response response, HttpRequest nettyRequest)
            throws Exception {
        if (Logger.isTraceEnabled()) {
            Logger.trace("copyResponse: begin");
        }

        // Decide whether to close the connection or not.

        FullHttpResponse nettyResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
                HttpResponseStatus.valueOf(response.status));
        if (exposePlayServer) {
            nettyResponse.headers().set(SERVER, signature);
        }

        if (response.contentType != null) {
            nettyResponse.headers()
                    .set(CONTENT_TYPE,
                            response.contentType + (response.contentType.startsWith("text/")
                                    && !response.contentType.contains("charset") ? "; charset=" + response.encoding
                                            : ""));
        } else {
            nettyResponse.headers().set(CONTENT_TYPE, "text/plain; charset=" + response.encoding);
        }

        addToResponse(response, nettyResponse);

        Object obj = response.direct;
        File file = null;
        ChunkedInput<ByteBuf> stream = null;
        InputStream is = null;
        if (obj instanceof File) {
            file = (File) obj;
        } else if (obj instanceof InputStream) {
            is = (InputStream) obj;
        } else if (obj instanceof ChunkedInput) {
            // Streaming we don't know the content length
            stream = (ChunkedInput<ByteBuf>) obj;
        }

        boolean keepAlive = isKeepAlive(nettyRequest);
        if (file != null && file.isFile()) {
            addEtag(nettyRequest, nettyResponse, file);
            if (nettyResponse.status().equals(HttpResponseStatus.NOT_MODIFIED)) {

                Channel ch = ctx.channel();

                // Write the initial line and the header.
                ChannelFuture writeFuture = ch.writeAndFlush(nettyResponse);

                if (!keepAlive) {
                    // Close the connection when the whole content is
                    // written out.
                    writeFuture.addListener(ChannelFutureListener.CLOSE);
                }
            } else {
                FileService.serve(file, nettyRequest, nettyResponse, ctx, request, response, ctx.channel());
            }
        } else if (is != null) {
            ChannelFuture writeFuture = ctx.channel().writeAndFlush(nettyResponse);
            if (!nettyRequest.method().equals(HttpMethod.HEAD)
                    && !nettyResponse.status().equals(HttpResponseStatus.NOT_MODIFIED)) {
                writeFuture = ctx.channel().writeAndFlush(new ChunkedStream(is));
            } else {
                is.close();
            }
            if (!keepAlive) {
                writeFuture.addListener(ChannelFutureListener.CLOSE);
            }
        } else if (stream != null) {
            // Streaming response (response.writeChunk). The chunks are already
            // framed as HTTP chunks, so write the headers as a plain
            // HttpMessage to avoid the encoder emitting a premature last chunk.
            HttpResponse headerResponse = new DefaultHttpResponse(nettyResponse.protocolVersion(),
                    nettyResponse.status());
            headerResponse.headers().set(nettyResponse.headers());
            ChannelFuture writeFuture = ctx.channel().writeAndFlush(headerResponse);
            if (!nettyRequest.method().equals(HttpMethod.HEAD)
                    && !nettyResponse.status().equals(HttpResponseStatus.NOT_MODIFIED)) {
                // HttpChunkedInput emits the terminating LastHttpContent so the
                // response is properly closed and the connection can be reused.
                writeFuture = ctx.channel().writeAndFlush(new HttpChunkedInput(stream));
            } else {
                stream.close();
            }
            if (!keepAlive) {
                writeFuture.addListener(ChannelFutureListener.CLOSE);
            }
        } else {
            writeResponse(ctx, response, nettyResponse, nettyRequest);
        }
        if (Logger.isTraceEnabled()) {
            Logger.trace("copyResponse: end");
        }
    }

    static String getRemoteIPAddress(InetSocketAddress inet) {
        String fullAddress = inet.getAddress().getHostAddress();
        if (fullAddress.matches("/[0-9]+[.][0-9]+[.][0-9]+[.][0-9]+[:][0-9]+")) {
            fullAddress = fullAddress.substring(1);
            fullAddress = fullAddress.substring(0, fullAddress.indexOf(':'));
        } else if (fullAddress.matches(".*[%].*")) {
            fullAddress = fullAddress.substring(0, fullAddress.indexOf('%'));
        }
        return fullAddress;
    }

    public Request parseRequest(ChannelHandlerContext ctx, HttpRequest nettyRequest)
            throws Exception {
        if (Logger.isTraceEnabled()) {
            Logger.trace("parseRequest: begin");
            Logger.trace("parseRequest: URI = " + nettyRequest.uri());
        }

        String uri = nettyRequest.uri();
        // Remove domain and port from URI if it's present.
        if (uri.startsWith("http://") || uri.startsWith("https://")) {
            // Begins searching / after 9th character (last / of https://)
            int index = uri.indexOf("/", 9);
            // prevent the IndexOutOfBoundsException that was occurring
            if (index >= 0) {
                uri = uri.substring(index);
            } else {
                uri = "/";
            }
        }

        String contentType = nettyRequest.headers().get(CONTENT_TYPE);

        // need to get the encoding now - before the Http.Request is created
        String encoding = Play.defaultWebEncoding;
        if (contentType != null) {
            HTTP.ContentTypeWithEncoding contentTypeEncoding = HTTP.parseContentType(contentType);
            if (contentTypeEncoding.encoding != null) {
                encoding = contentTypeEncoding.encoding;
            }
        }

        int i = uri.indexOf('?');
        String querystring = "";
        String path = uri;
        if (i != -1) {
            path = uri.substring(0, i);
            querystring = uri.substring(i + 1);
        }

        InetSocketAddress inet = (InetSocketAddress) ctx.channel().remoteAddress();
        String remoteAddress = inet == null ? "" : getRemoteIPAddress(inet);
        String method = nettyRequest.method().name();

        if (nettyRequest.headers().get(X_HTTP_METHOD_OVERRIDE) != null
                && allowedHttpMethodOverride.contains(nettyRequest.headers().get(X_HTTP_METHOD_OVERRIDE).intern())) {
            method = nettyRequest.headers().get(X_HTTP_METHOD_OVERRIDE).intern();
        }

        InputStream body = null;
        ByteBuf b = nettyRequest instanceof FullHttpRequest ? ((FullHttpRequest) nettyRequest).content()
                : Unpooled.EMPTY_BUFFER;
        if (b instanceof FileChannelBuffer buffer) {
            // An error occurred
            Integer max = Integer.valueOf(Play.configuration.getProperty("play.netty.maxContentLength", "-1"));

            body = buffer.getInputStream();
            if (!(max == -1 || body.available() < max)) {
                body = new ByteArrayInputStream(new byte[0]);
            }

        } else {
            body = new ByteArrayInputStream(new ByteBufInputStream(b).readAllBytes());
        }

        String host = nettyRequest.headers().get(HOST);
        boolean isLoopback = false;
        try {
            isLoopback = inet != null && inet.getAddress().isLoopbackAddress()
                    && host != null && host.matches("^127\\.0\\.0\\.1:?[0-9]*$");
        } catch (Exception e) {
            // ignore it
        }

        int port = 0;
        String domain = null;
        if (host == null) {
            host = "";
            port = 80;
            domain = "";
        }
        // Check for IPv6 address
        else if (host.startsWith("[")) {
            // There is no port
            if (host.endsWith("]")) {
                domain = host;
                port = 80;
            } else {
                // There is a port so take from the last colon
                int portStart = host.lastIndexOf(':');
                if (portStart > 0 && (portStart + 1) < host.length()) {
                    domain = host.substring(0, portStart);
                    port = Integer.parseInt(host.substring(portStart + 1));
                }
            }
        }
        // Non IPv6 but has port
        else if (host.contains(":")) {
            String[] hosts = host.split(":");
            port = Integer.parseInt(hosts[1]);
            domain = hosts[0];
        } else {
            port = 80;
            domain = host;
        }

        boolean secure = false;

        Request request = Request.createRequest(remoteAddress, method, path, querystring, contentType, body, uri, host,
                isLoopback, port, domain, secure, getHeaders(nettyRequest), getCookies(nettyRequest));

        if (Logger.isTraceEnabled()) {
            Logger.trace("parseRequest: end");
        }
        return request;
    }

    protected static Map<String, Http.Header> getHeaders(HttpRequest nettyRequest) {
        Map<String, Http.Header> headers = new HashMap<>(16);

        for (String key : nettyRequest.headers().names()) {
            Http.Header hd = new Http.Header();
            hd.name = key.toLowerCase();
            hd.values = new ArrayList<>(nettyRequest.headers().getAll(key));
            headers.put(hd.name, hd);
        }

        return headers;
    }

    protected static Map<String, Http.Cookie> getCookies(HttpRequest nettyRequest) {
        Map<String, Http.Cookie> cookies = new HashMap<>(16);
        String value = nettyRequest.headers().get(COOKIE);
        if (value != null) {
            Set<Cookie> cookieSet = ServerCookieDecoder.STRICT.decode(value);
            if (cookieSet != null) {
                for (Cookie cookie : cookieSet) {
                    Http.Cookie playCookie = new Http.Cookie();
                    playCookie.name = cookie.name();
                    playCookie.path = cookie.path();
                    playCookie.domain = cookie.domain();
                    playCookie.secure = cookie.isSecure();
                    playCookie.value = cookie.value();
                    playCookie.httpOnly = cookie.isHttpOnly();
                    cookies.put(playCookie.name, playCookie);
                }
            }
        }
        return cookies;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        try {
            // If we get a TooLongFrameException, we got a request exceeding 8k.
            // Log this, we can't call serve500()
            if (cause instanceof TooLongFrameException) {
                Logger.error("Request exceeds 8192 bytes");
            }
            ctx.channel().close();
        } catch (Exception ex) {
        }
    }

    public static void serve404(NotFound e, ChannelHandlerContext ctx, Request request, HttpRequest nettyRequest) {
        if (Logger.isTraceEnabled()) {
            Logger.trace("serve404: begin");
        }
        FullHttpResponse nettyResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_FOUND);
        if (exposePlayServer) {
            nettyResponse.headers().set(SERVER, signature);
        }

        nettyResponse.headers().set(CONTENT_TYPE, "text/html");
        Map<String, Object> binding = getBindingForErrors(e, false);

        String format = Request.current().format;
        if (format == null) {
            format = "txt";
        }
        nettyResponse.headers().set(CONTENT_TYPE, (MimeTypes.getContentType("404." + format, "text/plain")));

        String errorHtml = TemplateLoader.load("errors/404." + format).render(binding);
        try {
            byte[] bytes = errorHtml.getBytes(Response.current().encoding);
            setContentLength(nettyResponse, bytes.length);
            nettyResponse.content().writeBytes(bytes);
            ChannelFuture writeFuture = ctx.channel().writeAndFlush(nettyResponse);
            writeFuture.addListener(ChannelFutureListener.CLOSE);
        } catch (UnsupportedEncodingException fex) {
            Logger.error(fex, "(encoding ?)");
        }
        if (Logger.isTraceEnabled()) {
            Logger.trace("serve404: end");
        }
    }

    protected static Map<String, Object> getBindingForErrors(Exception e, boolean isError) {

        Map<String, Object> binding = new HashMap<>();
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
            // Logger.error(ex, "Error when getting Validation errors");
        }

        return binding;
    }

    // TODO: add request and response as parameter
    public static void serve500(Exception e, ChannelHandlerContext ctx, HttpRequest nettyRequest) {
        if (Logger.isTraceEnabled()) {
            Logger.trace("serve500: begin");
        }

        FullHttpResponse nettyResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
                HttpResponseStatus.INTERNAL_SERVER_ERROR);
        if (exposePlayServer) {
            nettyResponse.headers().set(SERVER, signature);
        }

        Request request = Request.current();
        Response response = Response.current();

        String encoding = response.encoding;

        try {
            if (!(e instanceof PlayException)) {
                e = new play.exceptions.UnexpectedException(e);
            }

            // Flush some cookies
            try {

                Map<String, Http.Cookie> cookies = response.cookies;
                for (Http.Cookie cookie : cookies.values()) {
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

                    nettyResponse.headers().add(SET_COOKIE, ServerCookieEncoder.STRICT.encode(c));
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

            nettyResponse.headers().set("Content-Type", (MimeTypes.getContentType("500." + format, "text/plain")));
            try {
                String errorHtml = TemplateLoader.load("errors/500." + format).render(binding);

                byte[] bytes = errorHtml.getBytes(encoding);
                setContentLength(nettyResponse, bytes.length);
                nettyResponse.content().writeBytes(bytes);
                ChannelFuture writeFuture = ctx.channel().writeAndFlush(nettyResponse);
                writeFuture.addListener(ChannelFutureListener.CLOSE);
                Logger.error(e, "Internal Server Error (500) for request %s", request.method + " " + request.url);
            } catch (Throwable ex) {
                Logger.error(e, "Internal Server Error (500) for request %s", request.method + " " + request.url);
                Logger.error(ex, "Error during the 500 response generation");
                try {
                    String errorHtml = "Internal Error (check logs)";
                    byte[] bytes = errorHtml.getBytes(encoding);
                    setContentLength(nettyResponse, bytes.length);
                    nettyResponse.content().writeBytes(bytes);
                    ChannelFuture writeFuture = ctx.channel().writeAndFlush(nettyResponse);
                    writeFuture.addListener(ChannelFutureListener.CLOSE);
                } catch (UnsupportedEncodingException fex) {
                    Logger.error(fex, "(encoding ?)");
                }
            }
        } catch (Throwable exxx) {
            try {
                String errorHtml = "Internal Error (check logs)";
                byte[] bytes = errorHtml.getBytes(encoding);
                setContentLength(nettyResponse, bytes.length);
                nettyResponse.content().writeBytes(bytes);
                ChannelFuture writeFuture = ctx.channel().writeAndFlush(nettyResponse);
                writeFuture.addListener(ChannelFutureListener.CLOSE);
            } catch (Exception fex) {
                Logger.error(fex, "(encoding ?)");
            }
            if (exxx instanceof RuntimeException) {
                throw (RuntimeException) exxx;
            }
            throw new RuntimeException(exxx);
        }
        if (Logger.isTraceEnabled()) {
            Logger.trace("serve500: end");
        }
    }

    public void serveStatic(RenderStatic renderStatic, ChannelHandlerContext ctx, Request request, Response response,
            HttpRequest nettyRequest) {
        if (Logger.isTraceEnabled()) {
            Logger.trace("serveStatic: begin");
        }

        FullHttpResponse nettyResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
                HttpResponseStatus.valueOf(response.status));
        if (exposePlayServer) {
            nettyResponse.headers().set(SERVER, signature);
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
                    File localFile = file.getRealFile();
                    boolean keepAlive = isKeepAlive(nettyRequest);
                    addEtag(nettyRequest, nettyResponse, localFile);

                    if (nettyResponse.status().equals(HttpResponseStatus.NOT_MODIFIED)) {
                        Channel ch = ctx.channel();

                        // Write the initial line and the header.
                        ChannelFuture writeFuture = ch.writeAndFlush(nettyResponse);
                        if (!keepAlive) {
                            // Write the content.
                            writeFuture.addListener(ChannelFutureListener.CLOSE);
                        }
                    } else {
                        FileService.serve(localFile, nettyRequest, nettyResponse, ctx, request, response,
                                ctx.channel());
                    }
                }

            }
        } catch (Throwable ez) {
            Logger.error(ez, "serveStatic for request %s", request.method + " " + request.url);
            try {
                FullHttpResponse errorResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
                        HttpResponseStatus.INTERNAL_SERVER_ERROR);
                String errorHtml = "Internal Error (check logs)";
                byte[] bytes = errorHtml.getBytes(response.encoding);
                setContentLength(errorResponse, bytes.length);
                errorResponse.content().writeBytes(bytes);
                ChannelFuture future = ctx.channel().writeAndFlush(errorResponse);
                future.addListener(ChannelFutureListener.CLOSE);
            } catch (Exception ex) {
                Logger.error(ex, "serveStatic for request %s", request.method + " " + request.url);
            }
        }
        if (Logger.isTraceEnabled()) {
            Logger.trace("serveStatic: end");
        }
    }

    public static boolean isModified(String etag, long last, HttpRequest nettyRequest) {
        String browserEtag = nettyRequest.headers().get(IF_NONE_MATCH);
        String ifModifiedSince = nettyRequest.headers().get(IF_MODIFIED_SINCE);
        return HTTP.isModified(etag, last, browserEtag, ifModifiedSince);
    }

    private static void addEtag(HttpRequest nettyRequest, FullHttpResponse httpResponse, File file) {
        if (Play.mode == Play.Mode.DEV) {
            httpResponse.headers().set(CACHE_CONTROL, "no-cache");
        } else {
            // Check if Cache-Control header is not set
            if (httpResponse.headers().get(CACHE_CONTROL) == null) {
                String maxAge = Play.configuration.getProperty("http.cacheControl", "3600");
                if (maxAge.equals("0")) {
                    httpResponse.headers().set(CACHE_CONTROL, "no-cache");
                } else {
                    httpResponse.headers().set(CACHE_CONTROL, "max-age=" + maxAge);
                }
            }
        }
        boolean useEtag = Play.configuration.getProperty("http.useETag", "true").equals("true");
        long last = file.lastModified();
        String etag = "\"" + last + "-" + file.hashCode() + "\"";
        if (!isModified(etag, last, nettyRequest)) {
            if (nettyRequest.method().equals(HttpMethod.GET)) {
                httpResponse.setStatus(HttpResponseStatus.NOT_MODIFIED);
            }
            if (useEtag) {
                httpResponse.headers().set(ETAG, etag);
            }

        } else {
            httpResponse.headers().set(LAST_MODIFIED, Utils.getHttpDateFormatter().format(new Date(last)));
            if (useEtag) {
                httpResponse.headers().set(ETAG, etag);
            }
        }
    }

    public static boolean isKeepAlive(HttpMessage message) {
        return HttpUtil.isKeepAlive(message) && message.protocolVersion().equals(HttpVersion.HTTP_1_1);
    }

    public static void setContentLength(HttpMessage message, long contentLength) {
        message.headers().set(HttpHeaderNames.CONTENT_LENGTH, String.valueOf(contentLength));
    }

    static class LazyChunkedInput implements ChunkedInput<ByteBuf> {

        private final ConcurrentLinkedQueue<ByteBuf> nextChunks = new ConcurrentLinkedQueue<>();
        private boolean closed = false;

        @Override
        public boolean isEndOfInput() throws Exception {
            return closed && nextChunks.isEmpty();
        }

        @Override
        public void close() throws Exception {
            closed = true;
        }

        @Override
        @SuppressWarnings("deprecation")
        public ByteBuf readChunk(ChannelHandlerContext ctx) throws Exception {
            return readChunk(ctx.alloc());
        }

        @Override
        public ByteBuf readChunk(ByteBufAllocator allocator) throws Exception {
            if (nextChunks.isEmpty()) {
                return null;
            }
            return nextChunks.poll();
        }

        @Override
        public long length() {
            return -1;
        }

        @Override
        public long progress() {
            return -1;
        }

        public void writeChunk(Object chunk) throws Exception {
            if (closed) {
                throw new Exception("HTTP output stream closed");
            }

            byte[] bytes;
            if (chunk instanceof byte[]) {
                bytes = (byte[]) chunk;
            } else {
                String message = chunk == null ? "" : chunk.toString();
                bytes = message.getBytes(Response.current().encoding);
            }

            nextChunks.offer(Unpooled.wrappedBuffer(bytes));
        }
    }

    public void writeChunk(Request playRequest, Response playResponse, ChannelHandlerContext ctx,
            HttpRequest nettyRequest, Object chunk) {
        try {
            if (playResponse.direct == null) {
                playResponse.setHeader("Transfer-Encoding", "chunked");
                playResponse.direct = new LazyChunkedInput();
                copyResponse(ctx, playRequest, playResponse, nettyRequest);
            }
            ((LazyChunkedInput) playResponse.direct).writeChunk(chunk);

            ctx.channel().flush();
        } catch (Exception e) {
            throw new UnexpectedException(e);
        }
    }

    public void closeChunked(Request playRequest, Response playResponse, ChannelHandlerContext ctx,
            HttpRequest nettyRequest) {
        try {
            ((LazyChunkedInput) playResponse.direct).close();
            ctx.channel().flush();
        } catch (Exception e) {
            throw new UnexpectedException(e);
        }
    }

    // ~~~~~~~~~~~ Websocket
    static final Map<ChannelHandlerContext, Http.Inbound> channels = new ConcurrentHashMap<>();

    private void websocketFrameReceived(ChannelHandlerContext ctx, WebSocketFrame webSocketFrame) {
        Http.Inbound inbound = channels.get(ctx);
        // Check for closing frame
        if (webSocketFrame instanceof CloseWebSocketFrame) {
            this.handshaker.close(ctx.channel(), (CloseWebSocketFrame) webSocketFrame);
        } else if (webSocketFrame instanceof PingWebSocketFrame) {
            ctx.channel().writeAndFlush(new PongWebSocketFrame(webSocketFrame.content().retain()));
        } else if (webSocketFrame instanceof BinaryWebSocketFrame) {
            inbound._received(new Http.WebSocketFrame(webSocketFrame.content().array()));
        } else if (webSocketFrame instanceof TextWebSocketFrame) {
            inbound._received(new Http.WebSocketFrame(((TextWebSocketFrame) webSocketFrame).text()));
        }
    }

    private String getWebSocketLocation(HttpRequest req) {
        return "ws://" + req.headers().get(HttpHeaderNames.HOST) + req.uri();
    }

    private void websocketHandshake(final ChannelHandlerContext ctx, HttpRequest req)
            throws Exception {

        Integer max = Integer.valueOf(Play.configuration.getProperty("play.netty.maxContentLength", "65345"));

        WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(
                this.getWebSocketLocation(req), null, false);
        this.handshaker = wsFactory.newHandshaker(req);
        if (this.handshaker == null) {
            wsFactory.sendUnsupportedVersionResponse(ctx.channel());
        } else {
            try {
                this.handshaker.handshake(ctx.channel(), req);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        Http.Request request = parseRequest(ctx, req);

        // Route the websocket request
        request.method = "WS";

        Map<String, String> route = Router.route(request.method, request.path);
        if (!route.containsKey("action")) {
            // No route found to handle this websocket connection
            ctx.channel().writeAndFlush(new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_FOUND));
            return;
        }

        // Inbound
        Http.Inbound inbound = new Http.Inbound(ctx) {

            @Override
            public boolean isOpen() {
                return ctx.channel().isOpen();
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
                    writeFuture.addListener(cf -> {
                        writeFutures.remove(cf);
                        futureClose();
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
                writeAndClose(ctx.channel().writeAndFlush(new TextWebSocketFrame(data)));
            }

            @Override
            public void send(byte opcode, byte[] data, int offset, int length) {
                if (!isOpen()) {
                    throw new IllegalStateException("The outbound channel is closed");
                }

                writeAndClose(ctx.channel().writeAndFlush(new BinaryWebSocketFrame(wrappedBuffer(data, offset, length))));
            }

            @Override
            public synchronized boolean isOpen() {
                return ctx.channel().isOpen() && closeTask == null;
            }

            @Override
            public synchronized void close() {
                closeTask = new Promise<>();
                closeTask.onRedeem(completed -> {
                    writeFutures.clear();
                    ctx.channel().disconnect();
                    closeTask = null;
                });
                futureClose();
            }
        };
        Logger.trace("invoking");

        Invoker.invoke(new WebSocketInvocation(route, request, inbound, outbound, ctx));
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        Http.Inbound inbound = channels.remove(ctx);
        if (inbound != null) {
            inbound.close();
        }
    }

    public static class WebSocketInvocation extends Invoker.Invocation {

        Map<String, String> route;
        Http.Request request;
        Http.Inbound inbound;
        Http.Outbound outbound;
        ChannelHandlerContext ctx;

        public WebSocketInvocation(Map<String, String> route, Http.Request request, Http.Inbound inbound,
                Http.Outbound outbound, ChannelHandlerContext ctx) {
            this.route = route;
            this.request = request;
            this.inbound = inbound;
            this.outbound = outbound;
            this.ctx = ctx;
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
            return new InvocationContext(Http.invocationType, request.invokedMethod.getAnnotations(),
                    request.invokedMethod.getDeclaringClass().getAnnotations());
        }

        @Override
        public void execute() throws Exception {
            WebSocketInvoker.invoke(request, inbound, outbound);
        }

        @Override
        public void onException(Throwable e) {
            Logger.error(e, "Internal Server Error in WebSocket (closing the socket) for request %s",
                    request.method + " " + request.url);
            ctx.channel().close();
            super.onException(e);
        }

        @Override
        public void onSuccess() throws Exception {
            outbound.close();
            super.onSuccess();
        }
    }
}