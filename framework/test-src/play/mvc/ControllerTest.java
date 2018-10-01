package play.mvc;

import org.junit.Test;
import static org.junit.Assert.*;
import static play.mvc.Controller.isFrameworkCode;

public class ControllerTest{
    @Test
    public void jdkAndPlayClassesShouldNeverBeenCheckedForEnhancement() {
        assertTrue(isFrameworkCode("sun.blah"));
        assertTrue(isFrameworkCode("jdk.base"));
        assertTrue(isFrameworkCode("play.foo"));
        assertFalse(isFrameworkCode("com.bar"));
    }
}