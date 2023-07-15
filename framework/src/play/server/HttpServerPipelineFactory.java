package play.server;

import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.ChannelHandler;
import play.Play;
import play.Logger;
import play.exceptions.UnexpectedException;

import java.util.Map;
import java.util.HashMap;

import static org.jboss.netty.channel.Channels.pipeline;

public class HttpServerPipelineFactory implements ChannelPipelineFactory {

    protected static final Map<String, Class<?>> classes = new HashMap<>();

    private final String pipelineConfig = Play.configuration.getProperty("play.netty.pipeline", "play.server.FlashPolicyHandler,org.jboss.netty.handler.codec.http.HttpRequestDecoder,play.server.StreamChunkAggregator,org.jboss.netty.handler.codec.http.HttpResponseEncoder,org.jboss.netty.handler.stream.ChunkedWriteHandler,play.server.PlayHandler");

    @Override
    public ChannelPipeline getPipeline() throws Exception {

        ChannelPipeline pipeline = pipeline();
        
        String[] handlers = pipelineConfig.split(",");  
        if(handlers.length <= 0){
            Logger.error("You must defined at least the playHandler in \"play.netty.pipeline\"");
            return pipeline;
        }       
        
        // Create the play Handler (always the last one)
        String handler = handlers[handlers.length - 1];
        ChannelHandler instance = getInstance(handler);
        PlayHandler playHandler = (PlayHandler) instance;
        if (playHandler == null) {
            Logger.error("The last handler must be the playHandler in \"play.netty.pipeline\"");
            return pipeline;
        }
      
        // Get all the pipeline. Give the user the opportunity to add their own
        for (int i = 0; i < handlers.length - 1; i++) {
            handler = handlers[i];
            try {
                String name = getName(handler.trim());
                instance = getInstance(handler);
                if (instance != null) {
                    pipeline.addLast(name, instance);
                    playHandler.pipelines.put(name, instance);
                }
            } catch (Throwable e) {
                Logger.error(" error adding " + handler, e);
            }
        }
               
        pipeline.addLast("handler", playHandler);
        playHandler.pipelines.put("handler", playHandler);

        return pipeline;
    }

    protected String getName(String name) {
        int dotIndex = name.lastIndexOf('.');
        if (dotIndex > 0)
            return name.substring(dotIndex + 1);
        return name;
    }

    protected ChannelHandler getInstance(String name) throws Exception {

        Class<?> clazz = classes.computeIfAbsent(name, className -> {
            try {
                return Class.forName(className);
            } catch (ClassNotFoundException e) {
                throw new UnexpectedException(e);
            }
        });
        if (ChannelHandler.class.isAssignableFrom(clazz))
            return (ChannelHandler)clazz.newInstance(); 
        return null;
    }
}

