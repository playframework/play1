package play.classloading.enhancers;

import org.junit.Before;
import org.junit.Test;
import play.Play;
import play.PlayBuilder;
import play.classloading.ApplicationClasses.ApplicationClass;
import play.vfs.VirtualFile;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import static org.junit.Assert.*;

/**
 * Unit tests for PropertiesEnhancer.
 *
 * Guards Phase 2C (PropertiesEnhancer removal): these tests document the
 * current behavior so that any regression during or after removal is caught.
 *
 * Tests use compiled fixture classes from the test-src fixtures package.
 * The test ClassLoader pattern:
 *   1. Load the unenhanced .class bytes from the test classpath.
 *   2. Create an ApplicationClass and register it with Play.classes.
 *   3. Run PropertiesEnhancer.enhanceThisClass().
 *   4. Load the enhanced bytes in an isolated ClassLoader.
 *   5. Assert via reflection.
 */
public class PropertiesEnhancerTest {

    @Before
    public void setUp() {
        new PlayBuilder().build();
    }

    // -------------------------------------------------------------------------
    // Helper: load .class bytes and create an ApplicationClass
    // -------------------------------------------------------------------------

    private ApplicationClass makeAppClass(String binaryName) throws Exception {
        String resourcePath = binaryName.replace('.', '/') + ".class";
        byte[] bytes;
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            assertNotNull("fixture class not found on classpath: " + resourcePath, in);
            bytes = in.readAllBytes();
        }

        ApplicationClass appClass = new ApplicationClass(binaryName);
        appClass.enhancedByteCode = bytes;
        // VirtualFile is needed for the isScala() check inside the enhancer.
        // The file doesn't need to exist — getName() just must not end in ".scala".
        appClass.javaFile = VirtualFile.open(new File(binaryName.replace('.', '/') + ".java"));
        Play.classes.add(appClass);
        return appClass;
    }

    /** Loads the enhanced bytes into an isolated ClassLoader so we can reflect on them. */
    private Class<?> loadEnhanced(ApplicationClass appClass) throws Exception {
        // The parent classloader has already loaded the original (unenhanced) fixture class,
        // so we must override loadClass — not just findClass — to bypass the normal
        // parent-first delegation for the fixture class specifically.
        // All other classes (framework types like @PlayPropertyAccessor) are still
        // resolved via the parent so reflection works correctly.
        ClassLoader loader = new ClassLoader(getClass().getClassLoader()) {
            @Override
            public Class<?> loadClass(String name) throws ClassNotFoundException {
                if (name.equals(appClass.name)) {
                    return findClass(name);
                }
                return super.loadClass(name);
            }

            @Override
            protected Class<?> findClass(String name) throws ClassNotFoundException {
                if (name.equals(appClass.name)) {
                    byte[] b = appClass.enhancedByteCode;
                    return defineClass(name, b, 0, b.length);
                }
                throw new ClassNotFoundException(name);
            }
        };
        return loader.loadClass(appClass.name);
    }

    private void runEnhancer(ApplicationClass appClass) throws Exception {
        new PropertiesEnhancer().enhanceThisClass(appClass);
    }

    // -------------------------------------------------------------------------
    // Accessor generation
    // -------------------------------------------------------------------------

    @Test
    public void enhancer_generatesGetterForPublicField() throws Exception {
        ApplicationClass appClass = makeAppClass("play.classloading.enhancers.fixtures.SimpleBeanFixture");
        runEnhancer(appClass);

        Class<?> enhanced = loadEnhanced(appClass);
        Method getter = enhanced.getMethod("getName");
        assertNotNull("getter getName() should be generated", getter);
        assertEquals(String.class, getter.getReturnType());
    }

    @Test
    public void enhancer_generatesSetterForPublicField() throws Exception {
        ApplicationClass appClass = makeAppClass("play.classloading.enhancers.fixtures.SimpleBeanFixture");
        runEnhancer(appClass);

        Class<?> enhanced = loadEnhanced(appClass);
        Method setter = enhanced.getMethod("setName", String.class);
        assertNotNull("setter setName(String) should be generated", setter);
        assertEquals(void.class, setter.getReturnType());
    }

    @Test
    public void enhancer_generatesAccessorsForPrimitiveField() throws Exception {
        ApplicationClass appClass = makeAppClass("play.classloading.enhancers.fixtures.SimpleBeanFixture");
        runEnhancer(appClass);

        Class<?> enhanced = loadEnhanced(appClass);
        assertNotNull(enhanced.getMethod("getCount"));
        assertNotNull(enhanced.getMethod("setCount", int.class));
    }

    @Test
    public void enhancer_generatesAccessorsForBooleanField() throws Exception {
        ApplicationClass appClass = makeAppClass("play.classloading.enhancers.fixtures.SimpleBeanFixture");
        runEnhancer(appClass);

        Class<?> enhanced = loadEnhanced(appClass);
        // PropertiesEnhancer uses get/set uniformly — no isXxx convention for booleans
        assertNotNull(enhanced.getMethod("getActive"));
        assertNotNull(enhanced.getMethod("setActive", boolean.class));
    }

    @Test
    public void enhancer_generatedGetterIsAnnotatedWithPlayPropertyAccessor() throws Exception {
        ApplicationClass appClass = makeAppClass("play.classloading.enhancers.fixtures.SimpleBeanFixture");
        runEnhancer(appClass);

        Class<?> enhanced = loadEnhanced(appClass);
        Method getter = enhanced.getMethod("getName");
        boolean hasAnnotation = java.util.Arrays.stream(getter.getDeclaredAnnotations())
                .anyMatch(a -> a.annotationType().getName()
                        .equals("play.classloading.enhancers.PropertiesEnhancer$PlayPropertyAccessor"));
        assertTrue("generated getter should carry @PlayPropertyAccessor", hasAnnotation);
    }

    // -------------------------------------------------------------------------
    // Constructor generation
    // -------------------------------------------------------------------------

    @Test
    public void enhancer_addsDefaultConstructor_whenMissing() throws Exception {
        ApplicationClass appClass = makeAppClass("play.classloading.enhancers.fixtures.NoDefaultConstructorFixture");
        runEnhancer(appClass);

        Class<?> enhanced = loadEnhanced(appClass);
        // Should have a public no-arg constructor after enhancement
        java.lang.reflect.Constructor<?> ctor = enhanced.getDeclaredConstructor();
        assertNotNull(ctor);
        assertTrue(Modifier.isPublic(ctor.getModifiers()));
    }

    @Test
    public void enhancer_preservesDefaultConstructor_whenAlreadyPresent() throws Exception {
        ApplicationClass appClass = makeAppClass("play.classloading.enhancers.fixtures.SimpleBeanFixture");
        runEnhancer(appClass);

        Class<?> enhanced = loadEnhanced(appClass);
        // The implicit no-arg constructor should still be present
        assertNotNull(enhanced.getDeclaredConstructor());
    }

    // -------------------------------------------------------------------------
    // Field access rewriting (behavioral)
    // -------------------------------------------------------------------------

    @Test
    public void enhancedGetter_returnsFieldValue() throws Exception {
        ApplicationClass appClass = makeAppClass("play.classloading.enhancers.fixtures.SimpleBeanFixture");
        runEnhancer(appClass);

        Class<?> enhanced = loadEnhanced(appClass);
        Object instance = enhanced.getDeclaredConstructor().newInstance();

        // Write directly to the field, read via generated getter
        enhanced.getField("name").set(instance, "hello");
        Object value = enhanced.getMethod("getName").invoke(instance);
        assertEquals("hello", value);
    }

    @Test
    public void enhancedSetter_updatesFieldValue() throws Exception {
        ApplicationClass appClass = makeAppClass("play.classloading.enhancers.fixtures.SimpleBeanFixture");
        runEnhancer(appClass);

        Class<?> enhanced = loadEnhanced(appClass);
        Object instance = enhanced.getDeclaredConstructor().newInstance();

        // Write via generated setter, read directly from the field
        enhanced.getMethod("setName", String.class).invoke(instance, "world");
        Object value = enhanced.getField("name").get(instance);
        assertEquals("world", value);
    }
}
