package play.server;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.frame.FrameDecoder;
import org.jboss.netty.util.CharsetUtil;

public class FlashPolicyHandler extends FrameDecoder {

    private static final String XML = "<cross-domain-policy><allow-access-from domain=\"*\" to-ports=\"*\" /></cross-domain-policy>";
    private ChannelBuffer policyResponse = ChannelBuffers.copiedBuffer(XML, CharsetUtil.UTF_8);

    /**
     * Creates a handler allowing access from any domain and any port
     */
    public FlashPolicyHandler() {
        super();
    }

    /**
     * Create a handler with a custom XML response. Useful for defining your own domains and ports.
     * @param policyResponse Response XML to be passed back to a connecting client
     */
    public FlashPolicyHandler(ChannelBuffer policyResponse) {
        super();
        this.policyResponse = policyResponse;
    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, Channel channel, ChannelBuffer buffer) throws Exception {
        if (buffer.readableBytes() < 2) {
            return null;
        }

        int magic1 = buffer.getUnsignedByte(buffer.readerIndex());
        int magic2 = buffer.getUnsignedByte(buffer.readerIndex() + 1);
        boolean isFlashPolicyRequest = (magic1 == '<' && magic2 == 'p');

        if (isFlashPolicyRequest) {
            buffer.skipBytes(buffer.readableBytes()); // Discard everything
            channel.write(policyResponse).addListener(ChannelFutureListener.CLOSE);
            return null;
        }

        // Remove ourselves, important since the byte length check at top can hinder frame decoding
        // down the pipeline
        ctx.getPipeline().remove(this);
        return buffer.readBytes(buffer.readableBytes());
    }

}
