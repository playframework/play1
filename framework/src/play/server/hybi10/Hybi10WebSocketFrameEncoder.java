package play.server.hybi10;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.http.websocket.DefaultWebSocketFrame;
import org.jboss.netty.handler.codec.oneone.OneToOneEncoder;

import java.security.SecureRandom;

/**
 * Encodes frames going out. Frames are not masked.
 */
public class Hybi10WebSocketFrameEncoder extends OneToOneEncoder {

    private static final byte OPCODE_TEXT = 0x1;
    private static final byte OPCODE_BINARY = 0x2;
    private static final byte OPCODE_PING = 0x9;
    private static final byte OPCODE_PONG = 0xA;

    private void mask(byte[] mask, byte[] target, int location, byte[] bytes) {
        if (bytes != null && target != null) {
            int index = 0;
            for (int i = 0; i < bytes.length; i++) {
                target[location + i] = mask == null
                        ? bytes[i]
                        : (byte) (bytes[i] ^ mask[index++ % 4]);
            }
        }
    }

    private static byte[] toArray(long length) {
        long value = length;
        byte[] b = new byte[8];
        for (int i = 7; i >= 0 && value > 0; i--) {
            b[i] = (byte) (value & 0xFF);
            value >>= 8;
        }
        return b;
    }

    private byte[] encodeLength(final long length) {
        byte[] lengthBytes;
        if (length <= 125) {
            lengthBytes = new byte[1];
            lengthBytes[0] = (byte) length;
        } else {
            byte[] b = toArray(length);
            if (length <= 0xFFFF) {
                lengthBytes = new byte[3];
                lengthBytes[0] = 126;
                System.arraycopy(b, 6, lengthBytes, 1, 2);
            } else {
                lengthBytes = new byte[9];
                lengthBytes[0] = 127;
                System.arraycopy(b, 0, lengthBytes, 1, 8);
            }
        }
        return lengthBytes;
    }


    @Override
    protected Object encode(ChannelHandlerContext ctx, Channel channel, Object msg) throws Exception {

        // TODO: work with channel buffer instead of byte[]
        if (msg instanceof DefaultWebSocketFrame) {
            final DefaultWebSocketFrame frame = (DefaultWebSocketFrame) msg;
            byte type;
            // Text frame
            ChannelBuffer buffer = frame.getBinaryData();
            buffer.clear();

            final byte[] bytes = buffer.array();
            final byte[] lengthBytes = encodeLength(bytes.length);

            final int length = 1 + lengthBytes.length + bytes.length + 4;
            final int payloadStart = 1 + lengthBytes.length + 4;
            final byte[] packet = new byte[length];

            if(frame instanceof Ping) {
                type = OPCODE_PING;
            } else if(frame instanceof Pong) {
                type = OPCODE_PONG;
            } else {
                type = frame.isText() ? OPCODE_TEXT : OPCODE_BINARY;
            }

            // Is this a final packet?
            packet[0] = (byte)(type | 0x80);
            System.arraycopy(lengthBytes, 0, packet, 1, lengthBytes.length);

            // Mask, yes please
            packet[1] |= 0x80;

            byte[] mask = generateMask();
            mask(mask, packet, payloadStart, bytes);
            System.arraycopy(mask, 0, packet, payloadStart - 4,
                    4);

            ChannelBuffer encoded =
                    channel.getConfig().getBufferFactory().getBuffer(packet.length);
            encoded.writeBytes(packet);


            return encoded;
        }
        return msg;
    }

    private byte[] generateMask() {
        byte[]  mask = new byte[4];
        new SecureRandom().nextBytes(mask);
        return mask;
    }
}
