package play.test.models;

import java.util.HashMap;
import java.util.Map;

public class ClassWithStaticMap {
	public static final Map<String, Object> map = new HashMap<>();
	
	public String name;
	
	public ClassWithStaticMap() {
		super();
	}
}
