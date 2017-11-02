package play.plugins;

import org.junit.Before;
import org.junit.Test;
import play.Play;
import play.PlayBuilder;
import play.PlayPlugin;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import static org.fest.assertions.Assertions.assertThat;

/**
 * Created by IntelliJ IDEA.
 * User: mortenkjetland
 * Date: 3/2/11
 * Time: 9:02 PM
 * To change this template use File | Settings | File Templates.
 */
public class ConfigurablePluginDisablingPluginTest {

    @Before
    public void before(){
        //each test must begin with empty memory..
        ConfigurablePluginDisablingPlugin.previousDisabledPlugins.clear();
    }


    @Test
    public void verify_without_config() throws Exception {

        Properties config = new Properties();
        config.put("some.setting", "some value");

        PluginCollection pc = new PluginCollection();
        TestPlugin p = new TestPlugin();
        pc.addPlugin( p );

        internalTest(config, pc, Arrays.asList(p));
        
    }

    private void internalTest(Properties config, PluginCollection pc, List<? extends PlayPlugin> correctPluginListAfter) {
        Play.configuration = config;
        Play.pluginCollection = pc;
        ConfigurablePluginDisablingPlugin plugin = new ConfigurablePluginDisablingPlugin();
        plugin.onConfigurationRead();

        assertThat(pc.getEnabledPlugins()).containsOnly(correctPluginListAfter.toArray());
    }

    @Test
    public void verify_disableing_plugins() throws Exception {


        PluginCollection pc = new PluginCollection();
        TestPlugin p = new TestPlugin();
        pc.addPlugin( p );
        TestPlugin2 p2 = new TestPlugin2();
        pc.addPlugin( p2 );


        Properties config = new Properties();
        config.put("some.setting", "some value");
        config.put("plugins.disable", "play.plugins.TestPlugin");

        internalTest(config, pc, Arrays.asList(p2));

    }

    @Test
    public void verify_disableing_many_plugins() throws Exception {


        PluginCollection pc = new PluginCollection();
        TestPlugin p = new TestPlugin();
        pc.addPlugin( p );
        TestPlugin2 p2 = new TestPlugin2();
        pc.addPlugin( p2 );


        Properties config = new Properties();
        config.put("some.setting", "some value");
        config.put("plugins.disable", "play.plugins.TestPlugin");
        config.put("plugins.disable.2", "play.plugins.TestPlugin2");

        internalTest(config, pc, new ArrayList<PlayPlugin>());

    }


    @Test
    public void verify_disableing_missing_plugins() throws Exception {

        PluginCollection pc = new PluginCollection();
        TestPlugin p = new TestPlugin();
        pc.addPlugin( p );
        TestPlugin2 p2 = new TestPlugin2();
        pc.addPlugin( p2 );


        Properties config = new Properties();
        config.put("some.setting", "some value");
        config.put("plugins.disable", "play.plugins.TestPlugin_XX");

        internalTest(config, pc, Arrays.asList(p,p2));

    }

    @Test
    public void verify_reenabling_disabled_plugins() throws Exception {


        PluginCollection pc = new PluginCollection();
        TestPlugin p = new TestPlugin();
        pc.addPlugin( p );
        TestPlugin2 p2 = new TestPlugin2();
        pc.addPlugin( p2 );


        Properties config = new Properties();
        config.put("some.setting", "some value");
        config.put("plugins.disable", "play.plugins.TestPlugin");

        internalTest(config, pc, Arrays.asList(p2));

        //remove the disabling from config
        config = new Properties();
        config.put("some.setting", "some value");

        internalTest(config, pc, Arrays.asList(p,p2));

    }

    @Test
    public void verify_that_the_plugin_gets_loaded(){
        PluginCollection pc = new PluginCollection();

        new PlayBuilder().build();
        pc.loadPlugins();
        ConfigurablePluginDisablingPlugin pi = pc.getPluginInstance(ConfigurablePluginDisablingPlugin.class);
        assertThat(pc.getEnabledPlugins()).contains( pi );
    }


}

class TestPlugin extends PlayPlugin {

    //missing constructor on purpose

}

class TestPlugin2 extends PlayPlugin {

    //included constructor on purpose
    public TestPlugin2() {
    }
}