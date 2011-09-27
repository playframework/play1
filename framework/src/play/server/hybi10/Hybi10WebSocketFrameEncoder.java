package play.server.hybi10;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.http.websocket.WebSocketFrame;
import org.jboss.netty.handler.codec.oneone.OneToOneEncoder;

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
        if (msg instanceof WebSocketFrame) {
            WebSocketFrame frame = (WebSocketFrame) msg;
            ChannelBuffer data = frame.getBinaryData();
            ChannelBuffer encoded =
                    channel.getConfig().getBufferFactory().getBuffer(
                            data.order(), data.readableBytes() + 6);

            byte opcode;
            if(frame instanceof Ping) {
                opcode = OPCODE_PING;
            } else if(frame instanceof Pong) {
                opcode = OPCODE_PONG;
            } else {
                opcode = frame.isText() ? OPCODE_TEXT : OPCODE_BINARY;
            }
            encoded.writeByte(0x80 | opcode);

            int length = data.readableBytes();
            if (length < 126) {
                encoded.writeByte(length);
            } else if (length < 65535) {
                encoded.writeByte(126);
                encoded.writeShort(length);
            } else {
                encoded.writeByte(127);
                encoded.writeInt(length);
            }

            encoded.writeBytes(data, data.readerIndex(), data.readableBytes());
            encoded = encoded.slice(0, encoded.writerIndex());
            return encoded;
        }
        return msg;
    }
}
