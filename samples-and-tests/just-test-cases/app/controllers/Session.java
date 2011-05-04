package controllers;

import java.lang.reflect.*;
import java.util.*;

import play.*;
import play.mvc.*;

import play.cache.Cache;

public class Session extends Controller {

	public static void setSendOnlyIfChanged(boolean value) throws Exception {
    	/*
    	 * Set the final static value Scope.SESSION_SEND_ONLY_IF_CHANGED using reflection.
    	 */
        Field field = Scope.class.getField("SESSION_SEND_ONLY_IF_CHANGED");
        field.setAccessible(true);
        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);

        // Set the new value
        field.setBoolean(null, value);

        index();
    }

	public static void restoreConfiguration() throws Exception {
    	boolean config = Play.configuration.getProperty("application.session.sendOnlyIfChanged", "false").toLowerCase().equals("true"); 
    	setSendOnlyIfChanged(config);
        index();
    }

	public static final String SUSPEND_PREFIX = "suspend-";

	public static void index() {
        renderArgs.put("SESSION_SEND_ONLY_IF_CHANGED", Scope.SESSION_SEND_ONLY_IF_CHANGED);
        render();
    }

	public static void submit() {
        render();
    }

	public static void put(String key, String value) {
        session.put(key, value);
        index();
    }

	public static void remove(String key) {
        session.remove(key);
        index();
    }

	public static void removeKeys(String[] keys) {
    	session.remove(keys);
        index();
    }

	public static void clear() {
    	session.clear();
    	index();
    }

	public static void suspendedRequest(String suspendKey, String callback) {
        Cache.add(SUSPEND_PREFIX + suspendKey, "wait");

        await(500);
        while("wait".equals(Cache.get(SUSPEND_PREFIX + suspendKey))) {
            await(500);
        }
        
        renderJSON(callback + "({'suspendKey':'" + suspendKey + "'})");
    }

	public static void stopSuspendedRequest(String suspendKey) {
        Cache.delete(SUSPEND_PREFIX + suspendKey);
    }
}
