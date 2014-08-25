//version: 0.9.5
package japidviews.Application;
import java.util.*;
import java.io.*;
import cn.bran.japid.tags.Each;
import cn.bran.japid.template.ActionRunner;
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
// NOTE: This file was generated from: japidviews/Application/testCacheFor.html
// Change to this file will be lost next time the template file is compiled.
//
@cn.bran.play.NoEnhance
public class testCacheFor extends cn.bran.play.JapidTemplateBase
{
	public static final String sourceTemplate = "japidviews/Application/testCacheFor.html";
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


	public testCacheFor() {
	super((StringBuilder)null);
	initHeaders();
	}
	public testCacheFor(StringBuilder out) {
		super(out);
		initHeaders();
	}
	public testCacheFor(cn.bran.japid.template.JapidTemplateBaseWithoutPlay caller) {
		super(caller);
	}

/* based on https://github.com/branaway/Japid/issues/12
 */
	public static final String[] argNames = new String[] {/* args of the template*/"a",  };
	public static final String[] argTypes = new String[] {/* arg types of the template*/"String",  };
	public static final Object[] argDefaults= new Object[] {null, };
	public static java.lang.reflect.Method renderMethod = getRenderMethod(japidviews.Application.testCacheFor.class);

	{
		setRenderMethod(renderMethod);
		setArgNames(argNames);
		setArgTypes(argTypes);
		setArgDefaults(argDefaults);
		setSourceTemplate(sourceTemplate);
	}
////// end of named args stuff

	private String a; // line 1, japidviews/Application/testCacheFor.html
	public cn.bran.japid.template.RenderResult render(String a) {
		this.a = a;
		try {super.layout();} catch (RuntimeException __e) { super.handleException(__e);} // line 1, japidviews/Application/testCacheFor.html
		return getRenderResult();
	}

	public static cn.bran.japid.template.RenderResult apply(String a) {
		return new testCacheFor().render(a);
	}

	@Override protected void doLayout() {
		beginDoLayout(sourceTemplate);
;// line 1, testCacheFor.html
		p("\n" + 
"<html>\n" + 
"\n" + 
"<body>\n" + 
"<p>heheh</p>\n" + 
"\n" + 
"	<p>hello ");// line 1, testCacheFor.html
		p(a);// line 8, testCacheFor.html
		p(", ");// line 8, testCacheFor.html
				actionRunners.put(getOut().length(), new cn.bran.play.CacheablePlayActionRunner("", Application.class, "every3", "") {
			@Override
			public void runPlayAction() throws cn.bran.play.JapidResult {
				Application.every3(); // line 8, testCacheFor.html
			}
		}); p("\n");// line 8, testCacheFor.html
		p(",</p> \n" + 
"	<p>directly, now seconds is ");// line 8, testCacheFor.html
				actionRunners.put(getOut().length(), new cn.bran.play.CacheablePlayActionRunner("", Application.class, "seconds", "") {
			@Override
			public void runPlayAction() throws cn.bran.play.JapidResult {
				Application.seconds(); // line 9, testCacheFor.html
			}
		}); p("\n");// line 9, testCacheFor.html
		p("</p>\n" + 
"\n" + 
"</body>\n" + 
"</html>");// line 9, testCacheFor.html
		
		endDoLayout(sourceTemplate);
	}

}