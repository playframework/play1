package play.db.jpa;

import org.junit.Before;
import org.junit.Test;

import java.io.Serializable;

import static org.junit.Assert.*;

/**
 * Unit tests for HibernateInterceptor.
 *
 * Guards Phase 1E (opt-in dirty checking) and Phase 2B (Hibernate 6 upgrade).
 * The interceptor is the mechanism by which Play's explicit-save model works:
 * findDirty() blocks Hibernate from flushing unless willBeSaved is true, and
 * the collection callbacks (onCollectionUpdate/Recreate/Remove) do the same for
 * collection changes.
 */
public class HibernateInterceptorTest {

    private HibernateInterceptor interceptor;

    /** Minimal concrete JPABase subclass — no database, no fields, just the flag. */
    private static class TestEntity extends JPABase {
        @Override public void _save() {}
        @Override public void _delete() {}
        @Override public Object _key() { return null; }
    }

    @Before
    public void setUp() {
        interceptor = new HibernateInterceptor();
    }

    // -------------------------------------------------------------------------
    // findDirty()
    // -------------------------------------------------------------------------

    @Test
    public void findDirty_returnsEmptyArray_whenJPABaseWillNotBeSaved() {
        TestEntity entity = new TestEntity();
        entity.willBeSaved = false;

        int[] result = interceptor.findDirty(entity, null, null, null, null, null);

        assertNotNull("expected non-null (empty array) to block flush", result);
        assertEquals("expected empty array to block flush", 0, result.length);
    }

    @Test
    public void findDirty_returnsNull_whenJPABaseWillBeSaved() {
        TestEntity entity = new TestEntity();
        entity.willBeSaved = true;

        int[] result = interceptor.findDirty(entity, null, null, null, null, null);

        // null means "defer to Hibernate's own dirty check"
        assertNull("expected null to let Hibernate decide", result);
    }

    @Test
    public void findDirty_returnsNull_forNonJPABaseObject() {
        Object plainObject = new Object();

        int[] result = interceptor.findDirty(plainObject, null, null, null, null, null);

        assertNull("non-JPABase objects should be deferred to Hibernate", result);
    }

    // -------------------------------------------------------------------------
    // onSave() / afterTransactionCompletion()
    // -------------------------------------------------------------------------

    @Test
    public void onSave_storesEntityInThreadLocal() {
        TestEntity entity = new TestEntity();

        interceptor.onSave(entity, (Serializable) null, null, null, null);

        assertSame("entity should be stored for collection callbacks", entity, interceptor.entities.get());
    }

    @Test
    public void afterTransactionCompletion_clearsEntityThreadLocal() {
        TestEntity entity = new TestEntity();
        interceptor.onSave(entity, (Serializable) null, null, null, null);
        assertNotNull(interceptor.entities.get()); // sanity

        interceptor.afterTransactionCompletion(null);

        assertNull("ThreadLocal should be cleared after transaction", interceptor.entities.get());
    }

    @Test
    public void entities_threadLocal_isNullByDefault() {
        // Fresh interceptor should have no entity stored
        assertNull(interceptor.entities.get());
    }
}
