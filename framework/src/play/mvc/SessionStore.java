package play.mvc;

/**
 * Implementations of session storage mechanisms.
 */
public interface SessionStore {
    void save(Scope.Session session);
    Scope.Session restore();
}
