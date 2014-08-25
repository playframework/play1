//version: 0.9.37
package japidviews.myapp;
import java.util.*;
import java.io.*;
import cn.bran.japid.tags.Each;
import japidviews._layouts.*;
import japidviews._tags.*;
//
// NOTE: This file was generated from: japidviews/myapp/aview.html
// Change to this file will be lost next time the template file is compiled.
//
public class aview extends main
{
	public static final String sourceTemplate = "japidviews/myapp/aview.html";
	{
	}
	public aview() {
	super((StringBuilder)null);
	}
	public aview(StringBuilder out) {
		super(out);
	}
	public aview(cn.bran.japid.template.JapidTemplateBaseWithoutPlay caller) {
		super(caller);
	}

/* based on https://github.com/branaway/Japid/issues/12
 */
	public static final String[] argNames = new String[] {/* args of the template*/"a",  };
	public static final String[] argTypes = new String[] {/* arg types of the template*/"String",  };
	public static final Object[] argDefaults= new Object[] {null, };
	public static java.lang.reflect.Method renderMethod = getRenderMethod(japidviews.myapp.aview.class);

	{
		setRenderMethod(renderMethod);
		setArgNames(argNames);
		setArgTypes(argTypes);
		setArgDefaults(argDefaults);
		setSourceTemplate(sourceTemplate);
	}
////// end of named args stuff

	private String a; // line 1, japidviews/myapp/aview.html
	public String render(String a) {
		this.a = a;
		try {super.layout();} catch (RuntimeException __e) { super.handleException(__e);} // line 1, japidviews/myapp/aview.html
		return getRenderResult().toString();
	}

	public static String apply(String a) {
		return new aview().render(a);
	}

	@Override protected void doLayout() {
		beginDoLayout(sourceTemplate);
;// line 1, aview.html
		p("\n");// line 1, aview.html
p("\n" + 
"escaped: ");// line 3, aview.html
		try { Object o = escape(a); if (o.toString().length() ==0) { p(escape(null)); } else { p(o); } } catch (NullPointerException npe) { p(escape(null)); }// line 5, aview.html
		p("\n" + 
"nice view: ");// line 5, aview.html
		new taggy(aview.this).render(a + "1"); // line 6, aview.html// line 6, aview.html
		;// line 6, aview.html
		
		endDoLayout(sourceTemplate);
	}

	@Override protected void title() {
		p("my view");;
	}
}