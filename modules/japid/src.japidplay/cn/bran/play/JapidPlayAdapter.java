package cn.bran.play;


import java.util.Collection;
import java.util.List;

import play.data.validation.Error;
import play.data.validation.Validation;
import play.i18n.Lang;
import play.i18n.Messages;
import play.mvc.Router.ActionDefinition;
import play.mvc.Scope.Flash;
import play.mvc.Scope.RenderArgs;
import play.mvc.Scope.Session;
import cn.bran.japid.util.FlashScope;

/**
 * offer wrappers to the Play! built-in varaibles avaible for templates
 * 
 * a bunch of wrappers are used for mainly read access to the Play!'s
 * components.
 * 
 * @author bran
 * 
 */
public class JapidPlayAdapter {
	public static FlashScope flash = new FlashWrapper();
//	public static ParamsWrapper params = new ParamsWrapper();
//	public static RenderArgsWrapper renderArgs = new RenderArgsWrapper();
	public static Messages messages = new Messages();
	public static Lang lang = new Lang();

	private static RouteAdapter urlMapper = new RouteAdapter();

	/**
	 * this one is more generic and connected to Play!
	 * 
	 * @param k
	 * @return
	 */
	public static Object renderArg(String k) {
//		return renderArgs.get(k);
		return RenderArgs.current().get(k);
	}

//	/**
//	 * get the last item from a list
//	 * 
//	 * @param list
//	 * @return
//	 */
//	public static <T> T lastOf(List<T> list) {
//		if (list.size() == 0)
//			return null;
//		return list.get(list.size() - 1);
//	}
//
//	public static Flash flash() {
//		return Flash.current();
//	}

	/**
	 * map an action to an absolute url
	 * 
	 * @param action
	 *            the controller.action part of the whole string
	 * @param args
	 *            : the argument list, each thereof is a result of an expression
	 *            evaluation meaning the name of the param is not kept
	 * 
	 * @param action
	 * @return
	 */
	public static String lookupAbs(String action, Object... args) {
		return urlMapper.lookupAbs(action, args);
	}

	public static String lookup(String action, Object... args) {
		return urlMapper.lookup(action, args);
	}

	/**
	 * lookup a static resource
	 * @param action
	 * @param args
	 * @return
	 */
	public static String lookupStaticAbs(String action) {
		return urlMapper.lookupStaticAbs(action);
	}
	
	public static String lookupStatic(String action) {
		return urlMapper.lookupStatic(action);
	}

	public static String getMessage(String msgName, Object... params) {
		return messages.get(msgName, params);
	}

	public static String i18n(String msgName, Object... params) {
		return messages.get(msgName, params);
	}
	
	public static Object flash(String key) {
		return flash.get(key);
	}

	/**
	 * generate an hidden field in the form with a security token to combat CSRF.
	 * @return
	 */
	public static String authenticityToken() {
		String t = "<input type=\"hidden\" name=\"authenticityToken\" value=\"%s\"/>" ;
		return String.format(t, Session.current().getAuthenticityToken());
	}
	
	public static String jsAction(String name, String... args) {
		String funcPattern = 
				"function(options) {\n" + 
				"	var pattern = '%s'; \n" + 
				"	for(key in options) { \n" + 
				"		pattern = pattern.replace(':'+key, options[key]); \n" + 
				"	} \n" + 
				"	return pattern; \n" + 
				"}";
		
		return String.format(funcPattern, lookup(name, (Object[])args).replace("&amp;", "&"));
	}
	
	/**
	 * create a js object that contains both a reverse lookup method and the action method of the current action
	 * @param name
	 * @param args
	 * @return
	 */
	public static String jsRoute(String name, String... args) {
        final ActionDefinition action = urlMapper.lookupActionDefinition(name, args);
        StringBuffer sb = new StringBuffer();
        sb.append("{");
        if (action.args.isEmpty()) {
        	sb.append("url: function() { return '" + action.url.replace("&amp;", "&") + "'; },");
        } else {
        	sb.append("url: function(args) { var pattern = '" + action.url.replace("&amp;", "&") + "'; for (var key in args) { pattern = pattern.replace(':'+key, args[key]); } return pattern; },");
        }
        sb.append("method: '" + action.method + "'");
        sb.append("}");
        return sb.toString();
    }
	
	public static String or(Object o , String substitude) {
		try {
			String string = o.toString();
			if (string == null || string.length() == 0)
				return substitude;
			else
				return string;
		}
		catch (NullPointerException e) {
			return substitude;
		}
	}
	
	/**
	 * determine if the current user
	 * @param roles
	 * @return
	 */
	public static boolean inRole(String... roles) {
		// TODO implement this
		return false;
	}
}
