package play.server;

import org.apache.commons.io.IOUtils;
import org.jboss.netty.channel.*;
import org.jboss.netty.handler.codec.http.HttpChunk;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpMessage;
import play.Play;

import java.io.*;
import java.util.List;
import java.util.UUID;

public class StreamChunkAggregator extends SimpleChannelUpstreamHandler {

    private volatile HttpMessage currentMessage;
    private volatile BufferedWriter out;
    private final int maxContentLength;
    private volatile File file;

    /**
     * Creates a new instance.
     */
    public StreamChunkAggregator(int maxContentLength) {
        this.maxContentLength = maxContentLength;
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e)
            throws Exception {

        Object msg = e.getMessage();
        if (!(msg instanceof HttpMessage) && !(msg instanceof HttpChunk)) {
            ctx.sendUpstream(e);
            return;
        }


        HttpMessage currentMessage = this.currentMessage;
        BufferedWriter out = this.out;
        File localFile = this.file;
        if (currentMessage == null) {
            HttpMessage m = (HttpMessage) msg;
            if (m.isChunked()) {
                final String localName = UUID.randomUUID().toString();

                // A chunked message - remove 'Transfer-Encoding' header,
                // initialize the cumulative buffer, and wait for incoming chunks.
                // TODO Add HttpMessage/HttpChunkTrailer.removeHeader(name, value)
                List<String> encodings = m.getHeaders(HttpHeaders.Names.TRANSFER_ENCODING);
                encodings.remove(HttpHeaders.Values.CHUNKED);
                if (encodings.isEmpty()) {
                    m.removeHeader(HttpHeaders.Names.TRANSFER_ENCODING);
                }
                this.currentMessage = m;
                this.file = new File(Play.tmpDir, localName);
                final FileWriter fstream = new FileWriter(file, true);
                this.out = new BufferedWriter(fstream);
            } else {
                // Not a chunked message - pass through.
                ctx.sendUpstream(e);
            }
        } else {
            // TODO: If less that threshold then in memory
            // Merge the received chunk into the content of the current message.
            final HttpChunk chunk = (HttpChunk) msg;
            if (maxContentLength != -1 && (localFile.length() > (maxContentLength - chunk.getContent().readableBytes()))) {
//                currentMessage.addHeader(
//                        HttpHeaders.Names.CONTENT_LENGTH, String.valueOf(maxContentLength));
                currentMessage.setHeader(
                        HttpHeaders.Names.WARNING, "play.netty.content.length.exceeded");
            } else {
                byte[] b = new byte[chunk.getContent().capacity()];
                chunk.getContent().getBytes(0, b);
                IOUtils.copy(new ByteArrayInputStream(b), out);


                //fstream.close();

                if (chunk.isLast()) {
                    currentMessage.setHeader(
                            HttpHeaders.Names.CONTENT_LENGTH,
                            String.valueOf(localFile.length()));

                    currentMessage.setContent(new FileChannelBuffer(localFile));
                    out.flush();
                    out.close();
                    this.out = null;
                    this.currentMessage = null;
                    this.file = null;
                    Channels.fireMessageReceived(ctx, currentMessage, e.getRemoteAddress());
                }
            }
        }

    }
}

