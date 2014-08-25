package cn.bran.play;

import play.mvc.Scope.Params;


/**
 * wraps the Play! {@code Params} class for use in html
 * @author bran
 *
 */
public class ParamsWrapper {
	public String get(String key) {
		return p().get(key);
	}
	
	private Params p() {
		return Params.current();
	}
	
	public String[] getAll(String k){
		return p().getAll(k);
	}
}
