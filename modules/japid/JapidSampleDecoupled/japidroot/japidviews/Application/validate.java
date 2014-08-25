//version: 0.9.5
package japidviews.Application;
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
// NOTE: This file was generated from: japidviews/Application/validate.html
// Change to this file will be lost next time the template file is compiled.
//
@cn.bran.play.NoEnhance
public class validate extends cn.bran.play.JapidTemplateBase
{
	public static final String sourceTemplate = "japidviews/Application/validate.html";
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


	public validate() {
	super((StringBuilder)null);
	initHeaders();
	}
	public validate(StringBuilder out) {
		super(out);
		initHeaders();
	}
	public validate(cn.bran.japid.template.JapidTemplateBaseWithoutPlay caller) {
		super(caller);
	}

/* based on https://github.com/branaway/Japid/issues/12
 */
	public static final String[] argNames = new String[] {/* args of the template*/"name", "age",  };
	public static final String[] argTypes = new String[] {/* arg types of the template*/"String", "Integer",  };
	public static final Object[] argDefaults= new Object[] {null,null, };
	public static java.lang.reflect.Method renderMethod = getRenderMethod(japidviews.Application.validate.class);

	{
		setRenderMethod(renderMethod);
		setArgNames(argNames);
		setArgTypes(argTypes);
		setArgDefaults(argDefaults);
		setSourceTemplate(sourceTemplate);
	}
////// end of named args stuff

	private String name; // line 1, japidviews/Application/validate.html
	private Integer age; // line 1, japidviews/Application/validate.html
	public cn.bran.japid.template.RenderResult render(String name,Integer age) {
		this.name = name;
		this.age = age;
		try {super.layout();} catch (RuntimeException __e) { super.handleException(__e);} // line 1, japidviews/Application/validate.html
		return getRenderResult();
	}

	public static cn.bran.japid.template.RenderResult apply(String name,Integer age) {
		return new validate().render(name, age);
	}

	@Override protected void doLayout() {
		beginDoLayout(sourceTemplate);
;// line 1, validate.html
		;// line 1, validate.html
		if(asBoolean(hasErrors())) {// line 3, validate.html
		p("    <p>Got some errors:</p>\n" + 
"    ");// line 3, validate.html
		for (Error e: errors()){// line 5, validate.html
		p("        <p>");// line 5, validate.html
		p(e.getKey());// line 6, validate.html
		p(" : ");// line 6, validate.html
		p(e);// line 6, validate.html
		p("</p>\n" + 
"    ");// line 6, validate.html
		}// line 7, validate.html
} else {// line 8, validate.html
		p("	name is: ");// line 8, validate.html
		p(name);// line 9, validate.html
		p(", age is: ");// line 9, validate.html
		p(age);// line 9, validate.html
		;// line 9, validate.html
		}// line 10, validate.html
		;// line 10, validate.html
		
		endDoLayout(sourceTemplate);
	}

}