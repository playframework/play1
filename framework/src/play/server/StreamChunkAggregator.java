package play.server;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMessage;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.LastHttpContent;
import play.Play;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.UUID;

public class StreamChunkAggregator extends ChannelInboundHandlerAdapter {

    private static final int MAX_CONTENT_LENGTH = Integer.parseInt(Play.configuration.getProperty("play.netty.maxContentLength", "-1"));

    private volatile HttpMessage currentMessage;
    private volatile OutputStream out;
    private volatile ByteBuf memoryBuffer;
    private volatile File file;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (!(msg instanceof HttpMessage) && !(msg instanceof HttpContent)) {
            ctx.fireChannelRead(msg);
            return;
        }

        HttpMessage currentMessage = this.currentMessage;
        if (currentMessage == null) {
            if (msg instanceof HttpRequest m) {
                this.currentMessage = m;
                if (HttpUtil.isTransferEncodingChunked(m)) {
                    this.file = new File(Play.tmpDir, UUID.randomUUID().toString());
                    this.out = new FileOutputStream(file, true);
                } else {
                    // Not a chunked message - buffer the body in memory.
                    this.memoryBuffer = Unpooled.buffer();
                }
            } else {
                // Unexpected object - pass through.
                ctx.fireChannelRead(msg);
            }
        } else if (msg instanceof HttpContent chunk) {
            boolean last = chunk instanceof LastHttpContent;
            // TODO: If less that threshold then in memory
            if (file != null) {
                if (MAX_CONTENT_LENGTH != -1 && (file.length() > (MAX_CONTENT_LENGTH - chunk.content().readableBytes()))) {
                    currentMessage.headers().set(HttpHeaderNames.WARNING, "play.netty.content.length.exceeded");
                } else {
                    try (var s = new ByteBufInputStream(chunk.content())) {
                        s.transferTo(this.out);
                    }
                }
            } else {
                memoryBuffer.writeBytes(chunk.content());
            }
            chunk.release();
            if (last) {
                finalizeAggregation(ctx, currentMessage);
                this.currentMessage = null;
                this.memoryBuffer = null;
                this.file = null;
                this.out = null;
            }
        } else {
            ctx.fireChannelRead(msg);
        }
    }

    private void finalizeAggregation(ChannelHandlerContext ctx, HttpMessage currentMessage) {
        try {
            if (out != null) {
                out.flush();
                out.close();
            }

            HttpRequest request = (HttpRequest) currentMessage;
            ByteBuf content;
            long contentLength;
            if (file != null) {
                content = new FileChannelBuffer(file);
                contentLength = file.length();
            } else {
                content = memoryBuffer;
                contentLength = content.readableBytes();
            }

            FullHttpRequest fullRequest = new DefaultFullHttpRequest(request.protocolVersion(), request.method(),
                    request.uri(), content);
            for (Map.Entry<String, String> entry : request.headers()) {
                fullRequest.headers().add(entry.getKey(), entry.getValue());
            }
            fullRequest.headers().remove(HttpHeaderNames.TRANSFER_ENCODING);
            fullRequest.headers().set(HttpHeaderNames.CONTENT_LENGTH, String.valueOf(contentLength));

            ctx.fireChannelRead(fullRequest);

            if (file != null) {
                file.delete();
            }
        } catch (Exception e) {
            ctx.fireExceptionCaught(e);
        }
    }
}