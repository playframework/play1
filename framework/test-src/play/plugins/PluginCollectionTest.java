package play.plugins;

import org.junit.Test;
import play.CorePlugin;
import play.Play;
import play.PlayBuilder;
import play.PlayPlugin;
import play.classloading.ApplicationClasses;
import play.data.parsing.TempFilePlugin;
import play.data.validation.ValidationPlugin;
import play.db.DBPlugin;
import play.db.Evolutions;
import play.db.jpa.JPAPlugin;
import play.i18n.MessagesPlugin;
import play.jobs.JobsPlugin;
import play.libs.WS;

import static org.fest.assertions.Assertions.assertThat;

/**
 * Created by IntelliJ IDEA.
 * User: mortenkjetland
 * Date: 3/3/11
 * Time: 12:14 AM
 * To change this template use File | Settings | File Templates.
 */
public class PluginCollectionTest {

    @Test
    public void verifyLoading() {
        new PlayBuilder().build();
        PluginCollection pc = new PluginCollection();
        pc.loadPlugins();

        //the following plugin-list should match the list in the file 'play.plugins'
        assertThat(pc.getEnabledPlugins()).containsExactly(
                pc.getPluginInstance(CorePlugin.class),
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
        //verify that only application specific plugins gets reloaded
        new PlayBuilder().build();

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
        //verify that only application specific plugins gets reloaded
        new PlayBuilder().build();


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
        new PlayBuilder().build();

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
