package cn.bran.play;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;

import play.cache.CacheFor;
import play.mvc.After;
import play.mvc.Before;
import play.mvc.Controller;
import play.mvc.Finally;
import cn.bran.japid.template.RenderResult;

/**
 * The class defines a cached adapter to invoke a Play controller action. A new instance
 * must be used for each action invocation.
 * 
 * This class can be used in template, indirectly by Japid engine.
 * 
 * @author Bing Ran<bing_ran@hotmail.com>
 * 
 */
public abstract class CacheablePlayActionRunner extends CacheableRunner {
//	String controllerActionString;
//	private CacheFor cacheFor;
//	private boolean gotFromCacheForCache;
//	private String cacheForVal;
	private Class<? extends Controller> controllerClass;
	private String actionName;
	
	/**
	 * 
	 * @param ttl
	 * @param args
	 * @deprecated use the other constructor that allow for better integration with CacheFor annotation
	 */
	private CacheablePlayActionRunner(String ttl, Object... args) {
		super(ttl, args);
//		if (args != null && args.length > 0) {
//			// the first argument is the methodInvocation string, like myController.myMethod
//			controllerActionString = (String) args[0];
//		}
	}
	
	public CacheablePlayActionRunner(String ttl, Class<? extends Controller> controllerClass, String actionName, Object... args) {
		this.controllerClass = controllerClass;
		this.actionName = actionName;
		Object[] fullArgs = buildCacheKeyParts(controllerClass, actionName, args);
		super.init(ttl, fullArgs);
	}

	public CacheablePlayActionRunner(String ttl) {
		super(ttl);
	}
	
	/**
	 * the action invocation to Play controller action, which must throw a
	 * JapidResult
	 * 
	 * @throws JapidResult
	 */
	protected abstract void runPlayAction() throws JapidResult;

	@Override
	protected RenderResult render() {
		Map<String, String> threadData = JapidController.threadData.get();
		try {
			play.classloading.enhancers.ControllersEnhancer.ControllerInstrumentation.initActionCall();
			threadData.put(JapidPlugin.ACTION_METHOD, controllerClass.getName() + "." + actionName);
			runPlayAction();
			throw new RuntimeException(
					"No render result from running play action. Probably the action was not using Japid templates.");
		} catch (JapidResult jr) {
			return jr.getRenderResult();
		} finally {
			threadData.remove(JapidPlugin.ACTION_METHOD);
		}
	}

	/**
	 * @return
	 */
	protected boolean shouldCache() {
		fillCacheFor(controllerClass, actionName);
		return super.shouldCache();
	}

//	/**
//	 * @param jr
//	 */
//	@Override
//	protected void cacheResult(RenderResult jr) {
//		if (shouldCache()) {
//			// already cached
//		}
//		else {
//			super.cacheResult(jr);
//		}
//	}


//	/**
//	 * check the cache for the action. The cache should have been caused by the CacheFor annotation
//	 * 
//	 * @param class1
//	 * @param actionName
//	 * @deprecated the logic is not robust. new logic has been implemented in the super class.  
//	 * 
//	 */
//	protected void checkActionCacheFor(Class<? extends JapidController> class1, String actionName) {
//		fillCacheFor(class1, actionName);
//		
//		if (cacheForVal != null && cacheForVal.length() > 0) {
//			String key = super.keyString;
//			try {
////			Object v = play.cache.Cache.get(key);
//				 RenderResult v = RenderResultCache.get(key);
//				if (v != null) {
//					System.out.println("got from cache");
//					this.gotFromCacheForCache = true;
//					throw new JapidResult(v);
////					if (v instanceof JapidResult) {
////						throw ((JapidResult) v);
////					} else if (v instanceof CachedRenderResult) {
////						throw new JapidResult(((CachedRenderResult) v).rr);
////					} else {
//////								throw new RuntimeException("got something from the cache but not sure what it is: "
//////										+ v.getClass().getName());
////					}
//				}
//			} catch (ShouldRefreshException e) {
//			}
//			return;
//		}				
//	}

	/**
	 * Keep a cache of the TTL spec of a CacheFor annotation on a controller action
	 * 
	 * @param class1
	 * @param actionName
	 */
	private void fillCacheFor(Class<?> class1, String actionName) {
		String className = class1.getName();
		String cacheForKey = className + "_" + actionName;
		String cacheForVal = (String) JapidPlugin.getCache().get(cacheForKey);
		if (cacheForVal == null) {
			// the cache has not been filled up yet.
			Method[] mths = class1.getDeclaredMethods();
			for (Method m : mths) {
				if (m.getName().equalsIgnoreCase(actionName) && Modifier.isPublic(m.getModifiers())) {
					if (!m.isAnnotationPresent(Before.class) && !m.isAnnotationPresent(After.class) && !m.isAnnotationPresent(Finally.class)) {
						CacheFor cacheFor = m.getAnnotation(CacheFor.class);
						if (cacheFor == null) {
							// well no annotation level cache spec
							cacheForVal = "";
						}
						else {
							cacheForVal = cacheFor.value();
						}
						JapidPlugin.getCache().put(cacheForKey, cacheForVal);
					}
				}
			}
		}
		
		if (cacheForVal.length() > 0){
			super.noCache = false;
			// only override ttlAbs if it's not specified.
			if (super.ttlAbs == null || super.ttlAbs.length() == 0)
				super.ttlAbs = cacheForVal;
			
		}
	}

}
