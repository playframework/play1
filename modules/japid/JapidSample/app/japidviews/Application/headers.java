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
// NOTE: This file was generated from: japidviews/Application/headers.html
// Change to this file will be lost next time the template file is compiled.
//
@cn.bran.play.NoEnhance
public class headers extends superheaders
{
	public static final String sourceTemplate = "japidviews/Application/headers.html";
	 private void initHeaders() {
		putHeader("Server", "nginx/0.8.26");
		putHeader("Cache-Control", "max-age=600");
		putHeader("Expires", "Tue, 23 Feb 2010 13:47:34 GMT");
		putHeader("Last-Modified", "Tue, 23 Feb 2010 13:40:01 GMT");
		putHeader("Date", "Tue, 23 Feb 2010 13:42:34 GMT");
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


	public headers() {
	super((StringBuilder)null);
	initHeaders();
	}
	public headers(StringBuilder out) {
		super(out);
		initHeaders();
	}
	public headers(cn.bran.japid.template.JapidTemplateBaseWithoutPlay caller) {
		super(caller);
	}

/* based on https://github.com/branaway/Japid/issues/12
 */
	public static final String[] argNames = new String[] {/* args of the template*/ };
	public static final String[] argTypes = new String[] {/* arg types of the template*/ };
	public static final Object[] argDefaults= new Object[] { };
	public static java.lang.reflect.Method renderMethod = getRenderMethod(japidviews.Application.headers.class);

	{
		setRenderMethod(renderMethod);
		setArgNames(argNames);
		setArgTypes(argTypes);
		setArgDefaults(argDefaults);
		setSourceTemplate(sourceTemplate);
	}
////// end of named args stuff

	public cn.bran.japid.template.RenderResult render() {
		try {super.layout();} catch (RuntimeException __e) { super.handleException(__e);} // line 0, japidviews/Application/headers.html
		return getRenderResult();
	}

	public static cn.bran.japid.template.RenderResult apply() {
		return new headers().render();
	}

	@Override protected void doLayout() {
		beginDoLayout(sourceTemplate);
;// line 1, headers.html
p("\n" + 
"<p>\n" + 
"\"setHeader\" is for adding a http response header to the response. One cannot use \"\" to surround the value part\n" + 
"</p>\n" + 
"\n" + 
"<p>\n" + 
"The header name and the value are separated by white spaces (space or tab)\n" + 
"</p>\n" + 
"\n" + 
"<p>\n" + 
"Notes: If a response includes both an Expires header and a max-age directive, the max-age directive overrides the Expires header, even if the Expires header is more restrictive.\n" + 
"</p>\n" + 
"\n" + 
"\n");// line 6, headers.html
		
		endDoLayout(sourceTemplate);
	}

}