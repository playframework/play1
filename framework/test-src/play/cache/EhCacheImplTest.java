package play.cache;

import org.junit.Test;
import static org.fest.assertions.Assertions.assertThat;

public class EhCacheImplTest {

    @Test
    public void verifyThatTTLSurvivesIncrDecr() throws Exception {
        EhCacheImpl cache = EhCacheImpl.newInstance();
        cache.clear();

        String key = "EhCacheImplTest_verifyThatTTLSurvivesIncrDecr";

        int expiration = 1;

        cache.add(key, 1, expiration);
        Thread.sleep(100);
        cache.incr(key, 4);

        Thread.sleep(100);
        cache.decr(key, 3);

        Thread.sleep(950);
        assertThat(cache.get(key)).isEqualTo(2L);

        //no make sure it disappear after the 1 sec + 200 mils
        Thread.sleep(150);
        assertThat(cache.get(key)).isNull();

    }
}
