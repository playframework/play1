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
// NOTE: This file was generated from: japidviews/Application/verbatim.html
// Change to this file will be lost next time the template file is compiled.
//
@cn.bran.play.NoEnhance
public class verbatim extends cn.bran.play.JapidTemplateBase
{
	public static final String sourceTemplate = "japidviews/Application/verbatim.html";
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


	public verbatim() {
	super((StringBuilder)null);
	initHeaders();
	}
	public verbatim(StringBuilder out) {
		super(out);
		initHeaders();
	}
	public verbatim(cn.bran.japid.template.JapidTemplateBaseWithoutPlay caller) {
		super(caller);
	}

/* based on https://github.com/branaway/Japid/issues/12
 */
	public static final String[] argNames = new String[] {/* args of the template*/ };
	public static final String[] argTypes = new String[] {/* arg types of the template*/ };
	public static final Object[] argDefaults= new Object[] { };
	public static java.lang.reflect.Method renderMethod = getRenderMethod(japidviews.Application.verbatim.class);

	{
		setRenderMethod(renderMethod);
		setArgNames(argNames);
		setArgTypes(argTypes);
		setArgDefaults(argDefaults);
		setSourceTemplate(sourceTemplate);
	}
////// end of named args stuff

	public cn.bran.japid.template.RenderResult render() {
		try {super.layout();} catch (RuntimeException __e) { super.handleException(__e);} // line 0, japidviews/Application/verbatim.html
		return getRenderResult();
	}

	public static cn.bran.japid.template.RenderResult apply() {
		return new verbatim().render();
	}

	@Override protected void doLayout() {
		beginDoLayout(sourceTemplate);
p("\n" + 
"<p>\n" + 
"you should be able to see all Japid command un-interpreted.    	\n" + 
"</p>\n" + 
"\n");// line 1, verbatim.html
		p("\n" + 
"\n" + 
"	`args models.japidsample.Author a\n" + 
"	\n" + 
"	<p>author name: $a.name</p>\n" + 
"	<p>his birthdate: $a.birthDate</p>\n" + 
"	<p>and his is a '${a.getGender()}'</p>\n" + 
"	    `tag SampleTag \"end\"\n" + 
"    \n");// line 6, verbatim.html
		p("\n" + 
"<p>got it?</p>\n" + 
"\n");// line 15, verbatim.html
		final String[] ss = new String[]{"a", "b"};// line 18, verbatim.html
new Runnable() {public void run() {
int _size = -100; int _index = 0; boolean _isOdd = false; String _parity = ""; boolean _isFirst = true; Boolean _isLast = _index == _size;
for (String s : ss) { // line 19, verbatim.html
	_index++; _isOdd = !_isOdd; _parity = _isOdd? "odd" : "even"; _isFirst = _index == 1; if (_size == -100) _size = getCollectionSize(ss); _isLast = (_size < 0 ? null : _index == _size);
// line 19, verbatim.html
		p("    <p>loop: ");// line 19, verbatim.html
		p(s);// line 20, verbatim.html
		p("</p>\n" + 
"    ");// line 20, verbatim.html
		p("\n" + 
"    <p>please use ` to start command and $s to get the value</p>\n" + 
"    ");// line 21, verbatim.html
		;// line 23, verbatim.html
		
}
}}.run();
// line 19, verbatim.html
		
		endDoLayout(sourceTemplate);
	}

}