//version: 0.9.37
package japidviews.cn.bran.japid.template.FooController;
import java.util.*;
import java.io.*;
import cn.bran.japid.tags.Each;
import japidviews._layouts.*;
import japidviews._tags.*;
//
// NOTE: This file was generated from: japidviews/cn/bran/japid/template/FooController/foo.html
// Change to this file will be lost next time the template file is compiled.
//
public class foo extends main
{
	public static final String sourceTemplate = "japidviews/cn/bran/japid/template/FooController/foo.html";
	{
	}
	public foo() {
	super((StringBuilder)null);
	}
	public foo(StringBuilder out) {
		super(out);
	}
	public foo(cn.bran.japid.template.JapidTemplateBaseWithoutPlay caller) {
		super(caller);
	}

/* based on https://github.com/branaway/Japid/issues/12
 */
	public static final String[] argNames = new String[] {/* args of the template*/"a",  };
	public static final String[] argTypes = new String[] {/* arg types of the template*/"String",  };
	public static final Object[] argDefaults= new Object[] {null, };
	public static java.lang.reflect.Method renderMethod = getRenderMethod(japidviews.cn.bran.japid.template.FooController.foo.class);

	{
		setRenderMethod(renderMethod);
		setArgNames(argNames);
		setArgTypes(argTypes);
		setArgDefaults(argDefaults);
		setSourceTemplate(sourceTemplate);
	}
////// end of named args stuff

	private String a; // line 1, japidviews/cn/bran/japid/template/FooController/foo.html
	public String render(String a) {
		this.a = a;
		try {super.layout();} catch (RuntimeException __e) { super.handleException(__e);} // line 1, japidviews/cn/bran/japid/template/FooController/foo.html
		return getRenderResult().toString();
	}

	public static String apply(String a) {
		return new foo().render(a);
	}

	@Override protected void doLayout() {
		beginDoLayout(sourceTemplate);
;// line 1, foo.html
p("foo: ");// line 3, foo.html
		new taggy(foo.this).render(a + "1"); // line 4, foo.html// line 4, foo.html
		;// line 4, foo.html
		
		endDoLayout(sourceTemplate);
	}

	@Override protected void title() {
		p("my view");;
	}
}