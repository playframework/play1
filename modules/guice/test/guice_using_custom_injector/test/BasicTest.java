import org.junit.*;
import java.util.*;
import play.test.*;
import models.*;
import notifiers.*;
public class BasicTest extends UnitTest {

    @Test
    public void testMailerItShouldPass() {
        Mails.welcome();
    }

}
