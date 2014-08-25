package cn.bran.japid.template;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class BeanInner implements Serializable {
	String a = "";

	Map<String, ActionRunner> map = new HashMap<String, ActionRunner>();
	
	
	public BeanInner() {
		ActionRunner ar = new ActionRunner() {
			public RenderResult run() {
				return null;
			}
		};
		map.put(a, ar);
	}
}
