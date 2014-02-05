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
import play.server.PlayHandler;
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
        if(handlers.length <= 0){
            Logger.error("You must defined at least the SslPlayHandler in \"play.netty.pipeline\"");
            return pipeline;
        }
        
        // Create the play Handler (always the last one)
        String handler = handlers[handlers.length - 1];
        ChannelHandler instance = getInstance(handler);
        SslPlayHandler sslPlayHandler = (SslPlayHandler) instance;
        if (instance == null || !(instance instanceof SslPlayHandler) || sslPlayHandler == null) {
            Logger.error("The last handler must be the SslPlayHandler in \"play.netty.pipeline\"");
            return pipeline;
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
            } catch(Throwable e) {
                Logger.error(" error adding " + handler, e);
            }

        }

        if (sslPlayHandler != null) {
            pipeline.addLast("handler", sslPlayHandler);
            sslPlayHandler.pipelines.put("SslHandler", sslPlayHandler);
        } 
        
        return pipeline;
    }
}

