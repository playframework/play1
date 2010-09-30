package play.server.ssl;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBufferInputStream;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.*;
import org.jboss.netty.handler.codec.http.*;
import org.jboss.netty.handler.ssl.SslHandler;
import org.jboss.netty.handler.stream.ChunkedFile;
import org.jboss.netty.handler.stream.ChunkedStream;
import play.Invoker;
import play.Logger;
import play.Play;
import play.PlayPlugin;
import play.data.validation.Validation;
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
import play.server.FileChannelBuffer;
import play.server.PlayHandler;
import play.server.Server;
import play.templates.JavaExtensions;
import play.templates.TemplateLoader;
import play.utils.Utils;
import play.vfs.VirtualFile;

import javax.net.ssl.SSLException;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.ParseException;
import java.util.*;

import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.*;

public class SslPlayHandler extends PlayHandler {

    public SslPlayHandler() {
        
    }

    @Override
    public void channelConnected(
            ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
            ctx.setAttachment(e.getValue());
            // Get the SslHandler in the current pipeline.
            final SslHandler sslHandler = ctx.getPipeline().get(SslHandler.class);
            sslHandler.setEnableRenegotiation(false);
            // Get notified when SSL handshake is done.
            ChannelFuture handshakeFuture = sslHandler.handshake();
            handshakeFuture.addListener(new SslListener(sslHandler));
    }

    private static final class SslListener implements ChannelFutureListener {

        private final SslHandler sslHandler;

        SslListener(SslHandler sslHandler) {
            this.sslHandler = sslHandler;
        }

        public void operationComplete(ChannelFuture future) throws Exception {
            if (!future.isSuccess()) {
                Logger.warn(future.getCause(), "Invalid certificate ");

                if (future.getChannel().isOpen()) {
                    future.getChannel().close();
                }
            }
        }
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e)
            throws Exception {
        Logger.error(e.getCause(), "");
        // We have to redirect to https://, as it was targeting http://
        // Redirect to the root as we don't know the url at that point
        if (e.getCause() instanceof SSLException) {
            InetSocketAddress inet = ((InetSocketAddress) ctx.getAttachment());
            ctx.getPipeline().remove("ssl");
            HttpResponse nettyResponse = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.TEMPORARY_REDIRECT);
            nettyResponse.setHeader(LOCATION, "https://" + inet.getHostName() + ":" + Server.httpPort + "/");
            ChannelFuture writeFuture = ctx.getChannel().write(nettyResponse);
            writeFuture.addListener(ChannelFutureListener.CLOSE);
        } else {
            e.getChannel().close();
        }
    }

}
