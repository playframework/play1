//version: 0.9.37
package japidviews.cn.bran.japid.template.FooController;
import java.util.*;
import java.io.*;
import cn.bran.japid.tags.Each;
import japidviews._layouts.*;
import japidviews._tags.*;
//
// NOTE: This file was generated from: japidviews/cn/bran/japid/template/FooController/a1.html
// Change to this file will be lost next time the template file is compiled.
//
public class a1 extends main
{
	public static final String sourceTemplate = "japidviews/cn/bran/japid/template/FooController/a1.html";
	{
	}
	public a1() {
	super((StringBuilder)null);
	}
	public a1(StringBuilder out) {
		super(out);
	}
	public a1(cn.bran.japid.template.JapidTemplateBaseWithoutPlay caller) {
		super(caller);
	}

/* based on https://github.com/branaway/Japid/issues/12
 */
	public static final String[] argNames = new String[] {/* args of the template*/"a",  };
	public static final String[] argTypes = new String[] {/* arg types of the template*/"String",  };
	public static final Object[] argDefaults= new Object[] {null, };
	public static java.lang.reflect.Method renderMethod = getRenderMethod(japidviews.cn.bran.japid.template.FooController.a1.class);

	{
		setRenderMethod(renderMethod);
		setArgNames(argNames);
		setArgTypes(argTypes);
		setArgDefaults(argDefaults);
		setSourceTemplate(sourceTemplate);
	}
////// end of named args stuff

	private String a; // line 1, japidviews/cn/bran/japid/template/FooController/a1.html
	public String render(String a) {
		this.a = a;
		try {super.layout(a + "1");} catch (RuntimeException __e) { super.handleException(__e);} // line 1, japidviews/cn/bran/japid/template/FooController/a1.html
		return getRenderResult().toString();
	}

	public static String apply(String a) {
		return new a1().render(a);
	}

	@Override protected void doLayout() {
		beginDoLayout(sourceTemplate);
;// line 1, a1.html
p("\n");// line 3, a1.html
		p(foo(a));// line 5, a1.html
		p("\n" + 
"\n");// line 5, a1.html
		// line 7, a1.html
		;// line 9, a1.html
		
		endDoLayout(sourceTemplate);
	}

	@Override protected void title() {
		p("my view");;
	}
public String foo(String ar) {
StringBuilder sb = new StringBuilder();
StringBuilder ori = getOut();
this.setOut(sb);
// line 7, a1.html
		p("  -> a1-: ");// line 7, a1.html
		new taggy(a1.this).render(ar); // line 8, a1.html// line 8, a1.html

this.setOut(ori);
	return sb.toString();
}
}