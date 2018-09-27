package play.mvc;

import org.junit.Test;
import static org.junit.Assert.*;
import static play.mvc.Controller.enhancementCheckComplete;

public class ControllerTest{
    @Test
    public void jdkAndPlayClassesShouldNeverBeenCheckedForEnhancement() {
        assertTrue(enhancementCheckComplete("sun.blah"));
        assertTrue(enhancementCheckComplete("jdk.base"));
        assertTrue(enhancementCheckComplete("play.foo"));
        assertFalse(enhancementCheckComplete("com.bar"));
    }
}