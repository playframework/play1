package play.server.hybi10;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.handler.codec.http.websocket.DefaultWebSocketFrame;

public class Pong extends DefaultWebSocketFrame {
    public Pong(int type, ChannelBuffer frame) {
        super(type, frame);
    }
}
