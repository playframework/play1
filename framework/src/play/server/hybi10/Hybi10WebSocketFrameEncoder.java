package play.server.hybi10;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.http.websocket.DefaultWebSocketFrame;
import org.jboss.netty.handler.codec.oneone.OneToOneEncoder;
import play.Logger;

import java.nio.ByteBuffer;
import java.security.SecureRandom;

/**
 * Encodes frames going out. Frames are not masked.
 */
public class Hybi10WebSocketFrameEncoder extends OneToOneEncoder {

    private static final byte OPCODE_TEXT = 0x1;
    private static final byte OPCODE_BINARY = 0x2;
    private static final byte OPCODE_PING = 0x9;
    private static final byte OPCODE_PONG = 0xA;

    @Override
    protected Object encode(ChannelHandlerContext ctx, Channel channel, Object msg) throws Exception {

        if (msg instanceof DefaultWebSocketFrame) {
            final DefaultWebSocketFrame frame = (DefaultWebSocketFrame) msg;

            ChannelBuffer data = frame.getBinaryData();
			if (data == null) {
				data = ChannelBuffers.EMPTY_BUFFER;
			}

            byte opcode;
            // TODO: Close and CONTINUATION
            if(frame instanceof Ping) {
                opcode = OPCODE_PING;
            } else if(frame instanceof Pong) {
                opcode = OPCODE_PONG;
            } else {
                opcode = frame.isText() ? OPCODE_TEXT : OPCODE_BINARY;
            }


            int length = data.readableBytes();

			int b0 = 0;
		    b0 |= (1 << 7);
            // TODO: RSV, for now it is set to 0
			b0 |= (0 % 8) << 4;
			b0 |= opcode % 128;

            ChannelBuffer header;
            ChannelBuffer body;


            // TODO: if there is no mask
            int maskLength = 4;
            if (length <= 125) {
                header = ChannelBuffers.buffer(2 + maskLength);
                header.writeByte(b0);
                byte b = (byte) (0x80 | (byte) length);
                header.writeByte(b);
            } else if (length <= 0xFFFF) {
                header = ChannelBuffers.buffer(4 + maskLength);
                header.writeByte(b0);
                header.writeByte(0x80 | 126);
                header.writeByte((length >>> 8) & 0xFF);
                header.writeByte((length) & 0xFF);
            } else {
                header = ChannelBuffers.buffer(10 + maskLength);
                header.writeByte(b0);
                header.writeByte(0x80 | 127);
                header.writeLong(length);
            }

            Integer random = (int) (Math.random() * Integer.MAX_VALUE);
            byte[] mask = ByteBuffer.allocate(4).putInt(random).array();
            header.writeBytes(mask);

            body = ChannelBuffers.buffer(length);
            int counter = 0;
            while (data.readableBytes() > 0) {
                byte byteData = data.readByte();
                body.writeByte(byteData ^ mask[+counter++ % 4]);
            }
            return ChannelBuffers.wrappedBuffer(header, body);
        }

        return msg;
    }

}
