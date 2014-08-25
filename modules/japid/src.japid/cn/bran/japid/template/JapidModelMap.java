package cn.bran.japid.template;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import cn.bran.japid.util.StringUtils;

/**
 * 
 * @author Bing Ran<bing_ran@hotmail.com>
 * @deprecated not used
 */
public class JapidModelMap {
	Map<String, Object> map = new HashMap<String, Object>();
	
	public JapidModelMap put(String key, Object val) {
		map.put(key, val);
		return this;
	}


	public Object[] buildArgs(String[] argNames) {
		Object[] ret = new Object[argNames.length];
		
		for (int i = 0; i < argNames.length; i++) {
			ret[i] = map.remove(argNames[i]);
		}
		if (map.size() > 0) {
			Set<String> keys = map.keySet();
			String sep = ", ";
			String ks = "[" + StringUtils.join(keys, sep) + "]";
			String vs = "[" + StringUtils.join(argNames, sep) + "]";
			throw new RuntimeException("One or more argument names are not valid: " + ks + ". Valid argument names are: " + vs);
		}
		return ret;
	}
}
