//version: 0.9.5
package japidviews.more.MyController;
import java.util.*;
import java.io.*;
import cn.bran.japid.tags.Each;
import static cn.bran.play.JapidPlayAdapter.*;
import static play.data.validation.Validation.*;
import static play.templates.JavaExtensions.*;
import play.data.validation.Error;
import play.i18n.Messages;
import play.mvc.Scope.*;
import japidviews._tags.*;
import play.data.validation.Validation;
import play.i18n.Lang;
import controllers.*;
import japidviews._layouts.*;
import models.*;
import play.mvc.Http.*;
//
// NOTE: This file was generated from: japidviews/more/MyController/doBodyInDefTag.html
// Change to this file will be lost next time the template file is compiled.
//
@cn.bran.play.NoEnhance
public class doBodyInDefTag extends cn.bran.play.JapidTemplateBase
{
	public static final String sourceTemplate = "japidviews/more/MyController/doBodyInDefTag.html";
	 private void initHeaders() {
		putHeader("Content-Type", "text/html; charset=utf-8");
		setContentType("text/html; charset=utf-8");
	}
	{
	}

// - add implicit fields with Play

	final play.mvc.Http.Request request = play.mvc.Http.Request.current(); 
	final play.mvc.Http.Response response = play.mvc.Http.Response.current(); 
	final play.mvc.Scope.Session session = play.mvc.Scope.Session.current();
	final play.mvc.Scope.RenderArgs renderArgs = play.mvc.Scope.RenderArgs.current();
	final play.mvc.Scope.Params params = play.mvc.Scope.Params.current();
	final play.data.validation.Validation validation = play.data.validation.Validation.current();
	final cn.bran.play.FieldErrors errors = new cn.bran.play.FieldErrors(validation);
	final play.Play _play = new play.Play(); 

// - end of implicit fields with Play 


	public doBodyInDefTag() {
	super((StringBuilder)null);
	initHeaders();
	}
	public doBodyInDefTag(StringBuilder out) {
		super(out);
		initHeaders();
	}
	public doBodyInDefTag(cn.bran.japid.template.JapidTemplateBaseWithoutPlay caller) {
		super(caller);
	}

/* based on https://github.com/branaway/Japid/issues/12
 */
	public static final String[] argNames = new String[] {/* args of the template*/ };
	public static final String[] argTypes = new String[] {/* arg types of the template*/ };
	public static final Object[] argDefaults= new Object[] { };
	public static java.lang.reflect.Method renderMethod = getRenderMethod(japidviews.more.MyController.doBodyInDefTag.class);

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
    try {return runRenderer(args);} catch(RuntimeException e) {handleException(e); throw e;} // line 0, japidviews/more/MyController/doBodyInDefTag.html
}

	DoBody body;
public static interface DoBody<A,B> {
		void render(A a,B b);
		void setBuffer(StringBuilder sb);
		void resetBuffer();
}
<A,B> String renderBody(A a,B b) {
		StringBuilder sb = new StringBuilder();
		if (body != null){
			body.setBuffer(sb);
			body.render( a, b);
			body.resetBuffer();
		}
		return sb.toString();
	}
	public cn.bran.japid.template.RenderResult render(DoBody body) {
		this.body = body;
		try {super.layout();} catch (RuntimeException __e) { super.handleException(__e);} // line 0, japidviews/more/MyController/doBodyInDefTag.html
		return getRenderResult();
	}
	public cn.bran.japid.template.RenderResult render() {
		try {super.layout();} catch (RuntimeException __e) { super.handleException(__e);} // line 0, japidviews/more/MyController/doBodyInDefTag.html
		return getRenderResult();
	}

	public static cn.bran.japid.template.RenderResult apply() {
		return new doBodyInDefTag().render();
	}

	@Override protected void doLayout() {
		beginDoLayout(sourceTemplate);
p("outside: ");// line 1, doBodyInDefTag.html
		if (body != null){ body.setBuffer(getOut()); body.render("taggy", 1); body.resetBuffer();}// line 1, doBodyInDefTag.html
		p("ok, try to get the content as method call: \n" + 
"   ");// line 1, doBodyInDefTag.html
		p(renderBody("taddy", 3));// line 4, doBodyInDefTag.html
		p("\n");// line 4, doBodyInDefTag.html
		// line 6, doBodyInDefTag.html
		p("call the def\n" + 
"\n");// line 8, doBodyInDefTag.html
		p(foo());// line 11, doBodyInDefTag.html
		p("\n");// line 11, doBodyInDefTag.html
		new fooTag(doBodyInDefTag.this).render(// line 13, doBodyInDefTag.html
new fooTag.DoBody(){ // line 13, doBodyInDefTag.html
public void render() { // line 13, doBodyInDefTag.html
// line 13, doBodyInDefTag.html
		p("  -> called footag:  ");// line 13, doBodyInDefTag.html
		if (body != null){ body.setBuffer(getOut()); body.render("kaddy", 13); body.resetBuffer();}// line 14, doBodyInDefTag.html

}

StringBuilder oriBuffer;
@Override
public void setBuffer(StringBuilder sb) {
	oriBuffer = getOut();
	setOut(sb);
}

@Override
public void resetBuffer() {
	setOut(oriBuffer);
}

}
);// line 13, doBodyInDefTag.html
		;// line 15, doBodyInDefTag.html
		
		endDoLayout(sourceTemplate);
	}

public String foo() {
StringBuilder sb = new StringBuilder();
StringBuilder ori = getOut();
this.setOut(sb);
TreeMap<Integer, cn.bran.japid.template.ActionRunner> parentActionRunners = actionRunners;
actionRunners = new TreeMap<Integer, cn.bran.japid.template.ActionRunner>();
// line 6, doBodyInDefTag.html
		p("	hello ");// line 6, doBodyInDefTag.html
		if (body != null){ body.setBuffer(getOut()); body.render("saddy", 2); body.resetBuffer();}// line 7, doBodyInDefTag.html

this.setOut(ori);
if (actionRunners.size() > 0) {
	StringBuilder _sb2 = new StringBuilder();
	int segStart = 0;
	for (Map.Entry<Integer, cn.bran.japid.template.ActionRunner> _arEntry : actionRunners.entrySet()) {
		int pos = _arEntry.getKey();
		_sb2.append(sb.substring(segStart, pos));
		segStart = pos;
		cn.bran.japid.template.ActionRunner _a_ = _arEntry.getValue();
		_sb2.append(_a_.run().getContent().toString());
	}
	_sb2.append(sb.substring(segStart));
	actionRunners = parentActionRunners;
	return _sb2.toString();
} else {
	actionRunners = parentActionRunners;
	return sb.toString();
}
}
}