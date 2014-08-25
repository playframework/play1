package cn.bran.japid.template;


public class FooControllerBare {
	public String a1(String p) {
		return JapidRenderer.render(p);
	}
	public String a2(String p) {
		// the same as in the a1() method
		return JapidRenderer.renderWith(this.getClass().getName() + ".a1", p);
	}

	public String a3(String p) {
		// the same as in the a1() method
		return JapidRenderer.renderWith(japidviews.cn.bran.japid.template.FooControllerBare.a1.class, p);
	}
}