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
// NOTE: This file was generated from: japidviews/Application/reverseLookup0.html
// Change to this file will be lost next time the template file is compiled.
//
@cn.bran.play.NoEnhance
public class reverseLookup0 extends cn.bran.play.JapidTemplateBase
{
	public static final String sourceTemplate = "japidviews/Application/reverseLookup0.html";
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


	public reverseLookup0() {
	super((StringBuilder)null);
	initHeaders();
	}
	public reverseLookup0(StringBuilder out) {
		super(out);
		initHeaders();
	}
	public reverseLookup0(cn.bran.japid.template.JapidTemplateBaseWithoutPlay caller) {
		super(caller);
	}

/* based on https://github.com/branaway/Japid/issues/12
 */
	public static final String[] argNames = new String[] {/* args of the template*/ };
	public static final String[] argTypes = new String[] {/* arg types of the template*/ };
	public static final Object[] argDefaults= new Object[] { };
	public static java.lang.reflect.Method renderMethod = getRenderMethod(japidviews.Application.reverseLookup0.class);

	{
		setRenderMethod(renderMethod);
		setArgNames(argNames);
		setArgTypes(argTypes);
		setArgDefaults(argDefaults);
		setSourceTemplate(sourceTemplate);
	}
////// end of named args stuff

	public cn.bran.japid.template.RenderResult render() {
		try {super.layout();} catch (RuntimeException __e) { super.handleException(__e);} // line 0, japidviews/Application/reverseLookup0.html
		return getRenderResult();
	}

	public static cn.bran.japid.template.RenderResult apply() {
		return new reverseLookup0().render();
	}

	@Override protected void doLayout() {
		beginDoLayout(sourceTemplate);
p("<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\">\n" + 
"<html>\n" + 
"<head>\n" + 
"<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">\n" + 
"<title>Insert title here</title>\n" + 
"</head>\n" + 
"<body>\n" + 
"<h1>actions</h1>\n" + 
"<p><a href='");// line 1, reverseLookup0.html
		p(lookup("japid.SampleController.reverseLookup0", new Object[]{}));// line 9, reverseLookup0.html
		p("'>Action notation </a></p>\n" + 
"<p><a href='/japid.SampleController/reverseLookup1?agrs=order0&args=order2'>action reverse lookup cannot handle arrays or collections...</a></p>\n" + 
"</body>\n" + 
"</html>");// line 9, reverseLookup0.html
		
		endDoLayout(sourceTemplate);
	}

}