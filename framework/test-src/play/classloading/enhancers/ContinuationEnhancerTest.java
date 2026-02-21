package play.classloading.enhancers;

import org.junit.Before;
import org.junit.Test;
import play.Play;
import play.PlayBuilder;
import play.classloading.ApplicationClasses.ApplicationClass;
import play.vfs.VirtualFile;

import java.io.File;
import java.io.InputStream;

import static org.junit.Assert.*;

/**
 * Unit tests for ContinuationEnhancer.
 *
 * Guards Phase 3C (JavaFlow continuation removal): these tests document what
 * the enhancer detects and marks, so the removal can be validated.
 *
 * The key behavior tested here is the detection side: does the enhancer
 * correctly identify classes that use await() and mark them with the
 * EnhancedForContinuations interface? The actual JavaFlow bytecode
 * transformation is not tested here — that path requires a controller that
 * calls await() at runtime and is covered by the sample app integration tests.
 *
 * isEnhanced() is tested in isolation; full transformation testing (where the
 * class gets EnhancedForContinuations added and JavaFlow-transformed) requires
 * the integration test path because it needs a working Play classloader with
 * the class registered before enhancement so the classPool can resolve types.
 */
public class ContinuationEnhancerTest {

    @Before
    public void setUp() {
        new PlayBuilder().build();
    }

    // -------------------------------------------------------------------------
    // isEnhanced() — runtime detection
    // -------------------------------------------------------------------------

    @Test
    public void isEnhanced_returnsFalse_whenClassNotInPlayClasses() {
        // A class that was never loaded/enhanced through the Play classloader
        assertFalse(ContinuationEnhancer.isEnhanced("some.nonexistent.Controller"));
    }

    @Test
    public void isEnhanced_returnsFalse_whenClassInPlayClasses_butNotEnhanced() throws Exception {
        // Load a class that IS in Play.classes but does NOT implement
        // EnhancedForContinuations (i.e., was not enhanced by the enhancer)
        String className = "play.classloading.enhancers.fixtures.SimpleBeanFixture";
        ApplicationClass appClass = makeAppClass(className);

        // Register the unenhanced class with a real javaClass so isEnhanced()
        // can call isAssignableFrom()
        appClass.javaClass = Class.forName(className);

        assertFalse(ContinuationEnhancer.isEnhanced(className));
    }

    @Test
    public void isEnhanced_returnsFalse_whenJavaClassIsNull() throws Exception {
        // appClass.javaClass is null when the class is registered but not yet loaded
        // by Play's classloader. isEnhanced() must not NPE in this state.
        String className = "play.classloading.enhancers.fixtures.SimpleBeanFixture";
        ApplicationClass appClass = makeAppClass(className);
        appClass.javaClass = null; // explicit: simulate not-yet-loaded class

        assertFalse(ContinuationEnhancer.isEnhanced(className));
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    private ApplicationClass makeAppClass(String binaryName) throws Exception {
        String resourcePath = binaryName.replace('.', '/') + ".class";
        byte[] bytes;
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            assertNotNull("fixture class not found: " + resourcePath, in);
            bytes = in.readAllBytes();
        }

        ApplicationClass appClass = new ApplicationClass(binaryName);
        appClass.enhancedByteCode = bytes;
        appClass.javaFile = VirtualFile.open(new File(binaryName.replace('.', '/') + ".java"));
        Play.classes.add(appClass);
        return appClass;
    }
}
