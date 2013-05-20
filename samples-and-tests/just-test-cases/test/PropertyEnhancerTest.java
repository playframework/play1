import java.lang.reflect.Method;

import models.PropertyEnhancerModel;

import org.junit.Test;

import play.classloading.enhancers.PropertiesEnhancer.PlayPropertyAccessor;
import play.test.UnitTest;

public class PropertyEnhancerTest extends UnitTest {

    @Test
    public void checkForPlayFrameworkEnhancerMethods() throws Exception {
        PropertyEnhancerModel model = new PropertyEnhancerModel();
        Method getter = model.getClass().getMethod("getText");
        assertFalse(getter.isAnnotationPresent(PlayPropertyAccessor.class));
        Method setter = model.getClass().getMethod("setText", String.class);
        assertTrue(setter.isAnnotationPresent(PlayPropertyAccessor.class));
    }

    @Test
    public void checkGetterCreatedForFinal() throws Exception {
        PropertyEnhancerModel model = new PropertyEnhancerModel();
        Method getter = model.getClass().getMethod("getFinalText");
        assertNotNull(getter);
        try {
            Method setter = model.getClass().getMethod("setFinalText", String.class);
            fail("Expected no setter method");
        } catch (NoSuchMethodException e) {
            // expected
        }
    }

    @Test
    public void checkNothingCreatedForStatic() throws Exception {
        PropertyEnhancerModel model = new PropertyEnhancerModel();
        try {
            Method setter = model.getClass().getMethod("getStaticText", String.class);
            fail("Expected no getter method");
        } catch (NoSuchMethodException e) {
            // expected
        }
        try {
            Method setter = model.getClass().getMethod("setStaticText", String.class);
            fail("Expected no setter method");
        } catch (NoSuchMethodException e) {
            // expected
        }
    }

    @Test
    public void checkNothingCreatedForPrivate() throws Exception {
        PropertyEnhancerModel model = new PropertyEnhancerModel();
        try {
            Method setter = model.getClass().getMethod("getPrivateText", String.class);
            fail("Expected no getter method");
        } catch (NoSuchMethodException e) {
            // expected
        }
        try {
            Method setter = model.getClass().getMethod("setPrivateText", String.class);
            fail("Expected no setter method");
        } catch (NoSuchMethodException e) {
            // expected
        }
    }

}
