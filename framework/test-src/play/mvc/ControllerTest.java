package play.mvc;

import org.junit.Test;
import static org.junit.Assert.*;
import static play.mvc.Controller.shouldNotBeCheckedForEnhancement;

public class ControllerTest{
    @Test
    public void jdkAndPlayClassesShouldNeverBeenCheckedForEnhancement() {
        assertTrue(shouldNotBeCheckedForEnhancement("sun.blah"));
        assertTrue(shouldNotBeCheckedForEnhancement("jdk.internal"));
        assertTrue(shouldNotBeCheckedForEnhancement("play.foo"));
        assertFalse(shouldNotBeCheckedForEnhancement("model.user"));
        assertFalse(shouldNotBeCheckedForEnhancement("org.foo"));
        assertFalse(shouldNotBeCheckedForEnhancement("com.bar"));
    }
}