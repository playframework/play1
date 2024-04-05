package play.test;

import org.junit.Rule;
import org.junit.runner.RunWith;

import play.exceptions.UnexpectedException;

@RunWith(PlayJUnitRunner.class)
public abstract class BaseTest extends org.junit.Assert {

    @Rule
    public PlayJUnitRunner.StartPlay startPlayBeforeTests = PlayJUnitRunner.StartPlay.rule();

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

}