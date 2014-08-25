//version: 0.9.5.2
package japidviews.etc.MyController;
import java.util.*;
import java.io.*;
import cn.bran.japid.tags.Each;
import static cn.bran.play.JapidPlayAdapter.*;
import static play.data.validation.Validation.*;
import static play.templates.JavaExtensions.*;
import play.data.validation.Error;
import play.i18n.Messages;
import play.mvc.Scope.*;
import play.data.validation.Validation;
import play.i18n.Lang;
import controllers.*;
import static japidviews._javatags.JapidWebUtil.*;
import japidviews._layouts.*;
import models.*;
import play.mvc.Http.*;
//
// NOTE: This file was generated from: japidviews/etc/MyController/list.html
// Change to this file will be lost next time the template file is compiled.
//
@cn.bran.play.NoEnhance
public class list extends cn.bran.play.JapidTemplateBase
{
	public static final String sourceTemplate = "japidviews/etc/MyController/list.html";
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


	public list() {
	super((StringBuilder)null);
	initHeaders();
	}
	public list(StringBuilder out) {
		super(out);
		initHeaders();
	}
	public list(cn.bran.japid.template.JapidTemplateBaseWithoutPlay caller) {
		super(caller);
	}

/* based on https://github.com/branaway/Japid/issues/12
 */
	public static final String[] argNames = new String[] {/* args of the template*/"s", "i",  };
	public static final String[] argTypes = new String[] {/* arg types of the template*/"String", "int",  };
	public static final Object[] argDefaults= new Object[] {null,null, };
	public static java.lang.reflect.Method renderMethod = getRenderMethod(japidviews.etc.MyController.list.class);

	{
		setRenderMethod(renderMethod);
		setArgNames(argNames);
		setArgTypes(argTypes);
		setArgDefaults(argDefaults);
		setSourceTemplate(sourceTemplate);
	}
////// end of named args stuff

	private String s; // line 1, japidviews/etc/MyController/list.html
	private int i; // line 1, japidviews/etc/MyController/list.html
	public cn.bran.japid.template.RenderResult render(String s,int i) {
		this.s = s;
		this.i = i;
		try {super.layout();} catch (RuntimeException __e) { super.handleException(__e);} // line 1, japidviews/etc/MyController/list.html
		return getRenderResult();
	}

	public static cn.bran.japid.template.RenderResult apply(String s,int i) {
		return new list().render(s, i);
	}

	@Override protected void doLayout() {
		beginDoLayout(sourceTemplate);
;// line 1, list.html
		p("hello... ");// line 1, list.html
		p(s);// line 2, list.html
		p(", ");// line 2, list.html
		p(i);// line 2, list.html
		p("\n");// line 2, list.html
		
		endDoLayout(sourceTemplate);
	}

}