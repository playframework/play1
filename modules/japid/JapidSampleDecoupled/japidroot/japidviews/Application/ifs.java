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
// NOTE: This file was generated from: japidviews/Application/ifs.html
// Change to this file will be lost next time the template file is compiled.
//
@cn.bran.play.NoEnhance
public class ifs extends cn.bran.play.JapidTemplateBase
{
	public static final String sourceTemplate = "japidviews/Application/ifs.html";
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


	public ifs() {
	super((StringBuilder)null);
	initHeaders();
	}
	public ifs(StringBuilder out) {
		super(out);
		initHeaders();
	}
	public ifs(cn.bran.japid.template.JapidTemplateBaseWithoutPlay caller) {
		super(caller);
	}

/* based on https://github.com/branaway/Japid/issues/12
 */
	public static final String[] argNames = new String[] {/* args of the template*/"str", "col", "b", "a1", "a2", "i", "s2",  };
	public static final String[] argTypes = new String[] {/* arg types of the template*/"String", "Collection", "boolean", "Object[]", "int[]", "int", "String",  };
	public static final Object[] argDefaults= new Object[] {null,null,null,null,null,null,null, };
	public static java.lang.reflect.Method renderMethod = getRenderMethod(japidviews.Application.ifs.class);

	{
		setRenderMethod(renderMethod);
		setArgNames(argNames);
		setArgTypes(argTypes);
		setArgDefaults(argDefaults);
		setSourceTemplate(sourceTemplate);
	}
////// end of named args stuff

	private String str; // line 1, japidviews/Application/ifs.html
	private Collection col; // line 1, japidviews/Application/ifs.html
	private boolean b; // line 1, japidviews/Application/ifs.html
	private Object[] a1; // line 1, japidviews/Application/ifs.html
	private int[] a2; // line 1, japidviews/Application/ifs.html
	private int i; // line 1, japidviews/Application/ifs.html
	private String s2; // line 1, japidviews/Application/ifs.html
	public cn.bran.japid.template.RenderResult render(String str,Collection col,boolean b,Object[] a1,int[] a2,int i,String s2) {
		this.str = str;
		this.col = col;
		this.b = b;
		this.a1 = a1;
		this.a2 = a2;
		this.i = i;
		this.s2 = s2;
		try {super.layout();} catch (RuntimeException __e) { super.handleException(__e);} // line 1, japidviews/Application/ifs.html
		return getRenderResult();
	}

	public static cn.bran.japid.template.RenderResult apply(String str,Collection col,boolean b,Object[] a1,int[] a2,int i,String s2) {
		return new ifs().render(str, col, b, a1, a2, i, s2);
	}

	@Override protected void doLayout() {
		beginDoLayout(sourceTemplate);
;// line 1, ifs.html
		p("\n" + 
"<p>\n" + 
"<pre>\n");// line 1, ifs.html
		p("\n" + 
"    `if expr {\n" + 
"        xxx\n" + 
"    `} else if expr {\n" + 
"        yyy\n" + 
"    `} else {\n" + 
"        zzz\n" + 
"    `}\n");// line 5, ifs.html
		p("</pre>\n" + 
"</p>\n" + 
"<p>\n" + 
"    is equals to\n" + 
"</p>\n" + 
"<p>\n" + 
"<pre>\n");// line 13, ifs.html
		p("\n" + 
"    `if(cn.bran.play.WebUtils.asBoolean(expr)){\n" + 
"        xxx\n" + 
"    `} else if(cn.bran.play.WebUtils.asBoolean(expr)){\n" + 
"        yyy\n" + 
"    `} else {\n" + 
"        zzz\n" + 
"    `}\n");// line 21, ifs.html
		p("<pre>\n" + 
"\n" + 
"<p/>\n");// line 29, ifs.html
		if(asBoolean(str)) {// line 33, ifs.html
		p("    Got ");// line 33, ifs.html
		p(str);// line 34, ifs.html
		p("\n");// line 34, ifs.html
		} else if(asBoolean(str )) {// line 35, ifs.html
		p("    finally got ");// line 35, ifs.html
		p(str);// line 36, ifs.html
		p("\n");// line 36, ifs.html
		} else {// line 37, ifs.html
		p("    str is empty\n");// line 37, ifs.html
		}// line 39, ifs.html
		p("<p/>\n" + 
"\n");// line 39, ifs.html
		if(asBoolean(col)) {// line 42, ifs.html
		p("    Got data from col: ");// line 42, ifs.html
		p(col);// line 43, ifs.html
		p("\n");// line 43, ifs.html
		} else {// line 44, ifs.html
		p("    col is empty\n");// line 44, ifs.html
		}// line 46, ifs.html
		p("\n" + 
"<p/>\n");// line 46, ifs.html
		if(asBoolean(b)) {// line 49, ifs.html
		p("    right\n");// line 49, ifs.html
		} else {// line 51, ifs.html
		p("    wrong\n");// line 51, ifs.html
		}// line 53, ifs.html
		p("\n" + 
"<p/>\n");// line 53, ifs.html
		if(asBoolean(a1)) {// line 56, ifs.html
		p("    got a1: ");// line 56, ifs.html
		p(a1);// line 57, ifs.html
		p("\n");// line 57, ifs.html
		} else {// line 58, ifs.html
		p("    a1 is empty\n");// line 58, ifs.html
		}// line 60, ifs.html
		p("<p/>\n");// line 60, ifs.html
		if(asBoolean(a2)) {// line 62, ifs.html
		p("    got a2: ");// line 62, ifs.html
		p(a2);// line 63, ifs.html
		p("\n");// line 63, ifs.html
		} else {// line 64, ifs.html
		p("    a2 is empty\n");// line 64, ifs.html
		}// line 66, ifs.html
		p("<p/>\n");// line 66, ifs.html
		if(asBoolean(i)) {// line 68, ifs.html
		p("    got i: ");// line 68, ifs.html
		p(i);// line 69, ifs.html
		p("\n");// line 69, ifs.html
		} else {// line 70, ifs.html
		p("    i == 0\n");// line 70, ifs.html
		}// line 72, ifs.html
		p("<p/>\n");// line 72, ifs.html
		if(asBoolean(s2)) {// line 74, ifs.html
		p("    got s2: ");// line 74, ifs.html
		p(s2);// line 75, ifs.html
		p("\n");// line 75, ifs.html
		} else {// line 76, ifs.html
		p("    s2 is empty\n");// line 76, ifs.html
		}// line 78, ifs.html
		p("\n" + 
"<p>try the negation</p>\n");// line 78, ifs.html
		String ss = str;// line 81, ifs.html
if(!asBoolean(ss)) {// line 82, ifs.html
		p("    ss is empty\n");// line 82, ifs.html
		} else if(!asBoolean(ss)) {// line 84, ifs.html
		p("    again...\n");// line 84, ifs.html
		} else {// line 86, ifs.html
		p("    ss has something\n");// line 86, ifs.html
		}// line 88, ifs.html
		p("\n" + 
"\n" + 
"\n" + 
"\n");// line 88, ifs.html
		
		endDoLayout(sourceTemplate);
	}

}