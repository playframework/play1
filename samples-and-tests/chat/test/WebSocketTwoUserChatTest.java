import org.junit.jupiter.api.Test;

import play.libs.F.EventStream;
import play.mvc.Http.Response;

import models.ChatRoom;

/**
 * Functional test of the two-user chat scenario over the WebSocket protocol.
 *
 * The WebSocket handshake itself is set up by {@code play.server.PlayHandler}
 * outside the functional test runner, so the socket exchange cannot be driven
 * here. What CAN be verified is the exact event stream that
 * {@code WebSocket.ChatRoomSocket.join} subscribes to: each connected user
 * receives their own join event, each message is delivered exactly once, and a
 * user connecting later replays the whole previous history. Both users' room
 * pages are also checked over HTTP.
 */
public class WebSocketTwoUserChatTest extends AbstractTwoUserChatTest {

    @Test
    public void bothUsersCanOpenRoomPage() {
        Response aliceRoom = GET("/websocket/room?user=Alice");
        assertIsOk(aliceRoom);
        assertContentType("text/html", aliceRoom);
        assertContentMatch(".*You are now chatting as Alice.*", aliceRoom);

        Response bobRoom = GET("/websocket/room?user=Bob");
        assertIsOk(bobRoom);
        assertContentType("text/html", bobRoom);
        assertContentMatch(".*You are now chatting as Bob.*", bobRoom);
    }

    @Test
    public void twoUsersChatScenario() throws Exception {
        ChatRoom room = ChatRoom.get();

        // 1. Alice connects — exactly ONE join event in her stream
        EventStream<ChatRoom.Event> aliceStream = room.join("Alice");
        ChatRoom.Event aliceJoin = aliceStream.nextEvent().get();
        assertTrue(aliceJoin instanceof ChatRoom.Join);
        assertEquals("Alice", ((ChatRoom.Join) aliceJoin).user);

        // 2. Alice sends a message — it appears exactly ONCE
        room.say("Alice", "Hello, Bob!");
        ChatRoom.Event aliceMessage = aliceStream.nextEvent().get();
        assertTrue(aliceMessage instanceof ChatRoom.Message);
        assertEquals("Alice", ((ChatRoom.Message) aliceMessage).user);
        assertEquals("Hello, Bob!", ((ChatRoom.Message) aliceMessage).text);

        // 3. Bob connects — Alice's stream sees Bob's join, Bob's stream
        //    replays the whole previous history
        EventStream<ChatRoom.Event> bobStream = room.join("Bob");

        ChatRoom.Event bobJoinOnAlice = aliceStream.nextEvent().get();
        assertTrue(bobJoinOnAlice instanceof ChatRoom.Join);
        assertEquals("Bob", ((ChatRoom.Join) bobJoinOnAlice).user);

        ChatRoom.Event bobSeesAliceJoin = bobStream.nextEvent().get();
        assertTrue(bobSeesAliceJoin instanceof ChatRoom.Join);
        assertEquals("Alice", ((ChatRoom.Join) bobSeesAliceJoin).user);
        ChatRoom.Event bobSeesMessage = bobStream.nextEvent().get();
        assertTrue(bobSeesMessage instanceof ChatRoom.Message);
        assertEquals("Hello, Bob!", ((ChatRoom.Message) bobSeesMessage).text);
        ChatRoom.Event bobSeesOwnJoin = bobStream.nextEvent().get();
        assertTrue(bobSeesOwnJoin instanceof ChatRoom.Join);
        assertEquals("Bob", ((ChatRoom.Join) bobSeesOwnJoin).user);

        // 4. Bob sends a message — it appears exactly ONCE in both streams
        room.say("Bob", "Hi, Alice!");
        ChatRoom.Event bobMessageOnAlice = aliceStream.nextEvent().get();
        assertTrue(bobMessageOnAlice instanceof ChatRoom.Message);
        assertEquals("Hi, Alice!", ((ChatRoom.Message) bobMessageOnAlice).text);
        ChatRoom.Event bobMessageOnBob = bobStream.nextEvent().get();
        assertTrue(bobMessageOnBob instanceof ChatRoom.Message);
        assertEquals("Hi, Alice!", ((ChatRoom.Message) bobMessageOnBob).text);

        // 5. Alice leaves — Bob's stream sees the leave event
        room.leave("Alice");
        ChatRoom.Event aliceLeave = bobStream.nextEvent().get();
        assertTrue(aliceLeave instanceof ChatRoom.Leave);
        assertEquals("Alice", ((ChatRoom.Leave) aliceLeave).user);
    }

}