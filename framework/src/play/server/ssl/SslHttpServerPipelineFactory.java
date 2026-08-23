package play.server.ssl;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.ssl.SslHandler;

import javax.net.ssl.SSLEngine;

import play.Logger;
import play.Play;
import play.server.HttpServerPipelineFactory;

public class SslHttpServerPipelineFactory extends HttpServerPipelineFactory {

    private final String pipelineConfig = Play.configuration.getProperty("play.ssl.netty.pipeline",
            "play.server.FlashPolicyHandler,io.netty.handler.codec.http.HttpRequestDecoder,play.server.StreamChunkAggregator,io.netty.handler.codec.http.HttpResponseEncoder,io.netty.handler.stream.ChunkedWriteHandler,play.server.ssl.SslPlayHandler");

    @Override
    public void initChannel(Channel channel) throws Exception {

        String mode = Play.configuration.getProperty("play.netty.clientAuth", "none");
        String enabledCiphers = Play.configuration.getProperty("play.ssl.enabledCiphers", "");
        String enabledProtocols = Play.configuration.getProperty("play.ssl.enabledProtocols", "");

        ChannelPipeline pipeline = channel.pipeline();

        // Add SSL handler first to encrypt and decrypt everything.
        SSLEngine engine = SslHttpServerContextFactory.getServerContext().createSSLEngine();
        engine.setUseClientMode(false);

        if (enabledCiphers != null && enabledCiphers.length() > 0) {
            engine.setEnabledCipherSuites(enabledCiphers.replaceAll(" ", "").split(","));
        }

        if ("want".equalsIgnoreCase(mode)) {
            engine.setWantClientAuth(true);
        } else if ("need".equalsIgnoreCase(mode)) {
            engine.setNeedClientAuth(true);
        }

        if (enabledProtocols != null && enabledProtocols.trim().length() > 0) {
            engine.setEnabledProtocols(enabledProtocols.replaceAll(" ", "").split(","));
        }

        engine.setEnableSessionCreation(true);

        pipeline.addLast("ssl", new SslHandler(engine));

        // Get all the pipeline. Give the user the opportunity to add their own
        String[] handlers = pipelineConfig.split(",");
        if (handlers.length <= 0) {
            Logger.error("You must defined at least the SslPlayHandler in \"play.netty.pipeline\"");
            return;
        }

        // Create the play Handler (always the last one)
        String handler = handlers[handlers.length - 1];
        ChannelHandler instance = getInstance(handler);
        SslPlayHandler sslPlayHandler = (SslPlayHandler) instance;
        if (instance == null || !(instance instanceof SslPlayHandler) || sslPlayHandler == null) {
            Logger.error("The last handler must be the SslPlayHandler in \"play.netty.pipeline\"");
            return;
        }

        for (int i = 0; i < handlers.length - 1; i++) {
            handler = handlers[i];
            try {
                String name = getName(handler.trim());
                instance = getInstance(handler);
                if (instance != null) {
                    pipeline.addLast(name, instance);
                    sslPlayHandler.pipelines.put("Ssl" + name, instance);
                }
            } catch (Throwable e) {
                Logger.error(e, " error adding " + handler);
            }

        }

        if (sslPlayHandler != null) {
            pipeline.addLast("handler", sslPlayHandler);
            sslPlayHandler.pipelines.put("SslHandler", sslPlayHandler);
        }
    }
}