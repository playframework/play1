package cn.bran.japid.template;

import japidviews.cn.bran.japid.template.FooController.foo;

/**
 * Note:
 * 
 * FooController does not need to sub-class JapidRenderer. It does so only to
 * save typing JapidRenderer in front of all the render() call. Nor does it need
 * to be named in any specific pattern.
 * 
 * @author Bing Ran<bing_ran@hotmail.com>
 * 
 */
public class FooController extends JapidRenderer {
	public static class ModelUser {
		public String name;

		public ModelUser(String name) {
			this.name = name + "!";
		}

		public String what() {
			return ">" + name;
		}
	}

	public String a1(String p) {
		return render(p);
	}

	public String foo(String p) {
		return JapidRenderer.render(p);
	}

	public String bar(String p) {
		return JapidRenderer.renderWith(foo.class, p);
	}

//	public String bar2(String p) {
//		return new foo().render(p);
//	}

	public String tee(ModelUser u) {
		return JapidRenderer.render(u);
	}

}