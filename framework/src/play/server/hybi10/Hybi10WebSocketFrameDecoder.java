package play.server.hybi10;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.frame.CorruptedFrameException;
import org.jboss.netty.handler.codec.frame.TooLongFrameException;
import org.jboss.netty.handler.codec.http.websocket.DefaultWebSocketFrame;
import org.jboss.netty.handler.codec.replay.ReplayingDecoder;

import java.util.ArrayList;
import java.util.List;

public class Hybi10WebSocketFrameDecoder extends ReplayingDecoder<Hybi10WebSocketFrameDecoder.State> {

    private static final byte OPCODE_CONT = 0x0;
    private static final byte OPCODE_TEXT = 0x1;
    private static final byte OPCODE_BINARY = 0x2;
    private static final byte OPCODE_CLOSE = 0x8;
    private static final byte OPCODE_PING = 0x9;
    private static final byte OPCODE_PONG = 0xA;

    public static final int MAX_LENGTH = 16384;

    private Byte fragmentOpcode;
    private Byte opcode = null;
    private int currentFrameLength;
    private ChannelBuffer maskingKey;
    private List<ChannelBuffer> frames = new ArrayList<ChannelBuffer>();

    public static enum State {
        FRAME_START,
        PARSING_LENGTH,
        MASKING_KEY,
        PARSING_LENGTH_2,
        PARSING_LENGTH_3,
        PAYLOAD
    }

    public Hybi10WebSocketFrameDecoder() {
        super(State.FRAME_START);
    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, Channel channel, ChannelBuffer buffer, State state) throws Exception {
        switch (state) {
            case FRAME_START:
                byte b = buffer.readByte();
                byte fin = (byte) (b & 0x80);
                byte reserved = (byte) (b & 0x70);
                byte opcode = (byte) (b & 0x0F);

                if (reserved != 0) {
                    throw new CorruptedFrameException("Reserved bits set: " + bits(reserved));
                }
                if (!isOpcode(opcode)) {
                    throw new CorruptedFrameException("Invalid opcode " + hex(opcode));
                }

                if (fin == 0) {
                    if (fragmentOpcode == null) {
                        if (!isDataOpcode(opcode)) {
                            throw new CorruptedFrameException("Fragmented frame with invalid opcode " + hex(opcode));
                        }
                        fragmentOpcode = opcode;
                    } else if (opcode != OPCODE_CONT) {
                        throw new CorruptedFrameException("Continuation frame with invalid opcode " + hex(opcode));
                    }
                } else {
                    if (fragmentOpcode != null) {
                        if (!isControlOpcode(opcode) && opcode != OPCODE_CONT) {
                            throw new CorruptedFrameException("Final frame with invalid opcode " + hex(opcode));
                        }
                    } else if (opcode == OPCODE_CONT) {
                        throw new CorruptedFrameException("Final frame with invalid opcode " + hex(opcode));
                    }
                    this.opcode = opcode;
                }

                checkpoint(State.PARSING_LENGTH);
            case PARSING_LENGTH:
                b = buffer.readByte();
                byte masked = (byte) (b & 0x80);
                if (masked == 0) {
                    throw new CorruptedFrameException("Unmasked frame received");
                }

                int length = (byte) (b & 0x7F);
                if (length < 126) {
                    currentFrameLength = length;
                    checkpoint(State.MASKING_KEY);
                } else if (length == 126) {
                    checkpoint(State.PARSING_LENGTH_2);
                } else if (length == 127) {
                    checkpoint(State.PARSING_LENGTH_3);
                }
                return null;
            case PARSING_LENGTH_2:
                int s =  buffer.readUnsignedShort();
                currentFrameLength = s;
                checkpoint(State.MASKING_KEY);
                return null;
            case PARSING_LENGTH_3:
                currentFrameLength = (int)buffer.readLong();
                checkpoint(State.MASKING_KEY);
                return null;
            case MASKING_KEY:
                maskingKey = buffer.readBytes(4);
                checkpoint(State.PAYLOAD);
            case PAYLOAD:
                if (currentFrameLength < 126)
                    checkpoint(State.FRAME_START);
                ChannelBuffer frame = buffer.readBytes(currentFrameLength);
                unmask(frame);

                if (this.opcode == OPCODE_CONT) {
                    this.opcode = fragmentOpcode;
                    frames.add(frame);

                    frame = channel.getConfig().getBufferFactory().getBuffer(0);
                    for (ChannelBuffer channelBuffer : frames) {
                        frame.ensureWritableBytes(channelBuffer.readableBytes());
                        frame.writeBytes(channelBuffer);
                    }

                    this.fragmentOpcode = null;
                    frames.clear();
                }


                if (this.opcode == OPCODE_TEXT) {
                    // TODO: This is only for the old hybri76?
                    // if (frame.readableBytes() > MAX_LENGTH) {
                       // throw new TooLongFrameException();
                    //}
                    return new DefaultWebSocketFrame(0x00, frame);
                } else if (this.opcode == OPCODE_BINARY) {
                    return new DefaultWebSocketFrame(0xFF, frame);
                } else if (this.opcode == OPCODE_PING) {
                    channel.write(new Pong(0x00, frame));
                    return null;
                } else if (this.opcode == OPCODE_PONG) {
                    return new Pong(0x00, frame);
                } else if (this.opcode == OPCODE_CLOSE) {
                    // TODO
                    return null;
                }
            default:
                throw new Error("Shouldn't reach here.");
        }
    }

    private void unmask(ChannelBuffer frame) {
        byte[] bytes = frame.array();
        for (int i = 0; i < bytes.length; i++) {
            int b = frame.getByte(i) ^ maskingKey.getByte(i % 4);
            frame.setByte(i, b);
        }
    }

    private String bits(byte b) {
        return Integer.toBinaryString(b).substring(24);
    }

    private String hex(byte b) {
        return Integer.toHexString(b);
    }

    private boolean isOpcode(int opcode) {
        return opcode == OPCODE_CONT ||
                opcode == OPCODE_TEXT ||
                opcode == OPCODE_BINARY ||
                opcode == OPCODE_CLOSE ||
                opcode == OPCODE_PING ||
                opcode == OPCODE_PONG;
    }

    private boolean isControlOpcode(int opcode) {
        return opcode == OPCODE_CLOSE ||
                opcode == OPCODE_PING ||
                opcode == OPCODE_PONG;
    }

    private boolean isDataOpcode(int opcode) {
        return opcode == OPCODE_TEXT ||
                opcode == OPCODE_BINARY;
    }
}
