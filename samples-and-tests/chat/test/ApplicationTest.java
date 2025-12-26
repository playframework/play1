import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import play.mvc.Http.Response;
import play.test.FunctionalTest;

import models.ChatRoomTestSupport;

public class ApplicationTest extends FunctionalTest {

    @BeforeEach
    public void resetChatRoom() {
        ChatRoomTestSupport.reset();
    }

    @Test
    public void indexRendersSignInPage() {
        Response response = GET("/");
        assertIsOk(response);
        assertContentType("text/html", response);
        assertContentMatch(".*Sign in.*", response);
        assertContentMatch(".*The Play's Chat demonstration.*", response);
        assertContentMatch(".*Choose a nick name.*", response);
    }

    @Test
    public void enterDemoWithoutParamsRedirectsToIndexWithFlashError() {
        Response response = GET("/demo");
        assertStatus(302, response);
        assertHeaderEquals("Location", "/", response);

        Response index = GET("/");
        assertIsOk(index);
        assertContentMatch(".*Please choose a nick name.*", index);
    }

    @Test
    public void enterDemoDispatchesToRefreshDemo() {
        Response response = GET("/demo?user=Bob&demo=refresh");
        assertStatus(302, response);
        assertHeaderEquals("Location", "/refresh?user=Bob", response);
    }

    @Test
    public void enterDemoDispatchesToLongPollingDemo() {
        Response response = GET("/demo?user=Bob&demo=longpolling");
        assertStatus(302, response);
        assertHeaderEquals("Location", "/longpolling/room?user=Bob", response);
    }

    @Test
    public void enterDemoDispatchesToWebSocketDemo() {
        Response response = GET("/demo?user=Bob&demo=websocket");
        assertStatus(302, response);
        assertHeaderEquals("Location", "/websocket/room?user=Bob", response);
    }

    @Test
    public void enterDemoWithUnknownDemoRendersNothing() {
        Response response = GET("/demo?user=Bob&demo=unknown");
        assertIsOk(response);
        assertContentEquals("", response);
    }

}