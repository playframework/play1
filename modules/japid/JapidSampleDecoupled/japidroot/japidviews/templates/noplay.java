//version: 0.9.5
package japidviews.templates;
import java.util.*;
import java.io.*;
import cn.bran.japid.tags.Each;
import japidviews._tags.*;
import controllers.*;
import japidviews._layouts.*;
import models.*;
//
// NOTE: This file was generated from: japidviews/templates/noplay.html
// Change to this file will be lost next time the template file is compiled.
//
@cn.bran.play.NoEnhance
public class noplay extends cn.bran.japid.template.JapidTemplateBaseWithoutPlay
{
	public static final String sourceTemplate = "japidviews/templates/noplay.html";
	{
	}
	public noplay() {
	super((StringBuilder)null);
	}
	public noplay(StringBuilder out) {
		super(out);
	}
	public noplay(cn.bran.japid.template.JapidTemplateBaseWithoutPlay caller) {
		super(caller);
	}

/* based on https://github.com/branaway/Japid/issues/12
 */
	public static final String[] argNames = new String[] {/* args of the template*/"s",  };
	public static final String[] argTypes = new String[] {/* arg types of the template*/"String",  };
	public static final Object[] argDefaults= new Object[] {null, };
	public static java.lang.reflect.Method renderMethod = getRenderMethod(japidviews.templates.noplay.class);

	{
		setRenderMethod(renderMethod);
		setArgNames(argNames);
		setArgTypes(argTypes);
		setArgDefaults(argDefaults);
		setSourceTemplate(sourceTemplate);
	}
////// end of named args stuff

	private String s; // line 2, japidviews/templates/noplay.html
	public String render(String s) {
		this.s = s;
		try {super.layout();} catch (RuntimeException __e) { super.handleException(__e);} // line 2, japidviews/templates/noplay.html
		return getRenderResult().toString();
	}

	public static String apply(String s) {
		return new noplay().render(s);
	}

	@Override protected void doLayout() {
		beginDoLayout(sourceTemplate);
;// line 1, noplay.html
p("\n" + 
"hello ");// line 2, noplay.html
		new japidviews._tags.Tag2(noplay.this).render(named("msg", s)); // line 4, noplay.html// line 4, noplay.html
		p(" !!!!\n" + 
"\n");// line 4, noplay.html
		
		endDoLayout(sourceTemplate);
	}

}