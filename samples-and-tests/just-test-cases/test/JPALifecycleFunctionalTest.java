import models.ModelWithLifecycleListeners;
import org.junit.Test;
import play.mvc.Http;
import play.test.FunctionalTest;

/**
 * @author Manuel Bernhardt <bernhardt.manuel@gmail.com>
 */
public class JPALifecycleFunctionalTest extends FunctionalTest {

    @Test
    public void testCreate() {
        ModelWithLifecycleListeners modelWithLifecycleListeners = create();
        assertNotNull(modelWithLifecycleListeners);
        assertEquals(modelWithLifecycleListeners.value, "created");
    }

    private ModelWithLifecycleListeners create() {
        Http.Response response = GET("/jpalifecycle/create");
        assertIsOk(response);
        String sid = getContent(response);
        Long lid = Long.parseLong(sid);
        return ModelWithLifecycleListeners.findById(lid);
    }

    @Test
    public void testUpdate() {
        ModelWithLifecycleListeners b = create();
        Http.Response response = GET("/jpalifecycle/update?id="+b.id);
        assertIsOk(response);
        String sid = getContent(response);
        Long lid = Long.parseLong(sid);
        ModelWithLifecycleListeners modelWithLifecycleListeners = ModelWithLifecycleListeners.findById(lid);
        assertNotNull(modelWithLifecycleListeners);
        assertEquals("updated", modelWithLifecycleListeners.value);
    }


}
