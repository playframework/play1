//version: 0.9.5
package japidviews;
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
// NOTE: This file was generated from: japidviews/error500.html
// Change to this file will be lost next time the template file is compiled.
//
@cn.bran.play.NoEnhance
public class error500 extends cn.bran.play.JapidTemplateBase
{
	public static final String sourceTemplate = "japidviews/error500.html";
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


	public error500() {
	super((StringBuilder)null);
	initHeaders();
	}
	public error500(StringBuilder out) {
		super(out);
		initHeaders();
	}
	public error500(cn.bran.japid.template.JapidTemplateBaseWithoutPlay caller) {
		super(caller);
	}

/* based on https://github.com/branaway/Japid/issues/12
 */
	public static final String[] argNames = new String[] {/* args of the template*/"ex",  };
	public static final String[] argTypes = new String[] {/* arg types of the template*/"Exception",  };
	public static final Object[] argDefaults= new Object[] {null, };
	public static java.lang.reflect.Method renderMethod = getRenderMethod(japidviews.error500.class);

	{
		setRenderMethod(renderMethod);
		setArgNames(argNames);
		setArgTypes(argTypes);
		setArgDefaults(argDefaults);
		setSourceTemplate(sourceTemplate);
	}
////// end of named args stuff

	private Exception ex; // line 1, japidviews/error500.html
	public cn.bran.japid.template.RenderResult render(Exception ex) {
		this.ex = ex;
		try {super.layout();} catch (RuntimeException __e) { super.handleException(__e);} // line 1, japidviews/error500.html
		return getRenderResult();
	}

	public static cn.bran.japid.template.RenderResult apply(Exception ex) {
		return new error500().render(ex);
	}

	@Override protected void doLayout() {
		beginDoLayout(sourceTemplate);
;// line 1, error500.html
		p("<html>\n" + 
"<head>\n" + 
"<title>Bad....</title>\n" + 
"</head>\n" + 
"<body>\n" + 
"	A 500 error: ");// line 1, error500.html
		p(ex);// line 7, error500.html
		p("\n" + 
"</body>\n" + 
"</html>");// line 7, error500.html
		
		endDoLayout(sourceTemplate);
	}

}