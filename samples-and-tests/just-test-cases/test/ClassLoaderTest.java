import org.junit.Test;
import play.Play;
import play.PlayPlugin;
import play.cache.Cache;
import play.plugins.PluginCollection;
import play.test.UnitTest;
import plugins.ClassLoaderTestPlugin;

import java.lang.reflect.Field;

public class ClassLoaderTest extends UnitTest {

    // Play.plugin(ClassLoaderTestPlugin.class) does not work because the plugin reloads
    private ClassLoader getLoader() {
        PlayPlugin found = null;
        for (PlayPlugin p: Play.pluginCollection.getEnabledPlugins()) {
            if (p.getClass().getName().equals("plugins.ClassLoaderTestPlugin")) {
                found = p;
                break;
            }
        }
        Field contextClassLoader = null;
        try {
            contextClassLoader = found.getClass().getField("contextClassLoader");
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
        try {
            return (ClassLoader) contextClassLoader.get(found);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testClassLoader() throws ClassNotFoundException {
        assertEquals(Play.classloader, getLoader());
        Play.start();
        assertEquals(Play.classloader, getLoader());
        Play.start();
        assertEquals(Play.classloader, getLoader());
    }
}
