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
// NOTE: This file was generated from: japidviews/Application/reverseUrl.html
// Change to this file will be lost next time the template file is compiled.
//
@cn.bran.play.NoEnhance
public class reverseUrl extends cn.bran.play.JapidTemplateBase
{
	public static final String sourceTemplate = "japidviews/Application/reverseUrl.html";
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


	public reverseUrl() {
	super((StringBuilder)null);
	initHeaders();
	}
	public reverseUrl(StringBuilder out) {
		super(out);
		initHeaders();
	}
	public reverseUrl(cn.bran.japid.template.JapidTemplateBaseWithoutPlay caller) {
		super(caller);
	}

/* based on https://github.com/branaway/Japid/issues/12
 */
	public static final String[] argNames = new String[] {/* args of the template*/ };
	public static final String[] argTypes = new String[] {/* arg types of the template*/ };
	public static final Object[] argDefaults= new Object[] { };
	public static java.lang.reflect.Method renderMethod = getRenderMethod(japidviews.Application.reverseUrl.class);

	{
		setRenderMethod(renderMethod);
		setArgNames(argNames);
		setArgTypes(argTypes);
		setArgDefaults(argDefaults);
		setSourceTemplate(sourceTemplate);
	}
////// end of named args stuff

	public cn.bran.japid.template.RenderResult render() {
		try {super.layout();} catch (RuntimeException __e) { super.handleException(__e);} // line 0, japidviews/Application/reverseUrl.html
		return getRenderResult();
	}

	public static cn.bran.japid.template.RenderResult apply() {
		return new reverseUrl().render();
	}

	@Override protected void doLayout() {
		beginDoLayout(sourceTemplate);
;// line 1, reverseUrl.html
		String name = "bran";// line 1, reverseUrl.html
		p("<a href=\"");// line 1, reverseUrl.html
		p(lookup("validate", name));// line 2, reverseUrl.html
		p("\">simple reverse lookup</a>\n" + 
"<p/>\n" + 
"<a href=\"");// line 2, reverseUrl.html
		p(lookupAbs("validate", name, 12));// line 4, reverseUrl.html
		p("\">simple reverse lookup with absolute url</a>\n" + 
"<p/>\n" + 
"<form action=\"");// line 4, reverseUrl.html
		p(lookup("validate", new Object[]{}));// line 6, reverseUrl.html
		p("\">\n" + 
"    <input type=\"hidden\" name=\"name\" value=\"bran\"/>\n" + 
"    <input type=\"hidden\" name=\"age\" value=\"12\"/>\n" + 
"    <input type=\"submit\"/>\n" + 
"</form>\n" + 
"<p/>\n" + 
"<a href=\"");// line 6, reverseUrl.html
		p(lookup("more.MyController.echo", name));// line 12, reverseUrl.html
		p("\">simple reverse lookup</a>\n" + 
"<p/>\n" + 
"\n" + 
"\n");// line 12, reverseUrl.html
		
		endDoLayout(sourceTemplate);
	}

}