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
// NOTE: This file was generated from: japidviews/templates/def.html
// Change to this file will be lost next time the template file is compiled.
//
@cn.bran.play.NoEnhance
public class def extends defLayout
{
	public static final String sourceTemplate = "japidviews/templates/def.html";
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


	public def() {
	super((StringBuilder)null);
	initHeaders();
	}
	public def(StringBuilder out) {
		super(out);
		initHeaders();
	}
	public def(cn.bran.japid.template.JapidTemplateBaseWithoutPlay caller) {
		super(caller);
	}

/* based on https://github.com/branaway/Japid/issues/12
 */
	public static final String[] argNames = new String[] {/* args of the template*/ };
	public static final String[] argTypes = new String[] {/* arg types of the template*/ };
	public static final Object[] argDefaults= new Object[] { };
	public static java.lang.reflect.Method renderMethod = getRenderMethod(japidviews.templates.def.class);

	{
		setRenderMethod(renderMethod);
		setArgNames(argNames);
		setArgTypes(argTypes);
		setArgDefaults(argDefaults);
		setSourceTemplate(sourceTemplate);
	}
////// end of named args stuff

	public cn.bran.japid.template.RenderResult render() {
		try {super.layout();} catch (RuntimeException __e) { super.handleException(__e);} // line 0, japidviews/templates/def.html
		return getRenderResult();
	}

	public static cn.bran.japid.template.RenderResult apply() {
		return new def().render();
	}

	@Override protected void doLayout() {
		beginDoLayout(sourceTemplate);
;// line 1, def.html
		p("\n" + 
"<p>check 1</p>\n" + 
"\n");// line 1, def.html
		p("\n");// line 5, def.html
		// line 6, def.html
		p("\n" + 
"<p>check 2</p>\n");// line 11, def.html
		// line 14, def.html
		p("<p>check 3</p>\n" + 
"\n");// line 17, def.html
		p("\n" + 
"\n");// line 20, def.html
		// line 22, def.html
		p("\n" + 
"\n" + 
"<p>check 4</p>\n");// line 25, def.html
		new dummyTag(def.this).render(get("bar")); // line 28, def.html// line 28, def.html

p("<p>check 5</p>\n" + 
"\n");// line 30, def.html
		p(foo());// line 33, def.html
		p("\n" + 
"\n" + 
"<p>check 6</p>\n" + 
"\n");// line 33, def.html
		//japid compiler: artificial line to avoid being treated as a terminating line// line 37, def.html
		    String a = "";// line 38, def.html
		    a += "bssdfsdf";// line 39, def.html
		p("\n");// line 40, def.html
		if (a.length() > 0) {// line 41, def.html
      p(a);// line 42, def.html
    }// line 43, def.html
		p("\n");// line 43, def.html
		
		endDoLayout(sourceTemplate);
	}

public String foo2(String p) {
StringBuilder sb = new StringBuilder();
StringBuilder ori = getOut();
this.setOut(sb);
TreeMap<Integer, cn.bran.japid.template.ActionRunner> parentActionRunners = actionRunners;
actionRunners = new TreeMap<Integer, cn.bran.japid.template.ActionRunner>();
// line 6, def.html
		p("	<p>hi ");// line 6, def.html
		p(p);// line 7, def.html
		p("!,</p> \n" + 
"	<p>from ");// line 7, def.html
		p(request.action);// line 8, def.html
		p("</p>\n" + 
"	<p>OK you can call a tag:</p>\n" + 
"	");// line 8, def.html
		new dummyTag(def.this).render(p); // line 10, def.html// line 10, def.html

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
public String foo() {
StringBuilder sb = new StringBuilder();
StringBuilder ori = getOut();
this.setOut(sb);
TreeMap<Integer, cn.bran.japid.template.ActionRunner> parentActionRunners = actionRunners;
actionRunners = new TreeMap<Integer, cn.bran.japid.template.ActionRunner>();
// line 14, def.html
	String s = "hi there";// line 15, def.html
		p("	<p>foo hello ");// line 15, def.html
		p(foo2(s));// line 16, def.html
		p("</p>\n");// line 16, def.html
		
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
public String bar() {
StringBuilder sb = new StringBuilder();
StringBuilder ori = getOut();
this.setOut(sb);
TreeMap<Integer, cn.bran.japid.template.ActionRunner> parentActionRunners = actionRunners;
actionRunners = new TreeMap<Integer, cn.bran.japid.template.ActionRunner>();
// line 22, def.html
		p("\n" + 
"	");// line 22, def.html
		String s = "hi2";// line 23, def.html
		p("	<p>bar hi ");// line 23, def.html
		p(s);// line 24, def.html
		p("!</p>\n");// line 24, def.html
		
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