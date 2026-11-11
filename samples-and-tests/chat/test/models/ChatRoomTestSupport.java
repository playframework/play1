package models;

import play.libs.F.IndexedEvent;

/**
 * Test support to reset the singleton {@link ChatRoom} between tests.
 *
 * ChatRoom.instance is package-private, so functional tests (which live in the
 * default package) cannot reset it directly. This class lives in the same
 * package and exposes a public helper.
 */
public class ChatRoomTestSupport {

    public static void reset() {
        ChatRoom.instance = null;
        IndexedEvent.resetIdGenerator();
    }

}