package play.test;

import play.db.jpa.JPA;
import play.exceptions.UnexpectedException;

public class BaseTest extends org.junit.Assert {

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
    public void clearJPASession() {
        JPA.em().flush();
        JPA.em().clear();
    }
    
}
