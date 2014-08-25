package cn.bran.play;

import play.mvc.Scope.RenderArgs;

/**
 * wraps the Play! {@Code RenderArgs}
 * @author bran
 *
 */
public class RenderArgsWrapper {
	private RenderArgs r() {
		return RenderArgs.current();
	}
	
	public Object get(String k) {
		return r().get(k);
	}

	public <T> T get(String k, Class<T> t) {
		return r().get(k, t);
	}
}
