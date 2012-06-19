package play.server;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.jboss.netty.buffer.ChannelBufferInputStream;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.handler.codec.http.HttpChunk;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpMessage;

import play.Play;

public class StreamChunkAggregator extends SimpleChannelUpstreamHandler {

    private volatile HttpMessage currentMessage;
    private volatile OutputStream out;
    private final int maxContentLength;
    private final int memoryThreshold;
    
    // used to read large body
    private volatile File file;
    // used to write into memory (until the content length is over the threshold)
    private ByteArrayOutputStream buf = null;

    /**
     * Creates a new instance.
     */
    public StreamChunkAggregator(int maxContentLength) {
        this(maxContentLength, -1);
    }
    
    /**
     * Creates a new instance.
     */
    public StreamChunkAggregator(int maxContentLength, int memoryThreshold) {
        this.maxContentLength = maxContentLength;
        this.memoryThreshold = memoryThreshold;
        
        if ( memoryThreshold > 0 ) {
        	// FIXME init with a shortness capacity ???
        	buf = new ByteArrayOutputStream(memoryThreshold);
        }
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        Object msg = e.getMessage();
        if (!(msg instanceof HttpMessage) && !(msg instanceof HttpChunk)) {
            ctx.sendUpstream(e);
            return;
        }

        HttpMessage currentMessage = this.currentMessage;
        File localFile = this.file;
        if (currentMessage == null) {
            HttpMessage m = (HttpMessage) msg;
            if (m.isChunked()) {
                // A chunked message - remove 'Transfer-Encoding' header,
                // initialize the cumulative buffer, and wait for incoming chunks.
                List<String> encodings = m.getHeaders(HttpHeaders.Names.TRANSFER_ENCODING);
                encodings.remove(HttpHeaders.Values.CHUNKED);
                if (encodings.isEmpty()) {
                    m.removeHeader(HttpHeaders.Names.TRANSFER_ENCODING);
                }
                this.currentMessage = m;
                if ( buf != null ) {
                	this.out = buf;
                	buf.reset(); // in theory already reseted but : twice is better than once
                } else { 
                	initTemporaryFile(false);
                }
            } else {
                // Not a chunked message - pass through.
                ctx.sendUpstream(e);
            }
        } else {
            // TODO: If less that threshold then in memory
            // Merge the received chunk into the content of the current message.
            final HttpChunk chunk = (HttpChunk) msg;
            if (maxContentLength != -1 && (contentLength() > (maxContentLength - chunk.getContent().readableBytes()))) {
                currentMessage.setHeader(HttpHeaders.Names.WARNING, "play.netty.content.length.exceeded");
            } else {
            	
            	// switch to file if out of memory buffer
            	if ( localFile == null && (buf.size() + chunk.getContent().readableBytes()) > memoryThreshold ) {
            		localFile = initTemporaryFile(true);
            	}
            	
                IOUtils.copyLarge(new ChannelBufferInputStream(chunk.getContent()), this.out);

                if (chunk.isLast()) {
                    this.out.flush();
                    this.out.close(); // has no effect on the ByteArrayOutputStream

                    currentMessage.setHeader(
                            HttpHeaders.Names.CONTENT_LENGTH,
                            String.valueOf(contentLength()));

                    currentMessage.setContent(localFile == null ? new MemoryChannelBuffer(buf.toByteArray()) : new FileChannelBuffer(localFile, true));
                    this.out = null;
                    this.currentMessage = null;
                    this.file = null;
                    if  ( buf != null ) buf.reset();
                    
                    Channels.fireMessageReceived(ctx, currentMessage, e.getRemoteAddress());
                }
            }
        }
    }
    
    /**
     * Init the temporary file and recopy already grabbed data into memory into this new temporary file. 
     */
    private final File initTemporaryFile(boolean recopy) throws IOException {
    	final String localName = UUID.randomUUID().toString();
    	
    	file = new File(Play.tmpDir, localName);
        out = new FileOutputStream(file, true);
        
        if ( recopy && buf != null && buf.size() > 0 ) {
        	buf.writeTo(out);
        }
        
        return file;
    }
    
    /**
     * Get the length of the read content
     */
    private final long contentLength()
    {
    	return file != null ? file.length() : buf.size(); 
    }
}

