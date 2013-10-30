package play.server.ssl;

import javax.net.ssl.SSLEngine;

import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.handler.codec.http.HttpRequestDecoder;
import org.jboss.netty.handler.codec.http.HttpResponseEncoder;
import org.jboss.netty.handler.ssl.SslHandler;
import org.jboss.netty.handler.stream.ChunkedWriteHandler;

import play.Play;
import play.server.FlashPolicyHandler;
import play.server.StreamChunkAggregator;
import play.server.HttpServerPipelineFactory;
import org.jboss.netty.channel.ChannelHandler;
import play.Logger;
import play.server.Server;

import static org.jboss.netty.channel.Channels.pipeline;


public class SslHttpServerPipelineFactory extends HttpServerPipelineFactory {

    private String pipelineConfig = Play.configuration.getProperty("play.ssl.netty.pipeline", "play.server.FlashPolicyHandler,org.jboss.netty.handler.codec.http.HttpRequestDecoder,play.server.StreamChunkAggregator,org.jboss.netty.handler.codec.http.HttpResponseEncoder,org.jboss.netty.handler.stream.ChunkedWriteHandler,play.server.ssl.SslPlayHandler");

    public ChannelPipeline getPipeline() throws Exception {

        String mode = Play.configuration.getProperty("play.netty.clientAuth", "none");
        String enabledCiphers = Play.configuration.getProperty("play.ssl.enabledCiphers", "");

        ChannelPipeline pipeline = pipeline();

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
        
        engine.setEnableSessionCreation(true);

        pipeline.addLast("ssl", new SslHandler(engine));
        
        // Get all the pipeline. Give the user the opportunity to add their own
        String[] handlers = pipelineConfig.split(",");
        for (int i = 0; i < handlers.length - 1; i++) {
            String handler = handlers[i];
            try {
                String name = getName(handler.trim());
                ChannelHandler instance = getInstance(handler);
                if (instance != null) {
                    pipeline.addLast(name, instance); 
                    Server.pipelines.put("Ssl" + name, instance);
                }
            } catch(Throwable e) {
                Logger.error(" error adding " + handler, e);
            }

        }

        // The last one is always the play handler
        String handler = handlers[handlers.length - 1];
        ChannelHandler instance = getInstance(handler);
        if (instance != null) {
            pipeline.addLast("handler", instance); 
            Server.pipelines.put("SslHandler", instance);
        }

        return pipeline;
    }
}

