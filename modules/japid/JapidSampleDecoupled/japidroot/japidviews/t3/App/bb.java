//version: 0.9.5
package japidviews.t3.App;
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
// NOTE: This file was generated from: japidviews/t3/App/bb.html
// Change to this file will be lost next time the template file is compiled.
//
@cn.bran.play.NoEnhance
public class bb extends cn.bran.play.JapidTemplateBase
{
	public static final String sourceTemplate = "japidviews/t3/App/bb.html";
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


	public bb() {
	super((StringBuilder)null);
	initHeaders();
	}
	public bb(StringBuilder out) {
		super(out);
		initHeaders();
	}
	public bb(cn.bran.japid.template.JapidTemplateBaseWithoutPlay caller) {
		super(caller);
	}

/* based on https://github.com/branaway/Japid/issues/12
 */
	public static final String[] argNames = new String[] {/* args of the template*/"a", "b",  };
	public static final String[] argTypes = new String[] {/* arg types of the template*/"int", "String",  };
	public static final Object[] argDefaults= new Object[] {null,null, };
	public static java.lang.reflect.Method renderMethod = getRenderMethod(japidviews.t3.App.bb.class);

	{
		setRenderMethod(renderMethod);
		setArgNames(argNames);
		setArgTypes(argTypes);
		setArgDefaults(argDefaults);
		setSourceTemplate(sourceTemplate);
	}
////// end of named args stuff

	private int a; // line 1, japidviews/t3/App/bb.html
	private String b; // line 1, japidviews/t3/App/bb.html
	public cn.bran.japid.template.RenderResult render(int a,String b) {
		this.a = a;
		this.b = b;
		try {super.layout();} catch (RuntimeException __e) { super.handleException(__e);} // line 1, japidviews/t3/App/bb.html
		return getRenderResult();
	}

	public static cn.bran.japid.template.RenderResult apply(int a,String b) {
		return new bb().render(a, b);
	}

	@Override protected void doLayout() {
		beginDoLayout(sourceTemplate);
;// line 1, bb.html
		p("hi: ");// line 1, bb.html
		p(a);// line 2, bb.html
		p(" & b ... reversed:: \"");// line 2, bb.html
		p(lookup("t3.App.bb", a, b));// line 2, bb.html
		p("\" \n" + 
"\n" + 
"or the path manually: \"");// line 2, bb.html
		p(play.Play.ctxPath);// line 4, bb.html
		p("/t3.App.bb/");// line 4, bb.html
		p(a);// line 4, bb.html
		p("/");// line 4, bb.html
		p(b);// line 4, bb.html
		p(".html\"\n" + 
"\n");// line 4, bb.html
		
		endDoLayout(sourceTemplate);
	}

}