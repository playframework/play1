//version: 0.9.37
package japidviews._tags;
import java.util.*;
import java.io.*;
import cn.bran.japid.tags.Each;
import japidviews._layouts.*;
import japidviews._tags.*;
//
// NOTE: This file was generated from: japidviews/_tags/taddy.html
// Change to this file will be lost next time the template file is compiled.
//
public class taddy extends cn.bran.japid.template.JapidTemplateBaseWithoutPlay
{
	public static final String sourceTemplate = "japidviews/_tags/taddy.html";
	{
	}
	public taddy() {
	super((StringBuilder)null);
	}
	public taddy(StringBuilder out) {
		super(out);
	}
	public taddy(cn.bran.japid.template.JapidTemplateBaseWithoutPlay caller) {
		super(caller);
	}

/* based on https://github.com/branaway/Japid/issues/12
 */
	public static final String[] argNames = new String[] {/* args of the template*/ };
	public static final String[] argTypes = new String[] {/* arg types of the template*/ };
	public static final Object[] argDefaults= new Object[] { };
	public static java.lang.reflect.Method renderMethod = getRenderMethod(japidviews._tags.taddy.class);

	{
		setRenderMethod(renderMethod);
		setArgNames(argNames);
		setArgTypes(argTypes);
		setArgDefaults(argDefaults);
		setSourceTemplate(sourceTemplate);
	}
////// end of named args stuff

	{ setHasDoBody(); }
public cn.bran.japid.template.RenderResult render(DoBody body, cn.bran.japid.compiler.NamedArgRuntime... named) {
    Object[] args = buildArgs(named, body);
    try {return runRenderer(args);} catch(RuntimeException e) {handleException(e); throw e;} // line 0, japidviews/_tags/taddy.html
}

	DoBody body;
public static interface DoBody<A> {
		void render(A a);
		void setBuffer(StringBuilder sb);
		void resetBuffer();
}
<A> String renderBody(A a) {
		StringBuilder sb = new StringBuilder();
		if (body != null){
			body.setBuffer(sb);
			body.render( a);
			body.resetBuffer();
		}
		return sb.toString();
	}
	public String render(DoBody body) {
		this.body = body;
		try {super.layout();} catch (RuntimeException __e) { super.handleException(__e);} // line 0, japidviews/_tags/taddy.html
		return getRenderResult().toString();
	}
	public String render() {
		try {super.layout();} catch (RuntimeException __e) { super.handleException(__e);} // line 0, japidviews/_tags/taddy.html
		return getRenderResult().toString();
	}

	public static String apply() {
		return new taddy().render();
	}

	@Override protected void doLayout() {
		beginDoLayout(sourceTemplate);
p("teddy bear\n" + 
"\n");// line 1, taddy.html
		String[] ss = new String[]{"a", "add", "cd"};// line 3, taddy.html

if (body != null){ body.setBuffer(getOut()); body.render(ss); body.resetBuffer();}// line 5, taddy.html
		
		endDoLayout(sourceTemplate);
	}

}