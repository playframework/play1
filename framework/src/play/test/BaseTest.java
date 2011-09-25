package play.test;

import org.junit.Rule;
import org.junit.runner.RunWith;
import play.db.DB;
import play.db.DBConfig;
import play.db.jpa.JPA;
import play.db.jpa.JPAConfig;
import play.exceptions.UnexpectedException;

import javax.persistence.EntityManager;

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
        for (DBConfig dbConfig : DB.getDBConfigs()) {
            JPAConfig jpaConfig = JPA.getJPAConfig(dbConfig.getDBConfigName(), true);
            if (jpaConfig != null) {
                EntityManager em = jpaConfig.getJPAContext().em();
                em.flush();
                em.clear();
            }
        }

    }

}
