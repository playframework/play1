//version: 0.9.5
package japidviews.AutoPather;
import java.util.*;
import java.io.*;
import cn.bran.japid.tags.Each;
import static cn.bran.play.JapidPlayAdapter.*;
import static play.data.validation.Validation.*;
import static play.templates.JavaExtensions.*;
import play.data.validation.Error;
import play.i18n.Messages;
import play.mvc.Scope.*;
import play.data.validation.Validation;
import play.i18n.Lang;
import controllers.*;
import models.*;
import play.mvc.Http.*;
//
// NOTE: This file was generated from: japidviews/AutoPather/foo.html
// Change to this file will be lost next time the template file is compiled.
//
@cn.bran.play.NoEnhance
public class foo extends cn.bran.play.JapidTemplateBase
{
	public static final String sourceTemplate = "japidviews/AutoPather/foo.html";
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


	public foo() {
	super((StringBuilder)null);
	initHeaders();
	}
	public foo(StringBuilder out) {
		super(out);
		initHeaders();
	}
	public foo(cn.bran.japid.template.JapidTemplateBaseWithoutPlay caller) {
		super(caller);
	}

/* based on https://github.com/branaway/Japid/issues/12
 */
	public static final String[] argNames = new String[] {/* args of the template*/"a", "b",  };
	public static final String[] argTypes = new String[] {/* arg types of the template*/"String", "int",  };
	public static final Object[] argDefaults= new Object[] {null,null, };
	public static java.lang.reflect.Method renderMethod = getRenderMethod(japidviews.AutoPather.foo.class);

	{
		setRenderMethod(renderMethod);
		setArgNames(argNames);
		setArgTypes(argTypes);
		setArgDefaults(argDefaults);
		setSourceTemplate(sourceTemplate);
	}
////// end of named args stuff

	private String a; // line 1, japidviews/AutoPather/foo.html
	private int b; // line 1, japidviews/AutoPather/foo.html
	public cn.bran.japid.template.RenderResult render(String a,int b) {
		this.a = a;
		this.b = b;
		try {super.layout();} catch (RuntimeException __e) { super.handleException(__e);} // line 1, japidviews/AutoPather/foo.html
		return getRenderResult();
	}

	public static cn.bran.japid.template.RenderResult apply(String a,int b) {
		return new foo().render(a, b);
	}

	@Override protected void doLayout() {
		beginDoLayout(sourceTemplate);
;// line 1, foo.html
		p("great content here again: ");// line 1, foo.html
		p(a);// line 2, foo.html
		p(" & ");// line 2, foo.html
		p(b);// line 2, foo.html
		;// line 2, foo.html
		
		endDoLayout(sourceTemplate);
	}

}