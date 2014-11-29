package play.server;


import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.util.CharsetUtil;

import java.util.List;

public class FlashPolicyHandler extends ByteToMessageDecoder {
	
	private static final String XML = "<cross-domain-policy><allow-access-from domain=\"*\" to-ports=\"*\" /></cross-domain-policy>";
    private ByteBuf policyResponse = Unpooled.copiedBuffer(XML, CharsetUtil.UTF_8);

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
    public FlashPolicyHandler(ByteBuf policyResponse) {
        super();
        this.policyResponse = policyResponse;
    }
    
	/*
	 * (non-Javadoc)
	 * 
	 * @see io.netty.handler.codec.ByteToMessageDecoder#decode(io.netty.channel.ChannelHandlerContext, io.netty.buffer.ByteBuf, java.util.List)
	 */
	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        if (in.readableBytes() < 2) {
            return;
        }

        final int magic1 = in.getUnsignedByte(in.readerIndex());
        final int magic2 = in.getUnsignedByte(in.readerIndex() + 1);
        boolean isFlashPolicyRequest = (magic1 == '<' && magic2 == 'p');

        if (isFlashPolicyRequest) {
        	in.skipBytes(in.readableBytes()); // Discard everything
            ctx.channel().write(policyResponse).addListener(ChannelFutureListener.CLOSE);
            return;
        }

        // Remove ourselves, important since the byte length check at top can hinder frame decoding
        // down the pipeline
        ctx.pipeline().remove(this);
        
        out.add(in.readBytes(in.readableBytes()));
    }
}
