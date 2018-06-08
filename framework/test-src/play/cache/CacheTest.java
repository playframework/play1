package play.cache;

import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class CacheTest {
    @Test
    public void clearCallsImplClear() {
        Cache.cacheImpl = mock(CacheImpl.class);
        Cache.clear();
        verify(Cache.cacheImpl).clear();
    }

    @Test
    public void clearIsNullSafe_ifImplIsNotInitializedYet() {
        Cache.cacheImpl = null;
        Cache.clear();
    }
}