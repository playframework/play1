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
// NOTE: This file was generated from: japidviews/templates/callPicka.html
// Change to this file will be lost next time the template file is compiled.
//
@cn.bran.play.NoEnhance
public class callPicka extends cn.bran.play.JapidTemplateBase
{
	public static final String sourceTemplate = "japidviews/templates/callPicka.html";
	 private void initHeaders() {
		putHeader("Content-Type", "text/html; charset=utf-8");
		setContentType("text/html; charset=utf-8");
	}
	{
		setTraceFile(false);
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


	public callPicka() {
	super((StringBuilder)null);
	initHeaders();
	}
	public callPicka(StringBuilder out) {
		super(out);
		initHeaders();
	}
	public callPicka(cn.bran.japid.template.JapidTemplateBaseWithoutPlay caller) {
		super(caller);
	}

/* based on https://github.com/branaway/Japid/issues/12
 */
	public static final String[] argNames = new String[] {/* args of the template*/ };
	public static final String[] argTypes = new String[] {/* arg types of the template*/ };
	public static final Object[] argDefaults= new Object[] { };
	public static java.lang.reflect.Method renderMethod = getRenderMethod(japidviews.templates.callPicka.class);

	{
		setRenderMethod(renderMethod);
		setArgNames(argNames);
		setArgTypes(argTypes);
		setArgDefaults(argDefaults);
		setSourceTemplate(sourceTemplate);
	}
////// end of named args stuff

	public cn.bran.japid.template.RenderResult render() {
		setStopwatchOn();
		try {super.layout();} catch (RuntimeException __e) { super.handleException(__e);} // line 0, japidviews/templates/callPicka.html
		return getRenderResult();
	}

	public static cn.bran.japid.template.RenderResult apply() {
		return new callPicka().render();
	}

	@Override protected void doLayout() {
		beginDoLayout(sourceTemplate);
;// line 1, callPicka.html
p("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">\n");// line 2, callPicka.html
		traceFile(); // must use traceFile() method to control where to insert the template file name// line 4, callPicka.html
		p("<p>beginning...<p>\n" + 
"<p>\n" + 
"<p>call a simple tag</p>\n" + 
"\n" + 
"Another simple tag aTag, which locates in the same directory as this template:\n" + 
"\n" + 
"first define something in a Java code block. \n" + 
"\n");// line 4, callPicka.html
		 List<String> strings = new ArrayList<String>(){{add("you tu");add("me");add("everyone");}};// line 13, callPicka.html

new aTag(callPicka.this).render(strings); // line 15, callPicka.html// line 15, callPicka.html
		p("\n" + 
"note: the picka tag is defined in the japidviews/_tags directory\n" + 
"\n");// line 15, callPicka.html
		new picka(callPicka.this).render(// line 19, callPicka.html
"a", "b" + "c", new picka.DoBody<String>(){ // line 19, callPicka.html
public void render(final String rr) { // line 19, callPicka.html
// line 19, callPicka.html
		p("    the tag chosed: ");// line 19, callPicka.html
		p(rr);// line 20, callPicka.html
		p("\n" + 
"    <p>and we can can call a tag recursively?</p>\n" + 
"    ");// line 20, callPicka.html
		new SampleTag(callPicka.this).render(rr); // line 22, callPicka.html// line 22, callPicka.html

}

StringBuilder oriBuffer;
@Override
public void setBuffer(StringBuilder sb) {
	oriBuffer = getOut();
	setOut(sb);
}

@Override
public void resetBuffer() {
	setOut(oriBuffer);
}

}
);// line 19, callPicka.html
		p("</p>\n" + 
"\n" + 
"<p>\n" + 
"we can call without the body part:\n" + 
"\n");// line 23, callPicka.html
		new picka(callPicka.this).render(named("a", "aa"), named("b", "bb")); // line 29, callPicka.html// line 29, callPicka.html
		p("\n" + 
"or \n");// line 29, callPicka.html
		new picka(callPicka.this).render("cc","dd"); // line 32, callPicka.html// line 32, callPicka.html
		p("\n" + 
"</p>\n" + 
"<p>\n" + 
"Or using the full path of the tag\n" + 
"</p>\n" + 
"\n");// line 32, callPicka.html
		new japidviews.templates.aTag(callPicka.this).render(strings); // line 39, callPicka.html// line 39, callPicka.html
		p("\n" + 
"<p>You can use \".\" instead of \"/\" on the path:</p>\n" + 
"\n");// line 39, callPicka.html
		new japidviews.templates.aTag(callPicka.this).render(strings); // line 43, callPicka.html// line 43, callPicka.html
		;// line 43, callPicka.html
		
		endDoLayout(sourceTemplate);
	}

}