package models;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import play.libs.F.EventStream;
import play.libs.F.IndexedEvent;
import play.test.UnitTest;

public class ChatRoomTest extends UnitTest {

    @BeforeEach
    public void resetChatRoom() {
        ChatRoom.instance = null;
        play.libs.F.IndexedEvent.resetIdGenerator();
    }

    @Test
    public void getReturnsSingleton() {
        assertSame(ChatRoom.get(), ChatRoom.get());
    }

    @Test
    public void joinPublishesJoinEvent() {
        ChatRoom room = ChatRoom.get();
        room.join("Bob");

        List<ChatRoom.Event> archive = room.archive();
        assertEquals(1, archive.size());
        ChatRoom.Join join = (ChatRoom.Join) archive.get(0);
        assertEquals("join", join.type);
        assertEquals("Bob", join.user);
    }

    @Test
    public void joinReturnsEventStreamWithPastEvents() throws Exception {
        ChatRoom room = ChatRoom.get();
        room.join("Bob");

        EventStream<ChatRoom.Event> stream = room.join("Guillaume");
        CompletableFuture<ChatRoom.Event> next = stream.nextEvent();
        assertTrue(next.isDone());
        assertEquals("Bob", ((ChatRoom.Join) next.get()).user);
    }

    @Test
    public void leavePublishesLeaveEvent() {
        ChatRoom room = ChatRoom.get();
        room.join("Bob");
        room.leave("Bob");

        List<ChatRoom.Event> archive = room.archive();
        assertEquals(2, archive.size());
        ChatRoom.Leave leave = (ChatRoom.Leave) archive.get(1);
        assertEquals("leave", leave.type);
        assertEquals("Bob", leave.user);
    }

    @Test
    public void sayPublishesMessageEvent() {
        ChatRoom room = ChatRoom.get();
        room.say("Bob", "Hello world");

        List<ChatRoom.Event> archive = room.archive();
        assertEquals(1, archive.size());
        ChatRoom.Message message = (ChatRoom.Message) archive.get(0);
        assertEquals("message", message.type);
        assertEquals("Bob", message.user);
        assertEquals("Hello world", message.text);
    }

    @Test
    public void sayIgnoresNullAndBlankMessages() {
        ChatRoom room = ChatRoom.get();
        room.say("Bob", null);
        room.say("Bob", "");
        room.say("Bob", "   ");

        assertTrue(room.archive().isEmpty());
    }

    @Test
    public void nextMessagesReturnsEventsAfterLastReceived() throws Exception {
        ChatRoom room = ChatRoom.get();
        room.join("Bob");
        room.say("Bob", "Hello");

        CompletableFuture<List<IndexedEvent<ChatRoom.Event>>> p = room.nextMessages(1);
        assertTrue(p.isDone());
        List<IndexedEvent<ChatRoom.Event>> events = p.get();
        assertEquals(1, events.size());
        assertEquals("message", events.get(0).data.type);
        assertEquals("Hello", ((ChatRoom.Message) events.get(0).data).text);
    }

    @Test
    public void nextMessagesResolvesWhenNewEventArrives() throws Exception {
        ChatRoom room = ChatRoom.get();
        room.join("Bob");

        CompletableFuture<List<IndexedEvent<ChatRoom.Event>>> p = room.nextMessages(1);
        assertFalse(p.isDone());

        room.say("Bob", "Hello");
        assertTrue(p.isDone());

        List<IndexedEvent<ChatRoom.Event>> events = p.get();
        assertEquals(1, events.size());
        assertEquals("message", events.get(0).data.type);
        assertEquals("Hello", ((ChatRoom.Message) events.get(0).data).text);
    }

    @Test
    public void archiveKeepsOnlyLastHundredEvents() {
        ChatRoom room = ChatRoom.get();
        for (int i = 1; i <= 110; i++) {
            room.say("Bob", "message " + i);
        }

        List<ChatRoom.Event> archive = room.archive();
        assertEquals(100, archive.size());
        ChatRoom.Message first = (ChatRoom.Message) archive.get(0);
        assertEquals("message 11", first.text);
    }

    @Test
    public void eventIdsAreMonotonicallyIncreasing() throws Exception {
        ChatRoom room = ChatRoom.get();
        room.join("Bob");
        room.say("Bob", "Hello");

        CompletableFuture<List<IndexedEvent<ChatRoom.Event>>> p = room.nextMessages(0);
        List<IndexedEvent<ChatRoom.Event>> events = p.get();
        assertEquals(2, events.size());
        assertTrue(events.get(0).id < events.get(1).id);
    }

}