//version: 0.9.5
package japidviews.Application;
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
// NOTE: This file was generated from: japidviews/Application/authorPanel2.html
// Change to this file will be lost next time the template file is compiled.
//
@cn.bran.play.NoEnhance
public class authorPanel2 extends cn.bran.play.JapidTemplateBase
{
	public static final String sourceTemplate = "japidviews/Application/authorPanel2.html";
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


	public authorPanel2() {
	super((StringBuilder)null);
	initHeaders();
	}
	public authorPanel2(StringBuilder out) {
		super(out);
		initHeaders();
	}
	public authorPanel2(cn.bran.japid.template.JapidTemplateBaseWithoutPlay caller) {
		super(caller);
	}

/* based on https://github.com/branaway/Japid/issues/12
 */
	public static final String[] argNames = new String[] {/* args of the template*/"a",  };
	public static final String[] argTypes = new String[] {/* arg types of the template*/"models.japidsample.Author",  };
	public static final Object[] argDefaults= new Object[] {null, };
	public static java.lang.reflect.Method renderMethod = getRenderMethod(japidviews.Application.authorPanel2.class);

	{
		setRenderMethod(renderMethod);
		setArgNames(argNames);
		setArgTypes(argTypes);
		setArgDefaults(argDefaults);
		setSourceTemplate(sourceTemplate);
	}
////// end of named args stuff

	private models.japidsample.Author a; // line 1, japidviews/Application/authorPanel2.html
	public cn.bran.japid.template.RenderResult render(models.japidsample.Author a) {
		this.a = a;
		try {super.layout();} catch (RuntimeException __e) { super.handleException(__e);} // line 1, japidviews/Application/authorPanel2.html
		return getRenderResult();
	}

	public static cn.bran.japid.template.RenderResult apply(models.japidsample.Author a) {
		return new authorPanel2().render(a);
	}

	@Override protected void doLayout() {
		beginDoLayout(sourceTemplate);
;// line 1, authorPanel2.html
		p("\n" + 
"<p>decorated name: ");// line 1, authorPanel2.html
				actionRunners.put(getOut().length(), new cn.bran.play.CacheablePlayActionRunner("", Application.class, "decorateName", a.name) {
			@Override
			public void runPlayAction() throws cn.bran.play.JapidResult {
				Application.decorateName(a.name); // line 3, authorPanel2.html
			}
		}); p("\n");// line 3, authorPanel2.html
		p("</p>\n");// line 3, authorPanel2.html
		
		endDoLayout(sourceTemplate);
	}

}