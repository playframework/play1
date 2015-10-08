package play.plugins;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;

import play.*;
import play.data.parsing.TempFilePlugin;
import play.data.validation.ValidationPlugin;
import play.db.DBPlugin;
import play.db.Evolutions;
import play.db.jpa.JPAPlugin;
import play.i18n.MessagesPlugin;
import play.jobs.JobsPlugin;
import play.libs.WS;
import play.test.TestEngine;
import static org.fest.assertions.Assertions.assertThat;

/**
 * Created by IntelliJ IDEA.
 * User: mortenkjetland
 * Date: 3/3/11
 * Time: 12:14 AM
 * To change this template use File | Settings | File Templates.
 */
public class PluginCollectionTest {
    
    @Before
    public void init(){
        new PlayBuilder().build();
    }

    @Test
    public void verifyLoading() {
        PluginCollection pc = new PluginCollection();
        pc.loadPlugins();

        //the following plugin-list should match the list in the file 'play.plugins'
        assertThat(pc.getEnabledPlugins()).containsExactly(
                pc.getPluginInstance(CorePlugin.class),
                pc.getPluginInstance(ConfigurationChangeWatcherPlugin.class),
                pc.getPluginInstance(TempFilePlugin.class),
                pc.getPluginInstance(ValidationPlugin.class),
                pc.getPluginInstance(DBPlugin.class),
                pc.getPluginInstance(JPAPlugin.class),
                pc.getPluginInstance(Evolutions.class),
                pc.getPluginInstance(MessagesPlugin.class),
                pc.getPluginInstance(WS.class),
                pc.getPluginInstance(JobsPlugin.class),
                pc.getPluginInstance(ConfigurablePluginDisablingPlugin.class));
    }

    @Test
    public void verifyLoadingFromFilesWithBlankLines() throws Exception {
        //create custom PluginCollection that fakes that TestPlugin is application plugin
        PluginCollection pc = new PluginCollection(){
            @Override
            protected boolean isLoadedByApplicationClassloader(PlayPlugin plugin) {
                //return true only if This is our TestPlugin
                return plugin.getClass().equals( TestPlugin.class);
            }
        };
        //make sure we load custom play.plugins-file
        pc.play_plugins_resourceName = "play/plugins/custom-play-with-blank-lines.plugins";

        pc.loadPlugins();

        PlayPlugin corePlugin_first_instance = pc.getPluginInstance(CorePlugin.class);
        PlayPlugin testPlugin_first_instance = pc.getPluginInstance(TestPlugin.class);

        assertThat(pc.getAllPlugins()).containsExactly(
                corePlugin_first_instance,
                testPlugin_first_instance);

    }

    @Test
    public void verifyReloading() throws Exception{
        //create custom PluginCollection that fakes that TestPlugin is application plugin
        PluginCollection pc = new PluginCollection(){
            @Override
            protected boolean isLoadedByApplicationClassloader(PlayPlugin plugin) {
                //return true only if This is our TestPlugin
                return plugin.getClass().equals( TestPlugin.class);
            }
        };
        //make sure we load custom play.plugins-file
        pc.play_plugins_resourceName = "play/plugins/custom-play.plugins";

        pc.loadPlugins();

        PlayPlugin corePlugin_first_instance = pc.getPluginInstance(CorePlugin.class);
        PlayPlugin testPlugin_first_instance = pc.getPluginInstance(TestPlugin.class);

        //the following plugin-list should match the list in the file 'play.plugins'
        assertThat(pc.getEnabledPlugins()).containsExactly(
                corePlugin_first_instance,
                testPlugin_first_instance);
        assertThat(pc.getAllPlugins()).containsExactly(
                corePlugin_first_instance,
                testPlugin_first_instance);

        pc.reloadApplicationPlugins();

        PlayPlugin testPlugin_second_instance = pc.getPluginInstance(TestPlugin.class);

        assertThat(pc.getPluginInstance(CorePlugin.class)).isEqualTo( corePlugin_first_instance);
        assertThat(testPlugin_second_instance).isNotEqualTo( testPlugin_first_instance);

    }

    @SuppressWarnings({"deprecation"})
    @Test
    public void verifyUpdatePlayPluginsList(){
        assertThat(Play.plugins).isEmpty();

        PluginCollection pc = new PluginCollection();
        pc.loadPlugins();

        assertThat(Play.plugins).containsExactly( pc.getEnabledPlugins().toArray());


    }

    @SuppressWarnings({"deprecation"})
    @Test
    public void verifyThatDisabelingPluginsTheOldWayStillWorks(){
        PluginCollection pc = new PluginCollection();


        PlayPlugin legacyPlugin = new LegacyPlugin();

        pc.addPlugin( legacyPlugin );
        pc.addPlugin( new TestPlugin() );

        pc.initializePlugin( legacyPlugin );

        assertThat( pc.getEnabledPlugins() ).containsExactly(legacyPlugin);

        //make sure Play.plugins-list is still correct
        assertThat(Play.plugins).isEqualTo( pc.getEnabledPlugins() );

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

    @SuppressWarnings({"deprecation"})
    @Override
    public void onLoad() {
        //find TestPlugin in Play.plugins-list and remove it to disable it
        PlayPlugin pluginToRemove = null;
        for( PlayPlugin pp : Play.plugins){
            if( pp.getClass().equals( TestPlugin.class)){
                pluginToRemove = pp;
                break;
            }
        }
        Play.plugins.remove( pluginToRemove);
    }

}

class PluginWithTests extends PlayPlugin {

    @Override
    public Collection<Class> getUnitTests() {
        return Arrays.asList(new Class[]{PluginUnit.class});
    }

    @Override
    public Collection<Class> getFunctionalTests() {
        return Arrays.asList(new Class[]{PluginFunc.class});
    }
}

class PluginWithTests2 extends PlayPlugin {

    @Override
    public Collection<Class> getUnitTests() {
        return Arrays.asList(new Class[]{PluginUnit2.class});
    }

    @Override
    public Collection<Class> getFunctionalTests() {
        return Arrays.asList(new Class[]{PluginFunc2.class});
    }
}

class PluginUnit {}
class PluginUnit2 {}
class PluginFunc {}
class PluginFunc2 {}
