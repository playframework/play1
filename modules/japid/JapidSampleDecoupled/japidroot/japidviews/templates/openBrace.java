//version: 0.9.5
package japidviews.templates;
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
// NOTE: This file was generated from: japidviews/templates/openBrace.html
// Change to this file will be lost next time the template file is compiled.
//
@cn.bran.play.NoEnhance
public class openBrace extends cn.bran.play.JapidTemplateBase
{
	public static final String sourceTemplate = "japidviews/templates/openBrace.html";
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


	public openBrace() {
	super((StringBuilder)null);
	initHeaders();
	}
	public openBrace(StringBuilder out) {
		super(out);
		initHeaders();
	}
	public openBrace(cn.bran.japid.template.JapidTemplateBaseWithoutPlay caller) {
		super(caller);
	}

/* based on https://github.com/branaway/Japid/issues/12
 */
	public static final String[] argNames = new String[] {/* args of the template*/ };
	public static final String[] argTypes = new String[] {/* arg types of the template*/ };
	public static final Object[] argDefaults= new Object[] { };
	public static java.lang.reflect.Method renderMethod = getRenderMethod(japidviews.templates.openBrace.class);

	{
		setRenderMethod(renderMethod);
		setArgNames(argNames);
		setArgTypes(argTypes);
		setArgDefaults(argDefaults);
		setSourceTemplate(sourceTemplate);
	}
////// end of named args stuff

	public cn.bran.japid.template.RenderResult render() {
		try {super.layout();} catch (RuntimeException __e) { super.handleException(__e);} // line 0, japidviews/templates/openBrace.html
		return getRenderResult();
	}

	public static cn.bran.japid.template.RenderResult apply() {
		return new openBrace().render();
	}

	@Override protected void doLayout() {
		beginDoLayout(sourceTemplate);
p("<p>hello</p>\n");// line 1, openBrace.html
		int i = 3;// line 2, openBrace.html
if(asBoolean(true)) {// line 3, openBrace.html
    while (i-- > 0) {// line 4, openBrace.html
		p("        <p>in while ");// line 4, openBrace.html
		p(i);// line 5, openBrace.html
		p("</p>\n" + 
"    ");// line 5, openBrace.html
		}// line 6, openBrace.html
		p("    <p>good</p>\n");// line 6, openBrace.html
		}// line 8, openBrace.html
		p("\n" + 
"<p>\n");// line 8, openBrace.html
		for(i =0; i < 4; i++){// line 11, openBrace.html
		p("    ");// line 11, openBrace.html
		p(i);// line 12, openBrace.html
		p(", \n");// line 12, openBrace.html
		}// line 13, openBrace.html
		p("<p/>\n" + 
"<p>good2</p>\n" + 
"\n");// line 13, openBrace.html
		if (true) {// line 17, openBrace.html
		p("    <p>good 3</p>\n");// line 17, openBrace.html
		}// line 19, openBrace.html
		p("<p/>\n" + 
"<p>\n" + 
"the result is ");// line 19, openBrace.html
		if(asBoolean(true)) {// line 22, openBrace.html
		p("got you!");// line 22, openBrace.html
		}// line 22, openBrace.html
		p(".\n" + 
"</p>\n");// line 22, openBrace.html
		
		endDoLayout(sourceTemplate);
	}

}