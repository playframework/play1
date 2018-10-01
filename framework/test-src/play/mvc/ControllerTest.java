package play.mvc;

import org.junit.Test;
import static org.junit.Assert.*;
import static play.mvc.Controller.shouldNotBeEnhanced;

public class ControllerTest{
    @Test
    public void jdkAndPlayClassesShouldNeverBeenCheckedForEnhancement() {
        assertTrue(shouldNotBeEnhanced("sun.blah"));
        assertTrue(shouldNotBeEnhanced("jdk.base"));
        assertTrue(shouldNotBeEnhanced("play.foo"));
        assertFalse(shouldNotBeEnhanced("com.bar"));
    }
}