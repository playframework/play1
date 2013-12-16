package play.templates;

import groovy.lang.Closure;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import play.mvc.Scope.Flash;

public class TagsTest {

	@Test
	public void testFieldTagFlash() {
		String name = "Dean Hiller";  //the input value originally
		MyClosure body = new MyClosure("this");
		Map args = new HashMap();
		args.put("arg", "user.name");
		String flashKey = args.get("arg")+"";
		Flash.current.set(new Flash());
		Flash.current().put(flashKey, name);
		FastTags._field(args, body, null, null, 0);
		
		Map<String, Object> field = (Map<String, Object>) body.getCache();
		String actualName = field.get("value")+"";
		Assert.assertEquals(name, actualName);
	}
	
	private static class MyClosure extends Closure {

		private Object cached;

		public MyClosure(Object owner) {
			super(owner);
		}

		@Override
		public Object call() {
			return null;
		}

		@Override
		public Object getProperty(String property) {
			return null;
		}

		@Override
		public void setProperty(String property, Object newValue) {
			cached = newValue;
		}
		
		public Object getCache() {
			return cached;
		}
	}
}
