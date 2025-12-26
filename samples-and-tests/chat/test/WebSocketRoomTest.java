import org.junit.jupiter.api.Test;

import play.mvc.Http.Response;
import play.test.FunctionalTest;

public class WebSocketRoomTest extends FunctionalTest {

    @Test
    public void roomRendersWebSocketPage() {
        Response response = GET("/websocket/room?user=Bob");
        assertIsOk(response);
        assertContentType("text/html", response);
        assertContentMatch(".*WebSocket.*", response);
        assertContentMatch(".*You are now chatting as Bob.*", response);
    }

}