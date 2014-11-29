package play.server;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;

import java.util.HashMap;
import java.util.Map;

import play.Logger;
import play.Play;

public class HttpServerChannelInitializer extends ChannelInitializer<SocketChannel> {

    private String pipelineConfig = Play.configuration.getProperty("play.netty.pipeline", "play.server.FlashPolicyHandler,org.jboss.netty.handler.codec.http.HttpRequestDecoder,play.server.StreamChunkAggregator,org.jboss.netty.handler.codec.http.HttpResponseEncoder,org.jboss.netty.handler.stream.ChunkedWriteHandler,play.server.PlayHandler");

    protected static Map<String, Class> classes = new HashMap<String, Class>();

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.netty.channel.ChannelInitializer#initChannel(io.netty.channel.Channel)
	 */
	@Override
	protected void initChannel(SocketChannel ch) throws Exception {
        
        String[] handlers = pipelineConfig.split(",");  
        if (handlers.length <= 0) {
            Logger.error("You must defined at least the playHandler in \"play.netty.pipeline\"");
            return;
        }       
        
        // Create the play Handler (always the last one)
        String handler = handlers[handlers.length - 1];
        ChannelHandler instance = getInstance(handler);
        PlayHandler playHandler = (PlayHandler) instance;
        if (playHandler == null) {
            Logger.error("The last handler must be the playHandler in \"play.netty.pipeline\"");
            return;
        }
      
        // Get all the pipeline. Give the user the opportunity to add their own
        for (int i = 0; i < handlers.length - 1; i++) {
            handler = handlers[i];
            try {
                String name = getName(handler.trim());
                instance = getInstance(handler);
                
                if (instance != null) {
                	ch.pipeline().addLast(name, instance);
                }
            } catch (Throwable e) {
                Logger.error(" error adding " + handler, e);
            }
        }
               
        if (playHandler != null) {
        	ch.pipeline().addLast("handler", playHandler);
        } 
    }

    protected String getName(String name) {
        if (name.lastIndexOf(".") > 0)
            return name.substring(name.lastIndexOf(".") + 1);
        return name;
    }

    protected ChannelHandler getInstance(String name) throws Exception {
        Class clazz = classes.get(name);
        if (clazz == null) {
            clazz = Class.forName(name);
            classes.put(name, clazz);
        }
        if (ChannelHandler.class.isAssignableFrom(clazz))
            return (ChannelHandler)clazz.newInstance(); 
        return null;
    }
}

