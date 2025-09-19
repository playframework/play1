package play.mvc;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static play.mvc.Controller.shouldBeCheckedForEnhancement;

public class ControllerTest{
    @Test
    public void jdkAndPlayClassesShouldNeverBeenCheckedForEnhancement() {
        assertTrue(shouldBeCheckedForEnhancement("sun.blah"));
        assertTrue(shouldBeCheckedForEnhancement("play.foo"));
        assertFalse(shouldBeCheckedForEnhancement("com.bar"));
    }
}