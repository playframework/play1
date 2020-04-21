package play.mvc;

import org.junit.Test;
import static org.junit.Assert.*;
import static play.mvc.Controller.shouldBeCheckedForEnhancement;

public class ControllerTest{
    @Test
    public void jdkAndPlayClassesShouldNeverBeenCheckedForEnhancement() {
        assertTrue(shouldBeCheckedForEnhancement("sun.blah"));
        assertTrue(shouldBeCheckedForEnhancement("play.foo"));
        assertFalse(shouldBeCheckedForEnhancement("com.bar"));
    }
}