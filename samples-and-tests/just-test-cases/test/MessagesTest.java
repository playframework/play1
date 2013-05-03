
import play.*;
import org.junit.Test;
import play.test.UnitTest;
import play.i18n.Messages;

public class MessagesTest extends UnitTest {

    @Test
    public void testIncludeMessaes() {
        assertEquals("Username:", Messages.get("login.username"));  
        assertEquals("Hello MessagesTest you are log in.", Messages.get("login.success", "MessagesTest"));     
    }
}