import java.io.File;

import org.junit.Test;

import play.test.UnitTest;

public class LogExistsTest extends UnitTest {

    @Test
    public void checkThatTestLogExistsTest() {
        final File testlog = new File("test.log");
        assertTrue(testlog.getAbsolutePath() + " should exists!", testlog.exists());

    }

}
