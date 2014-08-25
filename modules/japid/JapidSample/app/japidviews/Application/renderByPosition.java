//version: 0.9.5
package japidviews.Application;
import java.util.*;
import java.io.*;
import cn.bran.japid.tags.Each;
import models.japidsample.*;
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
// NOTE: This file was generated from: japidviews/Application/renderByPosition.html
// Change to this file will be lost next time the template file is compiled.
//
@cn.bran.play.NoEnhance
public class renderByPosition extends cn.bran.play.JapidTemplateBase
{
	public static final String sourceTemplate = "japidviews/Application/renderByPosition.html";
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


	public renderByPosition() {
	super((StringBuilder)null);
	initHeaders();
	}
	public renderByPosition(StringBuilder out) {
		super(out);
		initHeaders();
	}
	public renderByPosition(cn.bran.japid.template.JapidTemplateBaseWithoutPlay caller) {
		super(caller);
	}

/* based on https://github.com/branaway/Japid/issues/12
 */
	public static final String[] argNames = new String[] {/* args of the template*/"ss", "ii", "au1", "au2", "au22",  };
	public static final String[] argTypes = new String[] {/* arg types of the template*/"String", "int", "Author", "Author", "Author2",  };
	public static final Object[] argDefaults= new Object[] {null,null,null,null,null, };
	public static java.lang.reflect.Method renderMethod = getRenderMethod(japidviews.Application.renderByPosition.class);

	{
		setRenderMethod(renderMethod);
		setArgNames(argNames);
		setArgTypes(argTypes);
		setArgDefaults(argDefaults);
		setSourceTemplate(sourceTemplate);
	}
////// end of named args stuff

	private String ss; // line 2, japidviews/Application/renderByPosition.html
	private int ii; // line 2, japidviews/Application/renderByPosition.html
	private Author au1; // line 2, japidviews/Application/renderByPosition.html
	private Author au2; // line 2, japidviews/Application/renderByPosition.html
	private Author2 au22; // line 2, japidviews/Application/renderByPosition.html
	public cn.bran.japid.template.RenderResult render(String ss,int ii,Author au1,Author au2,Author2 au22) {
		this.ss = ss;
		this.ii = ii;
		this.au1 = au1;
		this.au2 = au2;
		this.au22 = au22;
		try {super.layout();} catch (RuntimeException __e) { super.handleException(__e);} // line 2, japidviews/Application/renderByPosition.html
		return getRenderResult();
	}

	public static cn.bran.japid.template.RenderResult apply(String ss,int ii,Author au1,Author au2,Author2 au22) {
		return new renderByPosition().render(ss, ii, au1, au2, au22);
	}

	@Override protected void doLayout() {
		beginDoLayout(sourceTemplate);
;// line 1, renderByPosition.html
		;// line 1, renderByPosition.html
		p("got: ");// line 3, renderByPosition.html
		p(ss);// line 4, renderByPosition.html
		p("\n" + 
"got: ");// line 4, renderByPosition.html
		p(ii);// line 5, renderByPosition.html
		p("\n" + 
"got: ");// line 5, renderByPosition.html
		p(au1.name);// line 6, renderByPosition.html
		p(", ");// line 6, renderByPosition.html
		p(au2.name);// line 6, renderByPosition.html
		p(", ");// line 6, renderByPosition.html
		p(au22.who);// line 6, renderByPosition.html
		p("\n" + 
"\n" + 
"<p>Lets call a tag by name:</p>\n" + 
"\n");// line 6, renderByPosition.html
		new tagPrimitives(renderByPosition.this).render(named("s", "hello"), named("b", true), named("f", 1.2f), named("d", 3.6)); // line 10, renderByPosition.html// line 10, renderByPosition.html
		
		endDoLayout(sourceTemplate);
	}

}