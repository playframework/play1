package play.plugins;

import play.Logger;
import play.Play;
import play.PlayPlugin;
import play.classloading.ApplicationClasses;
import play.db.Model;
import play.exceptions.UnexpectedException;
import play.mvc.Http;
import play.mvc.Router;
import play.mvc.results.Result;
import play.templates.BaseTemplate;
import play.templates.Template;
import play.test.BaseTest;
import play.test.TestEngine;
import play.vfs.VirtualFile;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
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
                enablePlugin(plugin);
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
                    Collections.sort( enabledPlugins);
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

    public void invocationFinally(){
        for( PlayPlugin plugin : getEnabledPlugins() ){
            plugin.invocationFinally();
        }
    }

    public void beforeInvocation(){
        for( PlayPlugin plugin : getEnabledPlugins() ){
            plugin.beforeInvocation();
        }
    }

    public void afterInvocation(){
        for( PlayPlugin plugin : getEnabledPlugins() ){
            plugin.afterInvocation();
        }
    }

    public void onInvocationSuccess(){
        for( PlayPlugin plugin : getEnabledPlugins() ){
            plugin.onInvocationSuccess();
        }
    }

    public void onInvocationException(Throwable e) {
        for (PlayPlugin plugin : getEnabledPlugins()) {
            try {
                plugin.onInvocationException(e);
            } catch (Throwable ex) {
                //nop
            }
        }
    }

    public void beforeDetectingChanges(){
        for( PlayPlugin plugin : getEnabledPlugins() ){
            plugin.beforeDetectingChanges();
        }
    }

    public void detectChange(){
        for( PlayPlugin plugin : getEnabledPlugins() ){
            plugin.detectChange();
        }
    }

    public void onApplicationReady(){
        for( PlayPlugin plugin : getEnabledPlugins() ){
            plugin.onApplicationReady();
        }
    }

    public void onConfigurationRead(){
        for( PlayPlugin plugin : getEnabledPlugins() ){
            plugin.onConfigurationRead();
        }
    }

    public void onApplicationStart(){
        for (PlayPlugin plugin : getEnabledPlugins()) {
            plugin.onApplicationStart();
        }
    }

    public void afterApplicationStart(){
        for( PlayPlugin plugin : getEnabledPlugins() ){
            plugin.afterApplicationStart();
        }
    }

    public void onApplicationStop(){
        for( PlayPlugin plugin : getEnabledPlugins() ){
            plugin.onApplicationStop();
        }
    }

    public void onEvent(String message, Object context){
        for( PlayPlugin plugin : getEnabledPlugins() ){
            plugin.onEvent(message, context);
        }
    }

    public void enhance(ApplicationClasses.ApplicationClass applicationClass){
        for (PlayPlugin plugin : getEnabledPlugins()) {
            try {
                long start = System.currentTimeMillis();
                plugin.enhance(applicationClass);
                Logger.trace("%sms to apply %s to %s", System.currentTimeMillis() - start, plugin, applicationClass.name);
            } catch (Exception e) {
                throw new UnexpectedException("While applying " + plugin + " on " + applicationClass.name, e);
            }
        }
    }

    public List<ApplicationClasses.ApplicationClass> onClassesChange(List<ApplicationClasses.ApplicationClass> modified){
        List<ApplicationClasses.ApplicationClass> modifiedWithDependencies = new ArrayList<ApplicationClasses.ApplicationClass>();
        for( PlayPlugin plugin : getEnabledPlugins() ){
            modifiedWithDependencies.addAll( plugin.onClassesChange(modified) );
        }
        return modifiedWithDependencies;
    }


    public void compileAll(List<ApplicationClasses.ApplicationClass> classes){
        for( PlayPlugin plugin : getEnabledPlugins() ){
            plugin.compileAll(classes);
        }
    }

    public Object bind(String name, Object o, Map<String, String[]> params){
        for (PlayPlugin plugin : getEnabledPlugins()) {
            Object result = plugin.bind(name, o, params);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    public Object bind(String name, Class clazz, Type type, Annotation[] annotations, Map<String, String[]> params){
        for (PlayPlugin plugin : getEnabledPlugins()) {
            Object result = plugin.bind(name, clazz, type, annotations, params);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    public Map<String, Object> unBind(Object src, String name){
        for (PlayPlugin plugin : getEnabledPlugins()) {
            Map<String, Object> r = plugin.unBind(src, name);
            if (r != null) {
                return r;
            }
        }
        return null;
    }

    public Object willBeValidated(Object value){
        for (PlayPlugin plugin : getEnabledPlugins()) {
            Object newValue = plugin.willBeValidated(value);
            if (newValue != null) {
                return newValue;
            }
        }
        return value;
    }

    public Model.Factory modelFactory(Class<? extends Model> modelClass){
        for(PlayPlugin plugin : getEnabledPlugins()) {
            Model.Factory factory = plugin.modelFactory(modelClass);
            if(factory != null) {
                return factory;
            }
        }
        return null;
    }

    public String getMessage(String locale, Object key, Object... args){
        for (PlayPlugin plugin : getEnabledPlugins()) {
            String message = plugin.getMessage(locale, key, args);
            if(message != null) {
                return message;
            }
        }
        return null;
    }

    public void beforeActionInvocation(Method actionMethod){
        for (PlayPlugin plugin : getEnabledPlugins()) {
            plugin.beforeActionInvocation(actionMethod);
        }
    }

    public void onActionInvocationResult(Result result){
        for (PlayPlugin plugin : getEnabledPlugins()) {
            plugin.onActionInvocationResult(result);
        }
    }

    public void afterActionInvocation(){
        for (PlayPlugin plugin : getEnabledPlugins()) {
            plugin.afterActionInvocation();
        }
    }

    public void routeRequest(Http.Request request){
        for (PlayPlugin plugin : getEnabledPlugins()) {
            plugin.routeRequest(request);
        }
    }

    public void onRequestRouting(Router.Route route){
        for (PlayPlugin plugin : getEnabledPlugins()) {
            plugin.onRequestRouting(route);
        }
    }

    public void onRoutesLoaded(){
        for (PlayPlugin plugin : getEnabledPlugins()) {
            plugin.onRoutesLoaded();
        }
    }

    public boolean rawInvocation(Http.Request request, Http.Response response)throws Exception{
        for (PlayPlugin plugin : getEnabledPlugins()) {
            if (plugin.rawInvocation(request, response)) {
                //raw = true;
                return true;
            }
        }
        return false;
    }


    public boolean serveStatic(VirtualFile file, Http.Request request, Http.Response response){
        for (PlayPlugin plugin : getEnabledPlugins()) {
            if (plugin.serveStatic(file, request, response)) {
                //raw = true;
                return true;
            }
        }
        return false;
    }

    public List<String> addTemplateExtensions(){
        List<String> list = new ArrayList<String>();
        for (PlayPlugin plugin : getEnabledPlugins()) {
            list.addAll(plugin.addTemplateExtensions());
        }
        return list;
    }

    public String overrideTemplateSource(BaseTemplate template, String source){
        for(PlayPlugin plugin : getEnabledPlugins()) {
            String newSource = plugin.overrideTemplateSource(template, source);
            if(newSource != null) {
                source = newSource;
            }
        }
        return source;
    }

    public Template loadTemplate(VirtualFile file){
        for(PlayPlugin plugin : getEnabledPlugins() ) {
            Template pluginProvided = plugin.loadTemplate(file);
            if(pluginProvided != null) {
                return pluginProvided;
            }
        }
        return null;
    }

    public void afterFixtureLoad(){
        for (PlayPlugin plugin : getEnabledPlugins()) {
            plugin.afterFixtureLoad();
        }
    }

    public TestEngine.TestResults runTest(Class<BaseTest> clazz){
        for (PlayPlugin plugin : getEnabledPlugins()) {
            TestEngine.TestResults pluginTestResults = plugin.runTest(clazz);
            if (pluginTestResults != null) {
                return pluginTestResults;
            }
        }
        return null;
    }















}
