//version: 0.9.5
package japidviews.more.Portlets;
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
// NOTE: This file was generated from: japidviews/more/Portlets/index.html
// Change to this file will be lost next time the template file is compiled.
//
@cn.bran.play.NoEnhance
public class index extends cn.bran.play.JapidTemplateBase
{
	public static final String sourceTemplate = "japidviews/more/Portlets/index.html";
	 private void initHeaders() {
		putHeader("Content-Type", "text/html; charset=utf-8");
		setContentType("text/html; charset=utf-8");
	}
	{
		setTraceFile(true);
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


	public index() {
	super((StringBuilder)null);
	initHeaders();
	}
	public index(StringBuilder out) {
		super(out);
		initHeaders();
	}
	public index(cn.bran.japid.template.JapidTemplateBaseWithoutPlay caller) {
		super(caller);
	}

/* based on https://github.com/branaway/Japid/issues/12
 */
	public static final String[] argNames = new String[] {/* args of the template*/"a", "b",  };
	public static final String[] argTypes = new String[] {/* arg types of the template*/"String", "String",  };
	public static final Object[] argDefaults= new Object[] {null,null, };
	public static java.lang.reflect.Method renderMethod = getRenderMethod(japidviews.more.Portlets.index.class);

	{
		setRenderMethod(renderMethod);
		setArgNames(argNames);
		setArgTypes(argTypes);
		setArgDefaults(argDefaults);
		setSourceTemplate(sourceTemplate);
	}
////// end of named args stuff

	private String a; // line 2, japidviews/more/Portlets/index.html
	private String b; // line 2, japidviews/more/Portlets/index.html
	public cn.bran.japid.template.RenderResult render(String a,String b) {
		this.a = a;
		this.b = b;
		setStopwatchOn();
		try {super.layout();} catch (RuntimeException __e) { super.handleException(__e);} // line 2, japidviews/more/Portlets/index.html
		return getRenderResult();
	}

	public static cn.bran.japid.template.RenderResult apply(String a,String b) {
		return new index().render(a, b);
	}

	@Override protected void doLayout() {
		beginDoLayout(sourceTemplate);
;// line 1, index.html
		;// line 1, index.html
		;// line 2, index.html
		p("<h1>To demonstrate various ways to composing complex pages with cached <b>invoke</b></h1>\n" + 
"<h3>Each parts render themselves with own cache control</h3>\n" + 
"\n" + 
"<p>The outer most content is cached for 20 seconds, using the CacheFor annotation. <em>");// line 3, index.html
		p(new Date());// line 7, index.html
		p("</em></p>\n" + 
"\n" + 
"<p>this part is never cached: \n");// line 7, index.html
				actionRunners.put(getOut().length(), new cn.bran.play.CacheablePlayActionRunner("", controllers.more.Portlets.class, "panel1", "\"never cached\"") {
			@Override
			public void runPlayAction() throws cn.bran.play.JapidResult {
				controllers.more.Portlets.panel1("\"never cached\""); // line 10, index.html
			}
		}); p("\n");// line 10, index.html
		p(" \n" + 
"</p>\n" + 
"\n" + 
"<p>this part is cached for 10 seconds. Note the timeout spec with invoke overrides CacheFor annotation. \n");// line 10, index.html
				actionRunners.put(getOut().length(), new cn.bran.play.CacheablePlayActionRunner("10s", controllers.more.Portlets.class, "panel2", b) {
			@Override
			public void runPlayAction() throws cn.bran.play.JapidResult {
				controllers.more.Portlets.panel2(b); // line 14, index.html
			}
		}); p("\n");// line 14, index.html
		p("</p>\n" + 
"\n" + 
"<div>\n" + 
" <a href=\"evict2\">Let's evict the panel2 cache!</a>\n" + 
"</div>\n" + 
"\n" + 
"<div>\n" + 
"    <p>this part is cached for 4 seconds, \n" + 
"    specified with CacheFor annotation in the controller. \n" + 
"    ");// line 14, index.html
				actionRunners.put(getOut().length(), new cn.bran.play.CacheablePlayActionRunner("", controllers.more.Portlets.class, "panel3", a + b) {
			@Override
			public void runPlayAction() throws cn.bran.play.JapidResult {
				controllers.more.Portlets.panel3(a + b); // line 24, index.html
			}
		}); p("\n");// line 24, index.html
		p("    </p>\n" + 
"</div>\n" + 
"<div>\n" + 
" <a href=\"evict3\">Let's evict the panel3 cache!</a>\n" + 
"</div>\n");// line 24, index.html
		System.out.println("japidviews/more/Portlets/index.html(line 30): " + "-- is this cool?");
		
		endDoLayout(sourceTemplate);
	}

}