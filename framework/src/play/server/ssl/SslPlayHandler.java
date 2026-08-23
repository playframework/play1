package play.server.ssl;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;
import play.Logger;
import play.mvc.Http.Request;
import play.server.PlayHandler;
import play.server.Server;

import javax.net.ssl.SSLException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

import static io.netty.handler.codec.http.HttpHeaderNames.LOCATION;

public class SslPlayHandler extends PlayHandler {

    private static final AttributeKey<InetSocketAddress> REMOTE_ADDRESS = AttributeKey.valueOf("ssl.remoteAddress");

    @Override
    public Request parseRequest(ChannelHandlerContext ctx, HttpRequest nettyRequest) throws Exception {
        Request request = super.parseRequest(ctx, nettyRequest);
        request.secure = true;
        return request;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        SocketAddress remote = ctx.channel().remoteAddress();
        if (remote instanceof InetSocketAddress) {
            ctx.channel().attr(REMOTE_ADDRESS).set((InetSocketAddress) remote);
        }
        // Get the SslHandler in the current pipeline.
        SslHandler sslHandler = ctx.pipeline().get(SslHandler.class);
        // Get notified when SSL handshake is done.
        sslHandler.handshakeFuture().addListener(new SslListener());
    }

    private static final class SslListener implements FutureListener<Channel> {

        @Override
        public void operationComplete(Future<Channel> future) {
            if (!future.isSuccess()) {
                Logger.debug(future.cause(), "Invalid certificate");
            }
        }

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        // We have to redirect to https://, as it was targeting http://
        // Redirect to the root as we don't know the url at that point
        if (cause instanceof SSLException) {
            Logger.debug(cause, "");
            InetSocketAddress inet = ctx.channel().attr(REMOTE_ADDRESS).get();
            if (inet == null && ctx.channel().remoteAddress() instanceof InetSocketAddress) {
                inet = (InetSocketAddress) ctx.channel().remoteAddress();
            }
            ctx.pipeline().remove("ssl");
            FullHttpResponse nettyResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.TEMPORARY_REDIRECT);
            nettyResponse.headers().set(LOCATION, "https://" + inet.getHostName() + ":" + Server.httpsPort + "/");
            ChannelFuture writeFuture = ctx.channel().writeAndFlush(nettyResponse);
            writeFuture.addListener(ChannelFutureListener.CLOSE);
        } else {
            Logger.error(cause, "");
            ctx.channel().close();
        }
    }

}