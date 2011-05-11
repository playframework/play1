import java.lang.reflect.Method;

import models.PropertyEnhancerModel;

import org.junit.Test;

import play.test.UnitTest;

public class PropertyEnhancerTest extends UnitTest {

    @Test
    public void checkForSyntheticMethods() throws Exception {
        PropertyEnhancerModel model = new PropertyEnhancerModel();
        Method getter = model.getClass().getMethod("getText");
        assertFalse(getter.isSynthetic());
        Method setter = model.getClass().getMethod("setText", String.class);
        assertTrue(setter.isSynthetic());
    }

}
