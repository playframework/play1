    package play.server;

import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.handler.codec.http.HttpChunkAggregator;
import org.jboss.netty.handler.codec.http.HttpRequestDecoder;
import org.jboss.netty.handler.codec.http.HttpResponseEncoder;
import org.jboss.netty.handler.codec.http.HttpContentCompressor;
import play.Play;

import static org.jboss.netty.channel.Channels.pipeline;

public class HttpServerPipelineFactory implements ChannelPipelineFactory {

    public ChannelPipeline getPipeline() throws Exception {

        Integer max = Integer.valueOf(Play.configuration.getProperty("play.netty.maxContentLength", "-1"));
        boolean useGzipCompression = "true".equals(Play.configuration.getProperty("play.netty.responseCompression", "false"));
           
        ChannelPipeline pipeline = pipeline();
        PlayHandler playHandler = new PlayHandler();
        
        pipeline.addLast("flashPolicy", new FlashPolicyHandler()); 
        pipeline.addLast("decoder", new HttpRequestDecoder());
        pipeline.addLast("aggregator", new StreamChunkAggregator(max));
        pipeline.addLast("encoder", new HttpResponseEncoder());
        // Compress
        if (useGzipCompression) {
        	pipeline.addLast("deflater", new HttpContentCompressor(1));
        }
        pipeline.addLast("chunkedWriter", playHandler.chunkedWriteHandler);
        pipeline.addLast("handler", playHandler);

        return pipeline;
    }
}

