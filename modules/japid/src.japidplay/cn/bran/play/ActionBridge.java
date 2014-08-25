package cn.bran.play;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import cn.bran.japid.exceptions.JapidRuntimeException;
import cn.bran.play.exceptions.ReverseRouteException;

import play.data.binding.Unbinder;
import play.exceptions.ActionNotFoundException;
import play.exceptions.NoRouteFoundException;
import play.exceptions.PlayException;
import play.exceptions.UnexpectedException;
import play.mvc.ActionInvoker;
import play.mvc.Http.Request;
import play.mvc.Router;
import play.mvc.Router.ActionDefinition;
import play.utils.Java;

/**
 * this file is copied from 
 * play.templates
 * .GroovyTemplate.ExecutableTemplate.ActionBridge.invokeMethod(...); version 1.2 trunk. 
 * 
 * since the class is not public.
 * 
 * Please check this with the original source code to find any updates to bring over.
 * 
 * @author Bing Ran<bing_ran@hotmail.com>
 * 
 */
public class ActionBridge {

	boolean absolute = false;

	public ActionBridge(boolean absolute) {
		this.absolute = absolute;
	}

	public Object _abs() {
		this.absolute = true;
		return this;
	}

	/**
	 * this is really to do the reverse url lookup
	 * 
	 * @param actionString
	 * @param param
	 * @return
	 */
	public ActionDefinition invokeMethod(String actionString, Object param) {
		try {

			// forms: Controller.action, action, package.Controller.action
			String action = actionString;
//			String methodName = actionString;
			if (actionString.indexOf(".") > 0) {
//				int lastIndexOf = actionString.lastIndexOf('.');
////				methodName = actionString.substring(lastIndexOf + 1);
//				controllerName = actionString.substring(0, lastIndexOf);
//				// fell spec with controller name
			} else {
				Request req = Request.current();
				if (req != null) {
					action = req.controller + "." + actionString;
				}
			}
			
			try {
				Map<String, Object> args = new HashMap<String, Object>();
				Method actionMethod = (Method) ActionInvoker.getActionMethod(action)[1];
//				String[] names = (String[]) actionMethod
//						.getDeclaringClass()
//						.getDeclaredField("$" + actionMethod.getName() + computeMethodHash(actionMethod.getParameterTypes())).get(null);
				String[] names = Java.parameterNames(actionMethod);
				if (param instanceof Object[]) {
					// too many parameters versus action, possibly a developer
					// error. we must warn him.
					if (names.length < ((Object[]) param).length) {
						throw new NoRouteFoundException(action, null);
					}
					Annotation[] annos = actionMethod.getAnnotations();
					for (int i = 0; i < ((Object[]) param).length; i++) {
						if (((Object[]) param)[i] instanceof Router.ActionDefinition && ((Object[]) param)[i] != null) {
							Unbinder.unBind(args, ((Object[]) param)[i].toString(), i < names.length ? names[i] : "", annos);
						} else if (isSimpleParam(actionMethod.getParameterTypes()[i])) {
							if (((Object[]) param)[i] != null) {
								Unbinder.unBind(args, ((Object[]) param)[i].toString(), i < names.length ? names[i] : "", annos);
							}
						} else {
							Unbinder.unBind(args, ((Object[]) param)[i], i < names.length ? names[i] : "", annos);
						}
					}
				}
				Router.ActionDefinition def = Router.reverse(action, args);
				if (absolute) {
					def.absolute();
				}

				// if (template.template.name.endsWith(".html") ||
				// template.template.name.endsWith(".xml")) {
				def.url = def.url.replace("&", "&amp;");
				// }
				return def;
			} catch (ActionNotFoundException e) {
//				throw new NoRouteFoundException(action, null);
				throw new ReverseRouteException(action);
			}
		} catch (Exception e) {
			if (e instanceof PlayException) {
				throw (PlayException) e;
			}
			
			if (e instanceof JapidRuntimeException) {
				throw (JapidRuntimeException) e;
			}
			
			
			throw new UnexpectedException(e);
		}
	}

	static boolean isSimpleParam(Class type) {
		return Number.class.isAssignableFrom(type) || type.equals(String.class) || type.isPrimitive();
	}
	
	/**
	 * copied from LVEnhancer
	 */
    public static Integer computeMethodHash(String[] parameters) {
        StringBuffer buffer = new StringBuffer();
        for (String param : parameters) {
            buffer.append(param);
        }
        Integer hash = buffer.toString().hashCode();
        if (hash < 0) {
            return -hash;
        }
        return hash;
    }

    public static Integer computeMethodHash(Class<?>[] parameters) {
        String[] names = new String[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            Class<?> param = parameters[i];
            names[i] = "";
            if (param.isArray()) {
                int level = 1;
                param = param.getComponentType();
                // Array of array
                while (param.isArray()) {
                    level++;
                    param = param.getComponentType();
                }
                names[i] = param.getName();
                for (int j = 0; j < level; j++) {
                    names[i] += "[]";
                }
            } else {
                names[i] = param.getName();
            }
        }
        return computeMethodHash(names);
    }

}