//version: 0.9.5
package japidviews._tags;
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
// NOTE: This file was generated from: japidviews/_tags/argtest.html
// Change to this file will be lost next time the template file is compiled.
//
@cn.bran.play.NoEnhance
public class argtest extends cn.bran.play.JapidTemplateBase
{
	public static final String sourceTemplate = "japidviews/_tags/argtest.html";
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


	public argtest() {
	super((StringBuilder)null);
	initHeaders();
	}
	public argtest(StringBuilder out) {
		super(out);
		initHeaders();
	}
	public argtest(cn.bran.japid.template.JapidTemplateBaseWithoutPlay caller) {
		super(caller);
	}

/* based on https://github.com/branaway/Japid/issues/12
 */
	public static final String[] argNames = new String[] {/* args of the template*/"arg",  };
	public static final String[] argTypes = new String[] {/* arg types of the template*/"String",  };
	public static final Object[] argDefaults= new Object[] {null, };
	public static java.lang.reflect.Method renderMethod = getRenderMethod(japidviews._tags.argtest.class);

	{
		setRenderMethod(renderMethod);
		setArgNames(argNames);
		setArgTypes(argTypes);
		setArgDefaults(argDefaults);
		setSourceTemplate(sourceTemplate);
	}
////// end of named args stuff

	{ setHasDoBody(); }
	private String arg; // line 1, japidviews/_tags/argtest.html
public cn.bran.japid.template.RenderResult render(DoBody body, cn.bran.japid.compiler.NamedArgRuntime... named) {
    Object[] args = buildArgs(named, body);
    try {return runRenderer(args);} catch(RuntimeException e) {handleException(e); throw e;} // line 1, japidviews/_tags/argtest.html
}

	private DoBody body;
public static interface DoBody {
		void render();
		void setBuffer(StringBuilder sb);
		void resetBuffer();
}
 String renderBody() {
		StringBuilder sb = new StringBuilder();
		if (body != null){
			body.setBuffer(sb);
			body.render();
			body.resetBuffer();
		}
		return sb.toString();
	}
	public cn.bran.japid.template.RenderResult render(String arg, DoBody body) {
		this.body = body;
		this.arg = arg;
		try {super.layout();} catch (RuntimeException __e) { super.handleException(__e);} // line 1, japidviews/_tags/argtest.html
		return getRenderResult();
	}
	public cn.bran.japid.template.RenderResult render(String arg) {
		this.arg = arg;
		try {super.layout();} catch (RuntimeException __e) { super.handleException(__e);} // line 1, japidviews/_tags/argtest.html
		return getRenderResult();
	}

	public static cn.bran.japid.template.RenderResult apply(String arg) {
		return new argtest().render(arg);
	}

	@Override protected void doLayout() {
		beginDoLayout(sourceTemplate);
;// line 1, argtest.html
		;// line 1, argtest.html
		new argtest1(argtest.this).render(// line 2, argtest.html
new argtest1.DoBody(){ // line 2, argtest.html
public void render() { // line 2, argtest.html
// line 2, argtest.html
  if (body != null){ body.setBuffer(getOut()); body.render(); body.resetBuffer();}// line 3, argtest.html
		p("  '");// line 3, argtest.html
		p(arg);// line 4, argtest.html
		p("'\n");// line 4, argtest.html
		
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
);// line 2, argtest.html
		;// line 5, argtest.html
		
		endDoLayout(sourceTemplate);
	}

}