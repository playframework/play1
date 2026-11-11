import org.junit.jupiter.api.Test;

import play.mvc.Http.Response;

/**
 * Functional test of the two-user chat scenario over the long-polling
 * protocol. The "chat" is the JSON returned by {@code LongPolling.waitMessages}.
 * The whole event archive is fetched with {@code lastReceived=0} and each event
 * type / text is counted to assert it appears exactly once.
 */
public class LongPollingTwoUserChatTest extends AbstractTwoUserChatTest {

    @Test
    public void twoUsersChatScenario() {
        // 1. Alice connects — exactly ONE join event in her chat
        GET("/longpolling/room?user=Alice");
        Response alicePoll = GET("/longpolling/room/messages?lastReceived=0");
        assertIsOk(alicePoll);
        assertContentType("application/json", alicePoll);
        assertEquals(1, countOccurrences(getContent(alicePoll), "\"type\":\"join\""));

        // 2. Alice sends a message — it appears exactly ONCE
        POST("/longpolling/room/messages", "application/x-www-form-urlencoded", "user=Alice&message=Hello, Bob!");
        Response alicePoll2 = GET("/longpolling/room/messages?lastReceived=0");
        assertIsOk(alicePoll2);
        assertEquals(1, countOccurrences(getContent(alicePoll2), "\"type\":\"message\""));
        assertEquals(1, countOccurrences(getContent(alicePoll2), "Hello, Bob!"));

        // 3. Bob connects — Alice sees Bob's join, Bob sees the full history
        GET("/longpolling/room?user=Bob");
        Response alicePoll3 = GET("/longpolling/room/messages?lastReceived=0");
        assertIsOk(alicePoll3);
        assertEquals(2, countOccurrences(getContent(alicePoll3), "\"type\":\"join\""));
        assertEquals(1, countOccurrences(getContent(alicePoll3), "\"user\":\"Bob\""));

        Response bobPoll3 = GET("/longpolling/room/messages?lastReceived=0");
        assertIsOk(bobPoll3);
        assertEquals(2, countOccurrences(getContent(bobPoll3), "\"type\":\"join\""));
        assertEquals(1, countOccurrences(getContent(bobPoll3), "\"type\":\"message\""));
        assertEquals(1, countOccurrences(getContent(bobPoll3), "\"user\":\"Bob\""));
        assertEquals(1, countOccurrences(getContent(bobPoll3), "Hello, Bob!"));

        // 4. Bob sends a message — it appears exactly ONCE in both chats
        POST("/longpolling/room/messages", "application/x-www-form-urlencoded", "user=Bob&message=Hi, Alice!");
        Response alicePoll4 = GET("/longpolling/room/messages?lastReceived=0");
        Response bobPoll4 = GET("/longpolling/room/messages?lastReceived=0");
        assertIsOk(alicePoll4);
        assertIsOk(bobPoll4);
        assertEquals(1, countOccurrences(getContent(alicePoll4), "Hi, Alice!"));
        assertEquals(1, countOccurrences(getContent(bobPoll4), "Hi, Alice!"));

        // 5. Alice leaves — Bob sees the leave event
        GET("/longpolling/room/leave?user=Alice");
        Response bobPoll5 = GET("/longpolling/room/messages?lastReceived=0");
        assertIsOk(bobPoll5);
        assertEquals(1, countOccurrences(getContent(bobPoll5), "\"type\":\"leave\""));
    }

}