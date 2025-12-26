import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import play.mvc.Http.Response;
import play.test.FunctionalTest;

import models.ChatRoomTestSupport;

public class RefreshTest extends FunctionalTest {

    @BeforeEach
    public void resetChatRoom() {
        ChatRoomTestSupport.reset();
    }

    @Test
    public void indexRedirectsToRoom() {
        Response response = GET("/refresh?user=Bob");
        assertStatus(302, response);
        assertHeaderEquals("Location", "/refresh/room?user=Bob", response);
    }

    @Test
    public void roomShowsEmptyThread() {
        Response response = GET("/refresh/room?user=Bob");
        assertIsOk(response);
        assertContentType("text/html", response);
        assertContentMatch(".*You are now chatting as Bob.*", response);
    }

    @Test
    public void sayRedirectsToRoom() {
        Response response = POST("/refresh/room", "application/x-www-form-urlencoded", "user=Bob&message=Hello there");
        assertStatus(302, response);
        assertHeaderEquals("Location", "/refresh/room?user=Bob", response);
    }

    @Test
    public void sayIgnoresBlankMessage() {
        Response response = POST("/refresh/room", "application/x-www-form-urlencoded", "user=Bob&message=");
        assertStatus(302, response);
        assertHeaderEquals("Location", "/refresh/room?user=Bob", response);
    }

    @Test
    public void leaveRedirectsToSignInPage() {
        Response response = GET("/refresh/room/leave?user=Bob");
        assertStatus(302, response);
        assertHeaderEquals("Location", "/", response);
    }

}