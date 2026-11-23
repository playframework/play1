import org.junit.jupiter.api.Test;

import play.mvc.Http.Response;

/**
 * Functional test of the two-user chat scenario over the active-refresh
 * protocol. The "chat" is the HTML room page rendered by {@code Refresh.room},
 * which shows the whole message archive. Occurrences are counted in the page
 * content to assert that each event appears exactly once.
 */
public class RefreshTwoUserChatTest extends AbstractTwoUserChatTest {

    @Test
    public void twoUsersChatScenario() {
        // 1. Alice connects — exactly ONE join message in her chat
        GET("/refresh?user=Alice");
        Response aliceRoom = GET("/refresh/room?user=Alice");
        assertIsOk(aliceRoom);
        assertEquals(1, countOccurrences(getContent(aliceRoom), "Alice joined the room"));

        // 2. Alice sends a message — it appears exactly ONCE
        POST("/refresh/room", "application/x-www-form-urlencoded", "user=Alice&message=Hello, Bob!");
        Response aliceRoom2 = GET("/refresh/room?user=Alice");
        assertIsOk(aliceRoom2);
        assertEquals(1, countOccurrences(getContent(aliceRoom2), "Hello, Bob!"));

        // 3. Bob connects — Alice sees Bob joining, Bob sees the full history
        GET("/refresh?user=Bob");
        Response aliceRoom3 = GET("/refresh/room?user=Alice");
        assertIsOk(aliceRoom3);
        assertEquals(1, countOccurrences(getContent(aliceRoom3), "Bob joined the room"));

        Response bobRoom3 = GET("/refresh/room?user=Bob");
        assertIsOk(bobRoom3);
        assertEquals(1, countOccurrences(getContent(bobRoom3), "Alice joined the room"));
        assertEquals(1, countOccurrences(getContent(bobRoom3), "Hello, Bob!"));
        assertEquals(1, countOccurrences(getContent(bobRoom3), "Bob joined the room"));

        // 4. Bob sends a message — it appears exactly ONCE in both chats
        POST("/refresh/room", "application/x-www-form-urlencoded", "user=Bob&message=Hi, Alice!");
        Response aliceRoom4 = GET("/refresh/room?user=Alice");
        Response bobRoom4 = GET("/refresh/room?user=Bob");
        assertIsOk(aliceRoom4);
        assertIsOk(bobRoom4);
        assertEquals(1, countOccurrences(getContent(aliceRoom4), "Hi, Alice!"));
        assertEquals(1, countOccurrences(getContent(bobRoom4), "Hi, Alice!"));

        // 5. Alice leaves — Bob sees the leave message
        GET("/refresh/room/leave?user=Alice");
        Response bobRoom5 = GET("/refresh/room?user=Bob");
        assertIsOk(bobRoom5);
        assertEquals(1, countOccurrences(getContent(bobRoom5), "Alice left the room"));
    }

}