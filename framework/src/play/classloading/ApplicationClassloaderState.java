package play.classloading;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Each unique instance of this class represent a State of the ApplicationClassloader.
 * When some classes is reloaded, them the ApplicationClassloader get a new state.
 *
 * This makes it easy for other parts of Play to cache stuff based on the
 * the current State of the ApplicationClassloader..
 *
 * They can store the reference to the current state, then later, before reading from cache,
 * they could check if the state of the ApplicationClassloader has changed..
 */
public class ApplicationClassloaderState {
    private static AtomicLong nextStateValue = new AtomicLong();

    private final long currentStateValue = nextStateValue.getAndIncrement();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ApplicationClassloaderState that = (ApplicationClassloaderState) o;

        if (currentStateValue != that.currentStateValue) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return (int) (currentStateValue ^ (currentStateValue >>> 32));
    }
}
