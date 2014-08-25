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
// NOTE: This file was generated from: japidviews/templates/aTag.html
// Change to this file will be lost next time the template file is compiled.
//
@cn.bran.play.NoEnhance
public class aTag extends cn.bran.play.JapidTemplateBase
{
	public static final String sourceTemplate = "japidviews/templates/aTag.html";
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


	public aTag() {
	super((StringBuilder)null);
	initHeaders();
	}
	public aTag(StringBuilder out) {
		super(out);
		initHeaders();
	}
	public aTag(cn.bran.japid.template.JapidTemplateBaseWithoutPlay caller) {
		super(caller);
	}

/* based on https://github.com/branaway/Japid/issues/12
 */
	public static final String[] argNames = new String[] {/* args of the template*/"strings",  };
	public static final String[] argTypes = new String[] {/* arg types of the template*/"List<String>",  };
	public static final Object[] argDefaults= new Object[] {null, };
	public static java.lang.reflect.Method renderMethod = getRenderMethod(japidviews.templates.aTag.class);

	{
		setRenderMethod(renderMethod);
		setArgNames(argNames);
		setArgTypes(argTypes);
		setArgDefaults(argDefaults);
		setSourceTemplate(sourceTemplate);
	}
////// end of named args stuff

	private List<String> strings; // line 1, japidviews/templates/aTag.html
	public cn.bran.japid.template.RenderResult render(List<String> strings) {
		this.strings = strings;
		try {super.layout();} catch (RuntimeException __e) { super.handleException(__e);} // line 1, japidviews/templates/aTag.html
		return getRenderResult();
	}

	public static cn.bran.japid.template.RenderResult apply(List<String> strings) {
		return new aTag().render(strings);
	}

	@Override protected void doLayout() {
		beginDoLayout(sourceTemplate);
;// line 1, aTag.html
		p("\n" + 
"<p>hi: ");// line 1, aTag.html
		p("hiiii:" + join(strings, "|"));// line 3, aTag.html
		p("</p>\n" + 
"\n" + 
"<p>hi2: ");// line 3, aTag.html
		p("hi:" + join(strings, "|"));// line 5, aTag.html
		p("</p>\n" + 
"\n" + 
"<p>hi3: ");// line 5, aTag.html
		p("hi:" + join(strings, "|"));// line 7, aTag.html
		p("</p>\n" + 
"\n" + 
"\n" + 
"Note: the join() is defined in the JavaExtensions class in the Play! framework, which is automatically imported. ");// line 7, aTag.html
		
		endDoLayout(sourceTemplate);
	}

}