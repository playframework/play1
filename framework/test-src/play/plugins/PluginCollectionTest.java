package play.plugins;

import static java.util.Arrays.asList;
import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.io.File;
import java.net.URL;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;

import play.ConfigurationChangeWatcherPlugin;
import play.Play;
import play.PlayBuilder;
import play.PlayPlugin;
import play.data.parsing.TempFilePlugin;
import play.data.validation.ValidationPlugin;
import play.db.DBPlugin;
import play.db.Evolutions;
import play.db.jpa.JPAPlugin;
import play.i18n.MessagesPlugin;
import play.jobs.JobsPlugin;
import play.libs.WS;
import play.test.TestEngine;

public class PluginCollectionTest {

    @Before
    public void init() {
        new PlayBuilder().build();
    }

    @Test
    public void verifyLoading() {
        PluginCollection pc = new PluginCollection();
        pc.loadPlugins();

        // the following plugin-list should match the list in the file 'play.plugins'
        assertThat(pc.getEnabledPlugins()).containsExactly(pc.getPluginInstance(EnhancerPlugin.class),
                pc.getPluginInstance(ConfigurationChangeWatcherPlugin.class), pc.getPluginInstance(TempFilePlugin.class),
                pc.getPluginInstance(ValidationPlugin.class), 
                pc.getPluginInstance(DBPlugin.class), pc.getPluginInstance(play.db.DBBrowserPlugin.class), 
                pc.getPluginInstance(JPAPlugin.class),
                pc.getPluginInstance(Evolutions.class), pc.getPluginInstance(MessagesPlugin.class), pc.getPluginInstance(WS.class),
                pc.getPluginInstance(JobsPlugin.class), pc.getPluginInstance(ConfigurablePluginDisablingPlugin.class),
                pc.getPluginInstance(PlayStatusPlugin.class));
    }

    @Test
    public void verifyLoadingFromFilesWithBlankLines() throws Exception {
        // create custom PluginCollection that fakes that TestPlugin is application plugin
        PluginCollection pc = new PluginCollection() {
            @Override
            protected boolean isLoadedByApplicationClassloader(PlayPlugin plugin) {
                // return true only if This is our TestPlugin
                return plugin.getClass().equals(TestPlugin.class);
            }
        };
        // make sure we load custom play.plugins-file
        pc.play_plugins_resourceName = "play/plugins/custom-play-with-blank-lines.plugins";

        pc.loadPlugins();

        EnhancerPlugin enhancerPlugin_first_instance = pc.getPluginInstance(EnhancerPlugin.class);
        TestPlugin testPlugin_first_instance = pc.getPluginInstance(TestPlugin.class);

        assertThat(pc.getAllPlugins()).containsExactly(enhancerPlugin_first_instance, testPlugin_first_instance);

    }

    /**
     * Avoid including the same class+index twice.
     * 
     * This happened in the past under a range of circumstances, including: 1. Class path on NTFS or other case
     * insensitive file system includes play.plugins directory 2x (C:/myproject/conf;c:/myproject/conf) 2.
     * https://play.lighthouseapp.com/projects/57987/tickets/176-app-playplugins-loaded-twice-conf-on-2-classpaths
     */
    @Test
    public void skipsDuplicatePlugins() {
        PluginCollection pc = spy(new PluginCollection());
        URL resource1 = getClass().getResource("custom-play.plugins");
        URL resource2 = getClass().getResource("custom-play.duplicate.plugins");
        assertThat(resource2).isNotNull();
        when(pc.loadPlayPluginDescriptors()).thenReturn(asList(resource1, resource2));
        pc.loadPlugins();
        assertThat(pc.getAllPlugins()).containsExactly(pc.getPluginInstance(EnhancerPlugin.class), pc.getPluginInstance(TestPlugin.class));
    }

    @Test
    public void canLoadPlayPluginsFromASingleDescriptor() throws Exception {
        Play.configuration.setProperty("play.plugins.descriptor", "test-src/play/plugins/custom-play.plugins");
        PluginCollection pc = new PluginCollection();
        assertThat(pc.loadPlayPluginDescriptors()).containsExactly(new File(Play.applicationPath, "test-src/play/plugins/custom-play.plugins").toURI().toURL());
    }

    @Test
    public void canLoadPlayPluginsFromMultipleDescriptors() throws Exception {
        Play.configuration.setProperty("play.plugins.descriptor", "test-src/play/plugins/custom-play.plugins,test-src/play/plugins/custom-play.test.plugins");
        PluginCollection pc = new PluginCollection();
        assertThat(pc.loadPlayPluginDescriptors()).containsExactly(
            new File(Play.applicationPath, "test-src/play/plugins/custom-play.plugins").toURI().toURL(),
            new File(Play.applicationPath, "test-src/play/plugins/custom-play.test.plugins").toURI().toURL()
        );
    }

    @Test
    public void verifyReloading() throws Exception {
        // create custom PluginCollection that fakes that TestPlugin is application plugin
        PluginCollection pc = new PluginCollection() {
            @Override
            protected boolean isLoadedByApplicationClassloader(PlayPlugin plugin) {
                // return true only if This is our TestPlugin
                return plugin.getClass().equals(TestPlugin.class);
            }
        };
        // make sure we load custom play.plugins-file
        pc.play_plugins_resourceName = "play/plugins/custom-play.plugins";

        pc.loadPlugins();

        EnhancerPlugin enhancerPlugin_first_instance = pc.getPluginInstance(EnhancerPlugin.class);
        TestPlugin testPlugin_first_instance = pc.getPluginInstance(TestPlugin.class);

        // the following plugin-list should match the list in the file 'play.plugins'
        assertThat(pc.getEnabledPlugins()).containsExactly(enhancerPlugin_first_instance, testPlugin_first_instance);
        assertThat(pc.getAllPlugins()).containsExactly(enhancerPlugin_first_instance, testPlugin_first_instance);

        pc.reloadApplicationPlugins();

        TestPlugin testPlugin_second_instance = pc.getPluginInstance(TestPlugin.class);

        assertThat(pc.getPluginInstance(EnhancerPlugin.class)).isEqualTo(enhancerPlugin_first_instance);
        assertThat(testPlugin_second_instance).isNotEqualTo(testPlugin_first_instance);

    }

    @SuppressWarnings({ "deprecation" })
    @Test
    public void verifyUpdatePlayPluginsList() {
        assertThat(Play.plugins).isEmpty();

        PluginCollection pc = new PluginCollection();
        pc.loadPlugins();

        assertThat(Play.plugins).containsExactly(pc.getEnabledPlugins().toArray());

    }

    @SuppressWarnings({ "deprecation" })
    @Test
    public void verifyThatDisablingPluginsTheOldWayStillWorks() {
        PluginCollection pc = new PluginCollection();

        PlayPlugin legacyPlugin = new LegacyPlugin();

        pc.addPlugin(legacyPlugin);
        pc.addPlugin(new TestPlugin());

        pc.initializePlugin(legacyPlugin);

        assertThat(pc.getEnabledPlugins()).containsExactly(legacyPlugin);

        // make sure Play.plugins-list is still correct
        assertThat(Play.plugins).isEqualTo(pc.getEnabledPlugins());

    }

    @Test
    public void verifyThatPluginsCanAddUnitTests() {
        PluginCollection pc = new PluginCollection();
        Play.pluginCollection = pc;

        assertThat(TestEngine.allUnitTests()).isEmpty();
        assertThat(TestEngine.allFunctionalTests()).isEmpty();

        PluginWithTests p1 = new PluginWithTests();
        PluginWithTests2 p2 = new PluginWithTests2();
        pc.addPlugin(p1);
        pc.addPlugin(p2);

        pc.initializePlugin(p1);
        pc.initializePlugin(p2);

        assertThat(TestEngine.allUnitTests()).contains(PluginUnit.class, PluginUnit2.class);
        assertThat(TestEngine.allFunctionalTests()).contains(PluginFunc.class, PluginFunc2.class);
    }
}

class LegacyPlugin extends PlayPlugin {

    @SuppressWarnings({ "deprecation" })
    @Override
    public void onLoad() {
        // find TestPlugin in Play.plugins-list and remove it to disable it
        PlayPlugin pluginToRemove = null;
        for (PlayPlugin pp : Play.plugins) {
            if (pp.getClass().equals(TestPlugin.class)) {
                pluginToRemove = pp;
                break;
            }
        }
        Play.plugins.remove(pluginToRemove);
    }

}

class PluginWithTests extends PlayPlugin {

    @Override
    public Collection<Class> getUnitTests() {
        return asList(new Class[] { PluginUnit.class });
    }

    @Override
    public Collection<Class> getFunctionalTests() {
        return asList(new Class[] { PluginFunc.class });
    }
}

class PluginWithTests2 extends PlayPlugin {

    @Override
    public Collection<Class> getUnitTests() {
        return asList(new Class[] { PluginUnit2.class });
    }

    @Override
    public Collection<Class> getFunctionalTests() {
        return asList(new Class[] { PluginFunc2.class });
    }
}

class PluginUnit {
}

class PluginUnit2 {
}

class PluginFunc {
}

class PluginFunc2 {
}
