import org.junit.jupiter.api.BeforeEach;

import play.test.FunctionalTest;

import models.ChatRoomTestSupport;

/**
 * Shared base for the two-user chat functional tests. Play's test engine
 * ignores abstract FunctionalTest subclasses, so this class is never run as a
 * test itself.
 */
public abstract class AbstractTwoUserChatTest extends FunctionalTest {

    @BeforeEach
    public void resetChatRoom() {
        ChatRoomTestSupport.reset();
    }

    protected int countOccurrences(String content, String needle) {
        int count = 0;
        int index = 0;
        while ((index = content.indexOf(needle, index)) != -1) {
            count++;
            index += needle.length();
        }
        return count;
    }

}