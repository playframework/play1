//version: 0.9.5
package japidviews.DummyController;
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
// NOTE: This file was generated from: japidviews/DummyController/content.xml
// Change to this file will be lost next time the template file is compiled.
//
@cn.bran.play.NoEnhance
public class content_xml extends cn.bran.play.JapidTemplateBase
{
	public static final String sourceTemplate = "japidviews/DummyController/content.xml";
	 private void initHeaders() {
		putHeader("Content-Type", "text/xml; charset=utf-8");
		setContentType("text/xml; charset=utf-8");
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


	public content_xml() {
	super((StringBuilder)null);
	initHeaders();
	}
	public content_xml(StringBuilder out) {
		super(out);
		initHeaders();
	}
	public content_xml(cn.bran.japid.template.JapidTemplateBaseWithoutPlay caller) {
		super(caller);
	}

/* based on https://github.com/branaway/Japid/issues/12
 */
	public static final String[] argNames = new String[] {/* args of the template*/ };
	public static final String[] argTypes = new String[] {/* arg types of the template*/ };
	public static final Object[] argDefaults= new Object[] { };
	public static java.lang.reflect.Method renderMethod = getRenderMethod(japidviews.DummyController.content_xml.class);

	{
		setRenderMethod(renderMethod);
		setArgNames(argNames);
		setArgTypes(argTypes);
		setArgDefaults(argDefaults);
		setSourceTemplate(sourceTemplate);
	}
////// end of named args stuff

	public cn.bran.japid.template.RenderResult render() {
		try {super.layout();} catch (RuntimeException __e) { super.handleException(__e);} // line 0, japidviews/DummyController/content.xml
		return getRenderResult();
	}

	public static cn.bran.japid.template.RenderResult apply() {
		return new content_xml().render();
	}

	@Override protected void doLayout() {
		beginDoLayout(sourceTemplate);
;// line 1, content.xml
		p("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + 
"<a>this is xml content: ");// line 1, content.xml
		p(foo("cool"));// line 3, content.xml
		p("</a>\n" + 
"\n");// line 3, content.xml
		// line 5, content.xml
		
		endDoLayout(sourceTemplate);
	}

public String foo(String a) {
StringBuilder sb = new StringBuilder();
StringBuilder ori = getOut();
this.setOut(sb);
TreeMap<Integer, cn.bran.japid.template.ActionRunner> parentActionRunners = actionRunners;
actionRunners = new TreeMap<Integer, cn.bran.japid.template.ActionRunner>();
// line 5, content.xml
		p("goo stuff, ");// line 5, content.xml
		p(a);// line 6, content.xml
		p("");// line 6, content.xml
		
this.setOut(ori);
if (actionRunners.size() > 0) {
	StringBuilder _sb2 = new StringBuilder();
	int segStart = 0;
	for (Map.Entry<Integer, cn.bran.japid.template.ActionRunner> _arEntry : actionRunners.entrySet()) {
		int pos = _arEntry.getKey();
		_sb2.append(sb.substring(segStart, pos));
		segStart = pos;
		cn.bran.japid.template.ActionRunner _a_ = _arEntry.getValue();
		_sb2.append(_a_.run().getContent().toString());
	}
	_sb2.append(sb.substring(segStart));
	actionRunners = parentActionRunners;
	return _sb2.toString();
} else {
	actionRunners = parentActionRunners;
	return sb.toString();
}
}
}