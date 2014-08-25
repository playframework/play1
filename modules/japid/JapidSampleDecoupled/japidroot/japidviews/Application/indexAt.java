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
// NOTE: This file was generated from: japidviews/Application/indexAt.html
// Change to this file will be lost next time the template file is compiled.
//
@cn.bran.play.NoEnhance
public class indexAt extends cn.bran.play.JapidTemplateBase
{
	public static final String sourceTemplate = "japidviews/Application/indexAt.html";
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


	public indexAt() {
	super((StringBuilder)null);
	initHeaders();
	}
	public indexAt(StringBuilder out) {
		super(out);
		initHeaders();
	}
	public indexAt(cn.bran.japid.template.JapidTemplateBaseWithoutPlay caller) {
		super(caller);
	}

/* based on https://github.com/branaway/Japid/issues/12
 */
	public static final String[] argNames = new String[] {/* args of the template*/ };
	public static final String[] argTypes = new String[] {/* arg types of the template*/ };
	public static final Object[] argDefaults= new Object[] { };
	public static java.lang.reflect.Method renderMethod = getRenderMethod(japidviews.Application.indexAt.class);

	{
		setRenderMethod(renderMethod);
		setArgNames(argNames);
		setArgTypes(argTypes);
		setArgDefaults(argDefaults);
		setSourceTemplate(sourceTemplate);
	}
////// end of named args stuff

	public cn.bran.japid.template.RenderResult render() {
		try {super.layout();} catch (RuntimeException __e) { super.handleException(__e);} // line 0, japidviews/Application/indexAt.html
		return getRenderResult();
	}

	public static cn.bran.japid.template.RenderResult apply() {
		return new indexAt().render();
	}

	@Override protected void doLayout() {
		beginDoLayout(sourceTemplate);
p("<h2>Some Samples that demonstrate Japid features.</h2>\n" + 
"\n" + 
"<p>Please follow the controller actions and render paths for the\n" + 
"source code.</p>\n" + 
"\n" + 
"<ul>\n" + 
"	<li><a href=\"");// line 1, indexAt.html
		p(lookup("hello", new Object[]{}));// line 7, indexAt.html
		p("\">Hello Japid, using an overridden\n" + 
"	version of renderText()</a></li>\n" + 
"	<li><a href=\"/application/callTag\">using tags in a template</a></li>\n" + 
"	<li><a href=\"/renderJapidWith/templates/def.html\"><em>def</em>\n" + 
"	tag: define a method that return a string that can be invoked from\n" + 
"	super template. Compare this to the <b>set</b> tag</a></li>\n");// line 7, indexAt.html
		p("\n" + 
"	<li><a href=\"/more.Portlets/index\">demo how to composite a\n" + 
"	page with independent segments with the <b>invoke</b> tag</a>\n" + 
"	</li>\n" + 
"\n" + 
"	<li><a href=\"/application/renderByPosition\">implicit template\n" + 
"	binding with <b>JapidController.renderJapid()</b> </a></li>\n" + 
"	<li><a href=\"/application/postList\">a template with template\n" + 
"	and tags etc...</a></li>\n" + 
"	<li><a href=\"/renderJapidWith/templates/log.html\">use the log\n" + 
"	macro in a template. watch the output in the console</a></li>\n" + 
"	<li><a href=\"/renderJapidWith/templates/Msg.html\">i18n, use\n" + 
"	predefined messages</a></li>\n" + 
"	<li><a href=\"/application/postlist\">looping and tag calling</a></li>\n" + 
"	<li><a href=\"/application/each\">the \"open for\" loop and the now deprecated \"*Each* tag</a></li>\n" + 
"	<li><a href=\"/application/ifs2\">open if statement: minimalistic if-else statement can can take any expression rather than boolean values as the condition.</a>\n" + 
"	   <p>example: </p>\n" + 
"	   <pre>\n" + 
"		    @if expr\n" + 
"		        xxx\n" + 
"		    @else if expr\n" + 
"		        yyy\n" + 
"		    @else\n" + 
"		        zzz\n" + 
"		    @\n" + 
"	   </pre>\n" + 
"	</li>\n" + 
"	<li><a href=\"/application/invokeInLoop\">using invoke tag with\n" + 
"	local variables requires final attribute</a></li>\n" + 
"	<li><a href=\"/renderJapidWith/templates/suppressNull.html\">suppressNull,\n" + 
"	a directive to allow safe navigation in expression, default off</a></li>\n" + 
"	<li><a href=\"/application/dumpPost\">show how to dump a post\n" + 
"	detail with japid.dump.request spec in the application.conf. see\n" + 
"	console for output.</a></li>\n" + 
"	<li><a href=\"/application/cacheWithRenderJapid?a=hi\">cache with\n" + 
"	renderJapid</a></li>\n" + 
"	<li><a href=\"/application/testCacheFor\">using the CacheFor\n" + 
"	annotation</a></li>\n" + 
"	<li><a href=\"/application/in\">action forwarding with\n" + 
"	dontRedirect() from JapidController</a></li>\n" + 
"	<li><a href=\"/renderJapidWith/templates/openBrace.html\"> use { in if and while </a></li>\n" + 
"	<li><a href=\"/application/escapedExpr\"> ");// line 16, indexAt.html
		p("\n" + 
"	raw expression with ${} and html-safe expression with ~{}\n" + 
"	");// line 57, indexAt.html
		p("	</li>\n" + 
"	<li><a href=\"/more.ContentNegotiation/index\"> content\n" + 
"	negotiation.</a> Use tools like CURL to test it: <pre>curl -i -H \"Accept: application/json\" http://127.0.0.1:9000/more.ContentNegotiation/index</pre>\n" + 
"	<p>Content negotiation works with renderJapid(), which does\n" + 
"	implicit parameter binding and template picking.</p>\n" + 
"	<p>For more doc: see <a\n" + 
"		href=\"http://www.playframework.org/documentation/1.1/routes#content-negotiation\">Play\n" + 
"	Content Negotiation</a></p>\n" + 
"	</li>\n" + 
"	<li><a href=\"/more.MyController/quickview\"> Use relative path\n" + 
"	in layout spec and tags</a>: prefix the layout name or the tag name with a\n" + 
"	dot \".\" to let the compiler prefix the path with the current package.\n" + 
"	This saves using the full and long class qualifications.</li>\n" + 
"\n" + 
"    ");// line 59, indexAt.html
		String na = "bran";// line 74, indexAt.html
    int ag = 123;// line 75, indexAt.html
		p("	\n" + 
"	<li><a href=\"");// line 75, indexAt.html
		p(lookup("validate", na, ag));// line 77, indexAt.html
		p("\">validation and errors</a></li>\n" + 
"    <li> using the <em>flash</em> object\n" + 
"		<ul>\n" + 
"			<li><a href=\"/application/flashgood\">flash with success</a></li>\n" + 
"			<li><a href=\"/application/flashbad\">flash with errors</a></li>\n" + 
"			<li><a href=\"/application/flashmsg\">flash with a message</a></li>\n" + 
"			<li><a href=\"");// line 77, indexAt.html
		p(lookup("reverseUrl", new Object[]{}));// line 83, indexAt.html
		p("\">flash with a message</a></li>\n" + 
"		</ul>\n" + 
"    </li>\n" + 
"    <li>\n" + 
"	   ");// line 83, indexAt.html
		 SearchParams sp = new  SearchParams("key1, key2", "AND");// line 87, indexAt.html
		p("	   <a href=\"");// line 87, indexAt.html
		p(lookup("search", sp));// line 88, indexAt.html
		p("\">reverse URL lookup with complex object</a>\n" + 
"	</li>\n" + 
"    ");// line 88, indexAt.html
		p("\n" + 
"</ul>\n" + 
"\n");// line 91, indexAt.html
		
		endDoLayout(sourceTemplate);
	}

}