package play.server;

import org.apache.commons.io.IOUtils;
import io.netty.channel.*;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMessage;
import play.Play;

import java.io.*;
import java.util.List;
import java.util.UUID;

public class StreamChunkAggregator extends ChannelInboundHandlerAdapter {

    private volatile HttpMessage currentMessage;
    private volatile OutputStream out;
    private final static int maxContentLength = Integer.valueOf(Play.configuration.getProperty("play.netty.maxContentLength", "-1"));
    private volatile File file;

    /**
     * Creates a new instance.
     */
    public StreamChunkAggregator() { }

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
                final String localName = UUID.randomUUID().toString();
                // A chunked message - remove 'Transfer-Encoding' header,
                // initialize the cumulative buffer, and wait for incoming chunks.
                List<String> encodings = m.headers().getAll(HttpHeaders.Names.TRANSFER_ENCODING);
                encodings.remove(HttpHeaders.Values.CHUNKED);
                if (encodings.isEmpty()) {
                    m.headers().remove(HttpHeaders.Names.TRANSFER_ENCODING);
                }
                this.currentMessage = m;
                this.file = new File(Play.tmpDir, localName);
                this.out = new FileOutputStream(file, true);
            } else {
                // Not a chunked message - pass through.
                ctx.sendUpstream(e);
            }
        } else {
            // TODO: If less that threshold then in memory
            // Merge the received chunk into the content of the current message.
            final HttpChunk chunk = (HttpChunk) msg;
            if (maxContentLength != -1 && (localFile.length() > (maxContentLength - chunk.getContent().readableBytes()))) {
                currentMessage.headers().set(HttpHeaders.Names.WARNING, "play.netty.content.length.exceeded");
            } else {
                IOUtils.copyLarge(new ChannelBufferInputStream(chunk.getContent()), this.out);

                if (chunk.isLast()) {
                    this.out.flush();
                    this.out.close();

                    currentMessage.headers().set(HttpHeaders.Names.CONTENT_LENGTH, String.valueOf(localFile.length()));

                    currentMessage.setContent(new FileChannelBuffer(localFile));
                    this.out = null;
                    this.currentMessage = null;
		            this.file.delete();
                    this.file = null;
                    Channels.fireMessageReceived(ctx, currentMessage, e.getRemoteAddress());
                }
            }
        }

    }
}
