//version: 0.9.5
package japidviews.more.MyController;
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
// NOTE: This file was generated from: japidviews/more/MyController/subview.html
// Change to this file will be lost next time the template file is compiled.
//
@cn.bran.play.NoEnhance
public class subview extends superview
{
	public static final String sourceTemplate = "japidviews/more/MyController/subview.html";
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


	public subview() {
	super((StringBuilder)null);
	initHeaders();
	}
	public subview(StringBuilder out) {
		super(out);
		initHeaders();
	}
	public subview(cn.bran.japid.template.JapidTemplateBaseWithoutPlay caller) {
		super(caller);
	}

/* based on https://github.com/branaway/Japid/issues/12
 */
	public static final String[] argNames = new String[] {/* args of the template*/"s",  };
	public static final String[] argTypes = new String[] {/* arg types of the template*/"String",  };
	public static final Object[] argDefaults= new Object[] {null, };
	public static java.lang.reflect.Method renderMethod = getRenderMethod(japidviews.more.MyController.subview.class);

	{
		setRenderMethod(renderMethod);
		setArgNames(argNames);
		setArgTypes(argTypes);
		setArgDefaults(argDefaults);
		setSourceTemplate(sourceTemplate);
	}
////// end of named args stuff

	private String s; // line 2, japidviews/more/MyController/subview.html
	public cn.bran.japid.template.RenderResult render(String s) {
		this.s = s;
		try {super.layout();} catch (RuntimeException __e) { super.handleException(__e);} // line 2, japidviews/more/MyController/subview.html
		return getRenderResult();
	}

	public static cn.bran.japid.template.RenderResult apply(String s) {
		return new subview().render(s);
	}

	@Override protected void doLayout() {
		beginDoLayout(sourceTemplate);
;// line 1, subview.html
;// line 2, subview.html
		// line 4, subview.html
		;// line 4, subview.html
		// line 5, subview.html
		p("\n" + 
"\n" + 
"hello ");// line 5, subview.html
		p(s);// line 8, subview.html
		p("\n");// line 8, subview.html
		new japidviews.more.MyController._tags.taggy(subview.this).render(s); // line 10, subview.html// line 10, subview.html
		p(" ");// line 10, subview.html
		
		endDoLayout(sourceTemplate);
	}

	@Override protected void side() {
		p("my side bar");;
	}
	@Override protected void title() {
		p("my title from subview");;
	}
}