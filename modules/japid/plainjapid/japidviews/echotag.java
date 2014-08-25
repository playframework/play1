//version: 0.9.37
package japidviews;
import java.util.*;
import java.io.*;
import cn.bran.japid.tags.Each;
import japidviews._layouts.*;
import japidviews._tags.*;
//
// NOTE: This file was generated from: japidviews/echotag.html
// Change to this file will be lost next time the template file is compiled.
//
public class echotag extends cn.bran.japid.template.JapidTemplateBaseWithoutPlay
{
	public static final String sourceTemplate = "japidviews/echotag.html";
	{
	}
	public echotag() {
	super((StringBuilder)null);
	}
	public echotag(StringBuilder out) {
		super(out);
	}
	public echotag(cn.bran.japid.template.JapidTemplateBaseWithoutPlay caller) {
		super(caller);
	}

/* based on https://github.com/branaway/Japid/issues/12
 */
	public static final String[] argNames = new String[] {/* args of the template*/"s",  };
	public static final String[] argTypes = new String[] {/* arg types of the template*/"String",  };
	public static final Object[] argDefaults= new Object[] {null, };
	public static java.lang.reflect.Method renderMethod = getRenderMethod(japidviews.echotag.class);

	{
		setRenderMethod(renderMethod);
		setArgNames(argNames);
		setArgTypes(argTypes);
		setArgDefaults(argDefaults);
		setSourceTemplate(sourceTemplate);
	}
////// end of named args stuff

	private String s; // line 1, japidviews/echotag.html
	public String render(String s) {
		this.s = s;
		try {super.layout();} catch (RuntimeException __e) { super.handleException(__e);} // line 1, japidviews/echotag.html
		return getRenderResult().toString();
	}

	public static String apply(String s) {
		return new echotag().render(s);
	}

	@Override protected void doLayout() {
		beginDoLayout(sourceTemplate);
;// line 1, echotag.html
		p("\n" + 
"echotag: ");// line 1, echotag.html
		p(s);// line 2, echotag.html
		;// line 2, echotag.html
		
		endDoLayout(sourceTemplate);
	}

}