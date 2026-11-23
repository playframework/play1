import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import play.mvc.Http.Response;
import play.test.FunctionalTest;

import models.ChatRoomTestSupport;

public class LongPollingTest extends FunctionalTest {

    @BeforeEach
    public void resetChatRoom() {
        ChatRoomTestSupport.reset();
    }

    @Test
    public void roomJoinsAndShowsRoom() {
        Response response = GET("/longpolling/room?user=Bob");
        assertIsOk(response);
        assertContentType("text/html", response);
        assertContentMatch(".*Ajax, long polling.*", response);
        assertContentMatch(".*You are now chatting as Bob.*", response);
    }

    @Test
    public void waitMessagesReturnsJsonOfJoinEvent() {
        GET("/longpolling/room?user=Bob");

        Response response = GET("/longpolling/room/messages?lastReceived=0");
        assertIsOk(response);
        assertContentType("application/json", response);
        assertContentMatch(".*join.*", response);
        assertContentMatch(".*Bob.*", response);
    }

    @Test
    public void waitMessagesReturnsJsonOfMessageEvent() {
        GET("/longpolling/room?user=Bob");
        POST("/longpolling/room/messages", "application/x-www-form-urlencoded", "user=Bob&message=Hello there");

        Response response = GET("/longpolling/room/messages?lastReceived=0");
        assertIsOk(response);
        assertContentType("application/json", response);
        assertContentMatch(".*message.*", response);
        assertContentMatch(".*Hello there.*", response);
    }

    @Test
    public void sayReturnsEmptyResponse() {
        Response response = POST("/longpolling/room/messages", "application/x-www-form-urlencoded", "user=Bob&message=Hello there");
        assertIsOk(response);
        assertContentEquals("", response);
    }

    @Test
    public void leaveRedirectsToSignInPage() {
        Response response = GET("/longpolling/room/leave?user=Bob");
        assertStatus(302, response);
        assertHeaderEquals("Location", "/", response);
    }

}