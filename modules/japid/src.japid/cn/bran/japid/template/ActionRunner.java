package cn.bran.japid.template;

import java.io.Serializable;


/**
 * wrap the full body of a action controller
 * 
 * @author Bing Ran<bing_ran@hotmail.com>
 * 
 */
public abstract class ActionRunner implements Serializable{
	// public abstract void setArgs(Object... objects);

	// cannot set the args from run, because there is no way to know the types
	// in advance
	// /**
	// * create a RenderResult. It SHOULD us the arguments set in the setArgs()
	// * only, so the runtime behavior can be controllered by setArgs more than
	// * once.
	// *
	// * @return
	// */
	public abstract RenderResult run();

}
