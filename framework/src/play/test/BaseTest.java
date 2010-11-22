package play.test;

import org.junit.Rule;
import org.junit.runner.RunWith;
import play.db.jpa.JPA;
import play.exceptions.UnexpectedException;

@RunWith(PlayJUnitRunner.class)
public class BaseTest extends org.junit.Assert {

    @Rule
    public PlayJUnitRunner.StartPlay startPlayBeforeTests = PlayJUnitRunner.StartPlay.rule();

    /**
     * Pause the current thread
     */
    public void pause(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            throw new UnexpectedException(ex);
        }
    }
    
    /**
     * Flush and clear the JPA session
     */
    @Deprecated
    public void clearJPASession() {
        JPA.em().flush();
        JPA.em().clear();
    }
    
}
