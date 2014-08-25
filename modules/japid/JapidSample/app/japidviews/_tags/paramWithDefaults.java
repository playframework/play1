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
// NOTE: This file was generated from: japidviews/_tags/paramWithDefaults.html
// Change to this file will be lost next time the template file is compiled.
//
@cn.bran.play.NoEnhance
public class paramWithDefaults extends cn.bran.play.JapidTemplateBase
{
	public static final String sourceTemplate = "japidviews/_tags/paramWithDefaults.html";
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


	public paramWithDefaults() {
	super((StringBuilder)null);
	initHeaders();
	}
	public paramWithDefaults(StringBuilder out) {
		super(out);
		initHeaders();
	}
	public paramWithDefaults(cn.bran.japid.template.JapidTemplateBaseWithoutPlay caller) {
		super(caller);
	}

/* based on https://github.com/branaway/Japid/issues/12
 */
	public static final String[] argNames = new String[] {/* args of the template*/"msg", "m2", "age",  };
	public static final String[] argTypes = new String[] {/* arg types of the template*/"String", "String", "Integer",  };
	public static final Object[] argDefaults= new Object[] {"message 1 default value",new String("m2message"),20, };
	public static java.lang.reflect.Method renderMethod = getRenderMethod(japidviews._tags.paramWithDefaults.class);

	{
		setRenderMethod(renderMethod);
		setArgNames(argNames);
		setArgTypes(argTypes);
		setArgDefaults(argDefaults);
		setSourceTemplate(sourceTemplate);
	}
////// end of named args stuff

	private String msg; // line 1, japidviews/_tags/paramWithDefaults.html
	private String m2; // line 1, japidviews/_tags/paramWithDefaults.html
	private Integer age; // line 1, japidviews/_tags/paramWithDefaults.html
	public cn.bran.japid.template.RenderResult render(String msg,String m2,Integer age) {
		this.msg = msg;
		this.m2 = m2;
		this.age = age;
		try {super.layout();} catch (RuntimeException __e) { super.handleException(__e);} // line 1, japidviews/_tags/paramWithDefaults.html
		return getRenderResult();
	}

	public static cn.bran.japid.template.RenderResult apply(String msg,String m2,Integer age) {
		return new paramWithDefaults().render(msg, m2, age);
	}

	@Override protected void doLayout() {
		beginDoLayout(sourceTemplate);
;// line 1, paramWithDefaults.html
		p("<span>");// line 5, paramWithDefaults.html
		p(msg);// line 6, paramWithDefaults.html
		p("</span>\n" + 
"<span>");// line 6, paramWithDefaults.html
		p(m2);// line 7, paramWithDefaults.html
		p("</span>\n" + 
"<span>");// line 7, paramWithDefaults.html
		p(age);// line 8, paramWithDefaults.html
		p("</span>\n" + 
"\n");// line 8, paramWithDefaults.html
		
		endDoLayout(sourceTemplate);
	}

}