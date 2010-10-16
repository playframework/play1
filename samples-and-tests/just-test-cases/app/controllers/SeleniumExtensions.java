package controllers;

import java.util.ArrayList;

import play.cache.Cache;
import play.mvc.Controller;

public class SeleniumExtensions extends Controller {

	/**
	 * Check we can get strings out of the cache with Selenium (for example to
	 * help Captcha testing)
	 */
	public static void cacheAccess() {
		String key = "magicKey";
		Cache.set(key,"magicValue","1mn");
		
		ArrayList<Boolean> complex = new ArrayList<Boolean>();
		complex.add(true);
		complex.add(false);
		Cache.set("complex", complex);
		
		renderText("OK");
	}
	
	/**
	 * Check we can get the last email sent to an address with Selenium (for
	 * example to help user registration test)
	 */
	public static void emailAccess() {
        notifiers.Welcome.seleniumTest();
        renderText("OK");
	}

}
