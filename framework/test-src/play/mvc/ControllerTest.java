package play.mvc;

import org.junit.Test;
import static org.junit.Assert.*;
import static play.mvc.Controller.isFrameworkClass;

public class ControllerTest{
    @Test
    public void jdkAndPlayClassesShouldNeverBeenCheckedForEnhancement() {
        assertTrue(isFrameworkClass("sun.blah"));
        assertTrue(isFrameworkClass("jdk.base"));
        assertTrue(isFrameworkClass("play.foo"));
        assertFalse(isFrameworkClass("com.bar"));
    }
}