package play.server.ssl;

import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.concurrent.Future;
import play.Logger;
import play.mvc.Http.Request;
import play.server.PlayHandler;
import play.server.Server;

import javax.net.ssl.SSLException;

import java.net.InetSocketAddress;

import static io.netty.handler.codec.http.HttpHeaders.Names.*;

public class SslPlayHandler extends PlayHandler {

    @Override
    public Request parseRequest(ChannelHandlerContext ctx, HttpRequest nettyRequest) throws Exception {
        Request request = super.parseRequest(ctx, nettyRequest);
        request.secure = true;
        return request;
    }

	@Override
    public void channelActive(ChannelHandlerContext ctx /*, ChannelStateEvent e */) throws Exception {
// Not sure what this does.
//        ctx.setAttachment(e.getValue());
		
        // Get the SslHandler in the current pipeline.
        final SslHandler sslHandler = ctx.pipeline().get(SslHandler.class);
        sslHandler.setEnableRenegotiation(false);
        // Get notified when SSL handshake is done.
        Future<Channel> handshakeFuture = sslHandler.handshakeFuture();
        handshakeFuture.addListener(new SslListener());
    }

    private static final class SslListener implements ChannelFutureListener {
        public void operationComplete(ChannelFuture future) throws Exception {
            if (!future.isSuccess()) {
                Logger.debug(future.cause(), "Invalid certificate");
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable e) throws Exception {
        // We have to redirect to https://, as it was targeting http://
        // Redirect to the root as we don't know the url at that point
        if (e.getCause() instanceof SSLException) {
            Logger.debug(e.getCause(), "");
            InetSocketAddress inet = ((InetSocketAddress) ctx.channel().localAddress());
            ctx.pipeline().remove("ssl");
            HttpResponse nettyResponse = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.TEMPORARY_REDIRECT);
            nettyResponse.headers().set(LOCATION, "https://" + inet.getHostName() + ":" + Server.httpsPort + "/");
            ChannelFuture writeFuture = ctx.channel().write(nettyResponse);
            writeFuture.addListener(ChannelFutureListener.CLOSE);
        } else {
            Logger.error(e.getCause(), "");
            ctx.channel().close();
        }
    }
}
