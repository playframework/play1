package play.test;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.ExtendWith;

import play.db.jpa.JPA;
import play.exceptions.UnexpectedException;

@ExtendWith(PlayJUnitExtension.class)
public abstract class BaseTest extends Assertions {

    /**
     * Pause the current thread
     * 
     * @param millis
     *            Time in milliseconds
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