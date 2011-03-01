package play.plugins;

import play.Logger;
import play.Play;
import play.PlayPlugin;

import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: mortenkjetland
 * Date: 2/28/11
 * Time: 11:29 PM
 * To change this template use File | Settings | File Templates.
 */
public class PluginCollection {

    protected Object lock = new Object();
    protected List<PlayPlugin> allPlugins = new ArrayList<PlayPlugin>();
    protected List<PlayPlugin> enabledPlugins = new ArrayList<PlayPlugin>();


    /**
     * Adds one plugin and enables it
     * @param plugin
     * @return true if plugin was new and was added
     */
    public boolean addPlugin( PlayPlugin plugin ){
        synchronized( lock ){
            if( !allPlugins.contains(plugin) ){
                allPlugins.add( plugin );
                enabledPlugins.add( plugin );
                return true;
            }
        }
        return false;
    }

    /**
     * Enable plugin.
     *
     * @param plugin
     * @return true if plugin exists and was enabled now
     */
    public boolean enablePlugin( PlayPlugin plugin ){
        synchronized( lock ){
            if( allPlugins.contains( plugin )){
                //the plugin exists
                if( !enabledPlugins.contains( plugin )){
                    //plugin not currently enabled
                    enabledPlugins.add( plugin );
                    updayePlayPluginsList();
                    Logger.trace("Plugin " + plugin + " enabled");
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * enable plugin of specified type
     * @return true if plugin was enabled
     */
    public boolean enablePlugin( Class<? extends PlayPlugin> pluginClazz ){
        return enablePlugin(getPluginInstance(pluginClazz));
    }


    /**
     * Returns the first instance of a loaded plugin of specified type
     * @param pluginClazz
     * @return
     */
    public PlayPlugin getPluginInstance( Class<? extends PlayPlugin> pluginClazz){
        synchronized( lock ){
            for( PlayPlugin p : allPlugins){
                if (pluginClazz.isInstance(p)) {
                    return p;    
                }
            }
        }
        return null;
    }


    /**
     * disable plugin
     * @param plugin
     * @return true if plugin was enabled and now is disabled
     */
    public boolean disablePlugin( PlayPlugin plugin ){
        synchronized( lock ){
            //try to disable it?
            if( enabledPlugins.remove( plugin ) ){
                //plugin was removed
                updayePlayPluginsList();
                Logger.trace("Plugin " + plugin + " disabled");
                return true;
            }
        }
        return false;
    }


    /**
     * Must update Play.plugins-list everything relies on this list..
     */
    public void updayePlayPluginsList(){
        Play.plugins = Collections.unmodifiableList( getEnabledPlugins() );
    }

    /**
     * disable plugin of specified type
     * @return true if plugin was enabled and now is disabled
     */
    public boolean disablePlugin( Class<? extends PlayPlugin> pluginClazz ){
        return disablePlugin( getPluginInstance( pluginClazz));
    }

    /**
     * Returns new readonly list of all enabled plugins
     * @return
     */
    public List<PlayPlugin> getEnabledPlugins(){
        synchronized( lock ){
            Collections.sort( enabledPlugins);
            return Collections.unmodifiableList(enabledPlugins);
        }
    }

    /**
     * Returns new readonly list of all plugins
     * @return
     */
    public List<PlayPlugin> getAllPlugins(){
        synchronized( lock ){
            Collections.sort( allPlugins);
            return Collections.unmodifiableList(allPlugins);
        }
    }


    /**
     *
     * @param plugin
     * @return true if plugin is enabled
     */
    public boolean isEnabled( PlayPlugin plugin){
        synchronized( lock ){
            return enabledPlugins.contains( plugin );
        }
    }



}
