package play.test;

import play.db.jpa.JPA;
import play.exceptions.UnexpectedException;

public class BaseTest extends org.junit.Assert {

    public void pause(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            throw new UnexpectedException(ex);
        }
    }
    
    public void clearJPASession() {
        JPA.em().flush();
        JPA.em().clear();
    }
    
}
