package play.plugins;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.stream.Stream;

import play.Logger;
import play.Play;
import play.PlayPlugin;
import play.classloading.ApplicationClasses;
import play.classloading.ApplicationClassloader;
import play.data.binding.RootParamNode;
import play.db.Model;
import play.exceptions.UnexpectedException;
import play.inject.Injector;
import play.libs.F;
import play.mvc.Http;
import play.mvc.Router;
import play.mvc.results.Result;
import play.templates.BaseTemplate;
import play.templates.Template;
import play.test.BaseTest;
import play.test.TestEngine;
import play.vfs.VirtualFile;

import static java.util.Collections.emptyList;
import static java.util.Objects.hash;
import static java.util.stream.Collectors.toList;

/**
 * Class handling all plugins used by Play.
 *
 * Loading/reloading/enabling/disabling is handled here.
 *
 * This class also exposes many PlayPlugin-methods which when called, the method is executed on all enabled plugins.
 *
 * Since all the enabled-plugins-iteration is done here, the code elsewhere is cleaner.
 */
public class PluginCollection {

    /**
     * Property holding the name of the play.plugins-resource-name. Can be modified in unittest to supply modifies
     * plugin-list
     */
    protected String play_plugins_resourceName = "play.plugins";

    /**
     * List that holds all loaded plugins, enabled or disabled
     */
    protected List<PlayPlugin> allPlugins = new ArrayList<>();

    /**
     * Readonly copy of allPlugins - updated each time allPlugins is updated. Using this cached copy so we don't have to
     * create it all the time..
     */
    protected List<PlayPlugin> allPlugins_readOnlyCopy = createReadonlyCopy(allPlugins);

    /**
     * List of all enabled plugins
     */
    protected List<PlayPlugin> enabledPlugins = new ArrayList<>();

    /**
     * Readonly copy of enabledPlugins - updated each time enabledPlugins is updated. Using this cached copy so we don't
     * have to create it all the time
     */
    protected List<PlayPlugin> enabledPlugins_readOnlyCopy = createReadonlyCopy(enabledPlugins);

    /**
     * List of all enabled plugins with filters
     */
    protected List<PlayPlugin> enabledPluginsWithFilters = new ArrayList<>();

    /**
     * Readonly copy of enabledPluginsWithFilters - updated each time enabledPluginsWithFilters is updated. Using this
     * cached copy so we don't have to create it all the time
     */
    protected List<PlayPlugin> enabledPluginsWithFilters_readOnlyCopy = createReadonlyCopy(enabledPluginsWithFilters);

    /**
     * Using readonly list to crash if someone tries to modify the copy.
     * 
     * @param list
     *            The list of plugins
     * @return Read only list of plugins
     */
    protected List<PlayPlugin> createReadonlyCopy(List<PlayPlugin> list) {
        return Collections.unmodifiableList(new ArrayList<>(list));
    }

    private static class LoadingPluginInfo implements Comparable<LoadingPluginInfo> {
        public final String name;
        public final int index;
        public final URL url;

        private LoadingPluginInfo(String name, int index, URL url) {
            this.name = name;
            this.index = index;
            this.url = url;
        }

        @Override
        public String toString() {
            return String.format("LoadingPluginInfo{name='%s', index=%s, url=%s}", name, index, url);
        }

        @Override
        public int compareTo(LoadingPluginInfo o) {
            int res = index < o.index ? -1 : (index == o.index ? 0 : 1);
            if (res != 0) {
                return res;
            }

            // Index is equal in both plugins.
            // sort on name to get consistent order
            return name.compareTo(o.name);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;

            LoadingPluginInfo that = (LoadingPluginInfo) o;
            return Objects.equals(index, that.index) && Objects.equals(name, that.name);
        }

        @Override
        public int hashCode() {
            return hash(name, index);
        }
    }

    public void loadPlugins() {
        Logger.trace("Loading plugins");
        List<URL> urls = loadPlayPluginDescriptors();

        // First we build one big SortedSet of all plugins to load (sorted based on index)
        // This must be done to make sure the enhancing is happening
        // when loading plugins using other classes that must be enhanced.
        SortedSet<LoadingPluginInfo> pluginsToLoad = new TreeSet<>();
        for (URL url : urls) {
            Logger.trace("Found one plugins descriptor, %s", url);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream(), "utf-8"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.trim().length() == 0) {
                        continue;
                    }
                    String[] lineParts = line.split(":");
                    LoadingPluginInfo info = new LoadingPluginInfo(lineParts[1].trim(), Integer.parseInt(lineParts[0]), url);
                    pluginsToLoad.add(info);
                }
            } catch (Exception e) {
                Logger.error(e, "Error interpreting %s", url);
            }
        }

        for (LoadingPluginInfo info : pluginsToLoad) {
            Logger.trace("Loading plugin %s", info.name);
            try {
                PlayPlugin plugin = (PlayPlugin) Injector.getBeanOfType(Play.classloader.loadClass(info.name));
                plugin.index = info.index;
                if (addPlugin(plugin)) {
                    Logger.trace("Plugin %s loaded", plugin);
                } else {
                    Logger.warn("Did not load plugin %s. Already loaded", plugin);
                }
            } catch (Exception ex) {
                Logger.error(ex, "Error loading plugin %s", info.toString());
            }
        }
        // Now we must call onLoad for all plugins - and we must detect if a
        // plugin
        // disables another plugin the old way, by removing it from
        // Play.plugins.
        for (PlayPlugin plugin : getEnabledPlugins()) {

            // is this plugin still enabled?
            if (isEnabled(plugin)) {
                initializePlugin(plugin);
            }
        }

        // Must update Play.plugins-list one last time
        updatePlayPluginsList();
    }

    List<URL> loadPlayPluginDescriptors() {
        try {
            String playPluginsDescriptors = Play.configuration.getProperty("play.plugins.descriptor");
            if (playPluginsDescriptors != null) {
                return Stream.of(playPluginsDescriptors.split(","))
                    .map(playPluginsDescriptor -> fileToUrl(playPluginsDescriptor))
                    .collect(toList());
            }
            return Collections.list(Play.classloader.getResources(play_plugins_resourceName));
        }
        catch (IOException e) {
            Logger.error(e, "Error loading play.plugins");
            return emptyList();
        }
    }

    private URL fileToUrl(String fileName) {
        try {
            return new File(Play.applicationPath, fileName).toURI().toURL();
        }
        catch (MalformedURLException e) {
            throw new IllegalArgumentException("Error loading file " + fileName, e);
        }
    }

    /**
     * Reloads all loaded plugins that is application-supplied.
     * 
     * @throws Exception
     *             If problem occurred during reload
     */
    public void reloadApplicationPlugins() throws Exception {

        Set<PlayPlugin> reloadedPlugins = new HashSet<>();
        for (PlayPlugin plugin : getAllPlugins()) {

            // Is this plugin an application-supplied-plugin?
            if (isLoadedByApplicationClassloader(plugin)) {
                // This plugin is application-supplied - Must reload it
                Class pluginClazz = Play.classloader.loadClass(plugin.getClass().getName());
                PlayPlugin newPlugin = (PlayPlugin) Injector.getBeanOfType(pluginClazz);
                newPlugin.index = plugin.index;
                // Replace this plugin
                replacePlugin(plugin, newPlugin);
                reloadedPlugins.add(newPlugin);
            }
        }

        // Now we must call onLoad for all reloaded plugins
        for (PlayPlugin plugin : reloadedPlugins) {
            initializePlugin(plugin);
        }

        updatePlayPluginsList();

    }

    protected boolean isLoadedByApplicationClassloader(PlayPlugin plugin) {
        return plugin.getClass().getClassLoader().getClass().equals(ApplicationClassloader.class);
    }

    /**
     * Calls plugin.onLoad but detects if plugin removes other plugins from Play.plugins-list to detect if plugins
     * disables a plugin the old hacked way..
     * 
     * @param plugin
     *            The given plugin
     */
    @SuppressWarnings({ "deprecation" })
    protected void initializePlugin(PlayPlugin plugin) {
        Logger.trace("Initializing plugin " + plugin);
        // We're ready to call onLoad for this plugin.
        // must create a unique Play.plugins-list for this onLoad-method-call so
        // we can detect if some plugins are removed/disabled
        Play.plugins = new ArrayList<>(getEnabledPlugins());
        plugin.onLoad();
        // Check for missing/removed plugins
        for (PlayPlugin enabledPlugin : getEnabledPlugins()) {
            if (!Play.plugins.contains(enabledPlugin)) {
                Logger.info("Detected that plugin '" + plugin + "' disabled the plugin '" + enabledPlugin
                        + "' the old way - should use Play.disablePlugin()");
                // This enabled plugin was disabled.
                // must disable it in pluginCollection
                disablePlugin(enabledPlugin);
            }
        }
    }

    /**
     * Adds one plugin and enables it
     * 
     * @param plugin
     *            The given plugin
     * @return true if plugin was new and was added
     */
    protected synchronized boolean addPlugin(PlayPlugin plugin) {
        if (!allPlugins.contains(plugin)) {
            allPlugins.add(plugin);
            Collections.sort(allPlugins);
            allPlugins_readOnlyCopy = createReadonlyCopy(allPlugins);
            enablePlugin(plugin);
            return true;
        }
        return false;
    }

    protected synchronized void replacePlugin(PlayPlugin oldPlugin, PlayPlugin newPlugin) {
        if (allPlugins.remove(oldPlugin)) {
            allPlugins.add(newPlugin);
            Collections.sort(allPlugins);
            allPlugins_readOnlyCopy = createReadonlyCopy(allPlugins);
        }

        if (enabledPlugins.remove(oldPlugin)) {
            enabledPlugins.add(newPlugin);
            Collections.sort(enabledPlugins);
            enabledPlugins_readOnlyCopy = createReadonlyCopy(enabledPlugins);

            if (enabledPluginsWithFilters.remove(oldPlugin) && newPlugin.hasFilter()) {
                enabledPluginsWithFilters.add(newPlugin);
                Collections.sort(enabledPluginsWithFilters);
                enabledPluginsWithFilters_readOnlyCopy = createReadonlyCopy(enabledPluginsWithFilters);
            }
        }

    }

    /**
     * Enable plugin.
     *
     * @param plugin
     *            The given plugin
     * @return true if plugin exists and was enabled now
     */
    public synchronized boolean enablePlugin(PlayPlugin plugin) {
        if (allPlugins.contains(plugin)) {
            // the plugin exists
            if (!enabledPlugins.contains(plugin)) {
                // plugin not currently enabled
                enabledPlugins.add(plugin);
                Collections.sort(enabledPlugins);
                enabledPlugins_readOnlyCopy = createReadonlyCopy(enabledPlugins);

                if (plugin.hasFilter()) {
                    enabledPluginsWithFilters.add(plugin);
                    Collections.sort(enabledPluginsWithFilters);
                    enabledPluginsWithFilters_readOnlyCopy = createReadonlyCopy(enabledPluginsWithFilters);
                }

                updatePlayPluginsList();
                Logger.trace("Plugin " + plugin + " enabled");
                return true;
            }
        }

        return false;
    }

    /**
     * enable plugin of specified type
     * 
     * @param pluginClazz
     *            The plugin class
     * 
     * @return true if plugin was enabled
     */
    public boolean enablePlugin(Class<? extends PlayPlugin> pluginClazz) {
        return enablePlugin(getPluginInstance(pluginClazz));
    }

    /**
     * Returns the first instance of a loaded plugin of specified type
     * 
     * @param pluginClazz
     *            The plugin class
     * @return PlayPlugin
     */
    public synchronized <T extends PlayPlugin> T getPluginInstance(Class<T> pluginClazz) {
        for (PlayPlugin p : getAllPlugins()) {
            if (pluginClazz.isInstance(p)) {
                return (T) p;
            }
        }
        return null;
    }

    /**
     * disable plugin
     * 
     * @param plugin
     *            The given plugin
     * @return true if plugin was enabled and now is disabled
     */
    public synchronized boolean disablePlugin(PlayPlugin plugin) {
        // try to disable it?
        if (enabledPlugins.remove(plugin)) {
            // plugin was removed
            enabledPlugins_readOnlyCopy = createReadonlyCopy(enabledPlugins);

            if (enabledPluginsWithFilters.remove(plugin)) {
                enabledPluginsWithFilters_readOnlyCopy = createReadonlyCopy(enabledPluginsWithFilters);
            }

            updatePlayPluginsList();
            Logger.trace("Plugin " + plugin + " disabled");
            return true;
        }
        return false;
    }

    /**
     * Disable plugin of specified type
     * 
     * @param pluginClazz
     *            The plugin class
     * 
     * @return true if plugin was enabled and now is disabled
     */
    public boolean disablePlugin(Class<? extends PlayPlugin> pluginClazz) {
        return disablePlugin(getPluginInstance(pluginClazz));
    }

    /**
     * Must update Play.plugins-list to be backward compatible
     */
    @SuppressWarnings({ "deprecation" })
    public void updatePlayPluginsList() {
        Play.plugins = Collections.unmodifiableList(getEnabledPlugins());
    }

    /**
     * Returns new readonly list of all enabled plugins
     * 
     * @return List of plugins
     */
    public List<PlayPlugin> getEnabledPlugins() {
        return enabledPlugins_readOnlyCopy;
    }

    /**
     * Returns new readonly list of all enabled plugins that define filters.
     * 
     * @return List of plugins
     */
    public List<PlayPlugin> getEnabledPluginsWithFilters() {
        return enabledPluginsWithFilters_readOnlyCopy;
    }

    @SuppressWarnings("unchecked")
    public <T> F.Option<PlayPlugin.Filter<T>> composeFilters() {
        // Copy list of plugins here in case the list changes in the midst of
        // doing composition...
        // (Is it really necessary to do this?)
        List<PlayPlugin> pluginsWithFilters = new ArrayList<>(this.getEnabledPluginsWithFilters());

        if (pluginsWithFilters.isEmpty()) {
            return F.Option.None();
        } else {
            Iterator<PlayPlugin> itr = pluginsWithFilters.iterator();
            PlayPlugin.Filter<T> ret = itr.next().getFilter();
            while (itr.hasNext()) {
                ret = ret.<T> decorate(itr.next().getFilter());
            }
            return F.Option.Some(ret);
        }
    }

    /**
     * Returns readonly view of all enabled plugins in reversed order
     * 
     * @return Collection of plugins
     */
    public Collection<PlayPlugin> getReversedEnabledPlugins() {
        return new AbstractCollection<PlayPlugin>() {

            @Override
            public Iterator<PlayPlugin> iterator() {
                final ListIterator<PlayPlugin> enabledPluginsListIt = enabledPlugins.listIterator(size() - 1);
                return new Iterator<PlayPlugin>() {

                    @Override
                    public boolean hasNext() {
                        return enabledPluginsListIt.hasPrevious();
                    }

                    @Override
                    public PlayPlugin next() {
                        return enabledPluginsListIt.previous();
                    }

                    @Override
                    public void remove() {
                        enabledPluginsListIt.remove();
                    }
                };
            }

            @Override
            public int size() {
                return enabledPlugins.size();
            }

        };
    }

    /**
     * Returns new readonly list of all plugins
     * 
     * @return List of plugins
     */
    public List<PlayPlugin> getAllPlugins() {
        return allPlugins_readOnlyCopy;
    }

    /**
     * Indicate if a plugin is enabled
     * 
     * @param plugin
     *            The given plugin
     * @return true if plugin is enabled
     */
    public boolean isEnabled(PlayPlugin plugin) {
        return getEnabledPlugins().contains(plugin);
    }

    public boolean compileSources() {
        for (PlayPlugin plugin : getEnabledPlugins()) {
            if (plugin.compileSources()) {
                return true;
            }
        }
        return false;
    }

    public boolean detectClassesChange() {
        for (PlayPlugin plugin : getEnabledPlugins()) {
            if (plugin.detectClassesChange()) {
                return true;
            }
        }
        return false;
    }

    public void invocationFinally() {
        for (PlayPlugin plugin : getEnabledPlugins()) {
            plugin.invocationFinally();
        }
    }

    public void beforeInvocation() {
        for (PlayPlugin plugin : getEnabledPlugins()) {
            plugin.beforeInvocation();
        }
    }

    public void afterInvocation() {
        for (PlayPlugin plugin : getEnabledPlugins()) {
            plugin.afterInvocation();
        }
    }

    public void onInvocationSuccess() {
        for (PlayPlugin plugin : getEnabledPlugins()) {
            plugin.onInvocationSuccess();
        }
    }

    public void onInvocationException(Throwable e) {
        for (PlayPlugin plugin : getEnabledPlugins()) {
            try {
                plugin.onInvocationException(e);
            } catch (Throwable ex) {
                Logger.error(ex, "Failed to handle invocation exception by plugin %s", plugin.getClass().getName());
            }
        }
    }

    public void beforeDetectingChanges() {
        for (PlayPlugin plugin : getEnabledPlugins()) {
            plugin.beforeDetectingChanges();
        }
    }

    public void detectChange() {
        for (PlayPlugin plugin : getEnabledPlugins()) {
            plugin.detectChange();
        }
    }

    public void onApplicationReady() {
        for (PlayPlugin plugin : getEnabledPlugins()) {
            plugin.onApplicationReady();
        }
    }

    public void onConfigurationRead() {
        for (PlayPlugin plugin : getEnabledPlugins()) {
            plugin.onConfigurationRead();
        }
    }

    public void onApplicationStart() {
        for (PlayPlugin plugin : getEnabledPlugins()) {
            plugin.onApplicationStart();
        }
    }

    public void afterApplicationStart() {
        for (PlayPlugin plugin : getEnabledPlugins()) {
            plugin.afterApplicationStart();
        }
    }

    public void onApplicationStop() {
        for (PlayPlugin plugin : getReversedEnabledPlugins()) {
            try {
                plugin.onApplicationStop();
            } catch (Throwable t) {
                if (t.getMessage() == null)
                    Logger.error(t, "Error while stopping %s", plugin);
                else if (Logger.isDebugEnabled())
                    Logger.debug(t, "Error while stopping %s", plugin);
                else
                    Logger.info("Error while stopping %s: %s", plugin, t.toString());
            }
        }
    }

    public void onEvent(String message, Object context) {
        for (PlayPlugin plugin : getEnabledPlugins()) {
            plugin.onEvent(message, context);
        }
    }

    public void enhance(ApplicationClasses.ApplicationClass applicationClass) {
        for (PlayPlugin plugin : getEnabledPlugins()) {
            try {
                long start = System.currentTimeMillis();
                plugin.enhance(applicationClass);
                if (Logger.isTraceEnabled()) {
                    Logger.trace("%sms to apply %s to %s", System.currentTimeMillis() - start, plugin, applicationClass.name);
                }
            } catch (Exception e) {
                throw new UnexpectedException("While applying " + plugin + " on " + applicationClass.name, e);
            }
        }
    }

    @Deprecated
    public List<ApplicationClasses.ApplicationClass> onClassesChange(List<ApplicationClasses.ApplicationClass> modified) {
        List<ApplicationClasses.ApplicationClass> modifiedWithDependencies = new ArrayList<>();
        for (PlayPlugin plugin : getEnabledPlugins()) {
            modifiedWithDependencies.addAll(plugin.onClassesChange(modified));
        }
        return modifiedWithDependencies;
    }

    @Deprecated
    public void compileAll(List<ApplicationClasses.ApplicationClass> classes) {
        for (PlayPlugin plugin : getEnabledPlugins()) {
            plugin.compileAll(classes);
        }
    }

    public Object bind(RootParamNode rootParamNode, String name, Class<?> clazz, Type type, Annotation[] annotations) {
        for (PlayPlugin plugin : getEnabledPlugins()) {
            Object result = plugin.bind(rootParamNode, name, clazz, type, annotations);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    public Object bindBean(RootParamNode rootParamNode, String name, Object bean) {
        for (PlayPlugin plugin : getEnabledPlugins()) {
            Object result = plugin.bindBean(rootParamNode, name, bean);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    public Map<String, Object> unBind(Object src, String name) {
        for (PlayPlugin plugin : getEnabledPlugins()) {
            Map<String, Object> r = plugin.unBind(src, name);
            if (r != null) {
                return r;
            }
        }
        return null;
    }

    public Object willBeValidated(Object value) {
        for (PlayPlugin plugin : getEnabledPlugins()) {
            Object newValue = plugin.willBeValidated(value);
            if (newValue != null) {
                return newValue;
            }
        }
        return value;
    }

    public Model.Factory modelFactory(Class<? extends Model> modelClass) {
        for (PlayPlugin plugin : getEnabledPlugins()) {
            Model.Factory factory = plugin.modelFactory(modelClass);
            if (factory != null) {
                return factory;
            }
        }
        return null;
    }

    public String getMessage(String locale, Object key, Object... args) {
        for (PlayPlugin plugin : getEnabledPlugins()) {
            String message = plugin.getMessage(locale, key, args);
            if (message != null) {
                return message;
            }
        }
        return null;
    }

    public void beforeActionInvocation(Method actionMethod) {
        for (PlayPlugin plugin : getEnabledPlugins()) {
            plugin.beforeActionInvocation(actionMethod);
        }
    }

    public void onActionInvocationResult(Result result) {
        for (PlayPlugin plugin : getEnabledPlugins()) {
            plugin.onActionInvocationResult(result);
        }
    }

    public void afterActionInvocation() {
        for (PlayPlugin plugin : getEnabledPlugins()) {
            plugin.afterActionInvocation();
        }
    }

    public void onActionInvocationFinally() {
        for (PlayPlugin plugin : getEnabledPlugins()) {
            plugin.onActionInvocationFinally();
        }
    }

    public void routeRequest(Http.Request request) {
        for (PlayPlugin plugin : getEnabledPlugins()) {
            plugin.routeRequest(request);
        }
    }

    public void onRequestRouting(Router.Route route) {
        for (PlayPlugin plugin : getEnabledPlugins()) {
            plugin.onRequestRouting(route);
        }
    }

    public void onRoutesLoaded() {
        for (PlayPlugin plugin : getEnabledPlugins()) {
            plugin.onRoutesLoaded();
        }
    }

    public boolean rawInvocation(Http.Request request, Http.Response response) throws Exception {
        for (PlayPlugin plugin : getEnabledPlugins()) {
            if (plugin.rawInvocation(request, response)) {
                return true;
            }
        }
        return false;
    }

    public boolean serveStatic(VirtualFile file, Http.Request request, Http.Response response) {
        for (PlayPlugin plugin : getEnabledPlugins()) {
            if (plugin.serveStatic(file, request, response)) {
                return true;
            }
        }
        return false;
    }

    public List<String> addTemplateExtensions() {
        List<String> list = new ArrayList<>();
        for (PlayPlugin plugin : getEnabledPlugins()) {
            list.addAll(plugin.addTemplateExtensions());
        }
        return list;
    }

    public String overrideTemplateSource(BaseTemplate template, String source) {
        for (PlayPlugin plugin : getEnabledPlugins()) {
            String newSource = plugin.overrideTemplateSource(template, source);
            if (newSource != null) {
                source = newSource;
            }
        }
        return source;
    }

    public Template loadTemplate(VirtualFile file) {
        for (PlayPlugin plugin : getEnabledPlugins()) {
            Template pluginProvided = plugin.loadTemplate(file);
            if (pluginProvided != null) {
                return pluginProvided;
            }
        }
        return null;
    }

    public void afterFixtureLoad() {
        for (PlayPlugin plugin : getEnabledPlugins()) {
            plugin.afterFixtureLoad();
        }
    }

    public TestEngine.TestResults runTest(Class<BaseTest> clazz) {
        for (PlayPlugin plugin : getEnabledPlugins()) {
            TestEngine.TestResults pluginTestResults = plugin.runTest(clazz);
            if (pluginTestResults != null) {
                return pluginTestResults;
            }
        }
        return null;
    }

    public Collection<Class> getUnitTests() {
        Set<Class> allPluginTests = new HashSet<>();
        for (PlayPlugin plugin : getEnabledPlugins()) {
            Collection<Class> unitTests = plugin.getUnitTests();
            if (unitTests != null) {
                allPluginTests.addAll(unitTests);
            }
        }

        return allPluginTests;
    }

    public Collection<Class> getFunctionalTests() {
        Set<Class> allPluginTests = new HashSet<>();
        for (PlayPlugin plugin : getEnabledPlugins()) {
            Collection<Class> funcTests = plugin.getFunctionalTests();
            if (funcTests != null) {
                allPluginTests.addAll(funcTests);
            }
        }

        return allPluginTests;
    }
}
