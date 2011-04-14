import models.ModelWithLifecycleListeners;
import org.junit.Test;
import play.test.UnitTest;

/**
 * @author Manuel Bernhardt <bernhardt.manuel@gmail.com>
 */
public class JPALifecycleTest extends UnitTest {

    @Test
    public void testCreate() {
        ModelWithLifecycleListeners modelWithLifecycleListeners = new ModelWithLifecycleListeners();
        modelWithLifecycleListeners.transientValue = "created";
        assertNotSame("created", modelWithLifecycleListeners.value);
        modelWithLifecycleListeners.create();
        assertEquals("created", modelWithLifecycleListeners.value);
    }

    @Test
    public void testUpdate() {
        ModelWithLifecycleListeners modelWithLifecycleListeners = new ModelWithLifecycleListeners();
        modelWithLifecycleListeners.transientValue = "created";
        modelWithLifecycleListeners.create();
        assertEquals("created", modelWithLifecycleListeners.transientValue);
        modelWithLifecycleListeners.transientValue = "updated";
        modelWithLifecycleListeners.save();
        assertEquals("updated", modelWithLifecycleListeners.transientValue);
    }
}
