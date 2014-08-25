//version: 0.9.37
package japidviews.cn.bran.japid.template.FooController;
import java.util.*;
import java.io.*;
import cn.bran.japid.tags.Each;
import japidviews._layouts.*;
import japidviews._tags.*;
//
// NOTE: This file was generated from: japidviews/cn/bran/japid/template/FooController/tee.html
// Change to this file will be lost next time the template file is compiled.
//
public class tee extends cn.bran.japid.template.JapidTemplateBaseWithoutPlay
{
	public static final String sourceTemplate = "japidviews/cn/bran/japid/template/FooController/tee.html";
	{
	}
	public tee() {
	super((StringBuilder)null);
	}
	public tee(StringBuilder out) {
		super(out);
	}
	public tee(cn.bran.japid.template.JapidTemplateBaseWithoutPlay caller) {
		super(caller);
	}

/* based on https://github.com/branaway/Japid/issues/12
 */
	public static final String[] argNames = new String[] {/* args of the template*/"u",  };
	public static final String[] argTypes = new String[] {/* arg types of the template*/"cn.bran.japid.template.FooController.ModelUser",  };
	public static final Object[] argDefaults= new Object[] {null, };
	public static java.lang.reflect.Method renderMethod = getRenderMethod(japidviews.cn.bran.japid.template.FooController.tee.class);

	{
		setRenderMethod(renderMethod);
		setArgNames(argNames);
		setArgTypes(argTypes);
		setArgDefaults(argDefaults);
		setSourceTemplate(sourceTemplate);
	}
////// end of named args stuff

	private cn.bran.japid.template.FooController.ModelUser u; // line 1, japidviews/cn/bran/japid/template/FooController/tee.html
	public String render(cn.bran.japid.template.FooController.ModelUser u) {
		this.u = u;
		try {super.layout();} catch (RuntimeException __e) { super.handleException(__e);} // line 1, japidviews/cn/bran/japid/template/FooController/tee.html
		return getRenderResult().toString();
	}

	public static String apply(cn.bran.japid.template.FooController.ModelUser u) {
		return new tee().render(u);
	}

	@Override protected void doLayout() {
		beginDoLayout(sourceTemplate);
;// line 1, tee.html
		p("\n" + 
"Hi: ");// line 1, tee.html
		p(u.what());// line 3, tee.html
		p("\n");// line 3, tee.html
		
		endDoLayout(sourceTemplate);
	}

}