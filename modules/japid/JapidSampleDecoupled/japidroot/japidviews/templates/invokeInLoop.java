//version: 0.9.5
package japidviews.templates;
import java.util.*;
import java.io.*;
import cn.bran.japid.tags.Each;
import cn.bran.japid.template.ActionRunner;
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
// NOTE: This file was generated from: japidviews/templates/invokeInLoop.html
// Change to this file will be lost next time the template file is compiled.
//
@cn.bran.play.NoEnhance
public class invokeInLoop extends cn.bran.play.JapidTemplateBase
{
	public static final String sourceTemplate = "japidviews/templates/invokeInLoop.html";
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


	public invokeInLoop() {
	super((StringBuilder)null);
	initHeaders();
	}
	public invokeInLoop(StringBuilder out) {
		super(out);
		initHeaders();
	}
	public invokeInLoop(cn.bran.japid.template.JapidTemplateBaseWithoutPlay caller) {
		super(caller);
	}

/* based on https://github.com/branaway/Japid/issues/12
 */
	public static final String[] argNames = new String[] {/* args of the template*/"posts",  };
	public static final String[] argTypes = new String[] {/* arg types of the template*/"List<Post>",  };
	public static final Object[] argDefaults= new Object[] {null, };
	public static java.lang.reflect.Method renderMethod = getRenderMethod(japidviews.templates.invokeInLoop.class);

	{
		setRenderMethod(renderMethod);
		setArgNames(argNames);
		setArgTypes(argTypes);
		setArgDefaults(argDefaults);
		setSourceTemplate(sourceTemplate);
	}
////// end of named args stuff

	private List<Post> posts; // line 2, japidviews/templates/invokeInLoop.html
	public cn.bran.japid.template.RenderResult render(List<Post> posts) {
		this.posts = posts;
		try {super.layout();} catch (RuntimeException __e) { super.handleException(__e);} // line 2, japidviews/templates/invokeInLoop.html
		return getRenderResult();
	}

	public static cn.bran.japid.template.RenderResult apply(List<Post> posts) {
		return new invokeInLoop().render(posts);
	}

	@Override protected void doLayout() {
		beginDoLayout(sourceTemplate);
;// line 1, invokeInLoop.html
		;// line 1, invokeInLoop.html
		;// line 3, invokeInLoop.html
		p("\n" + 
"<p>	Since JDK 8 this is not necessary!!\n" + 
"</p>\n");// line 8, invokeInLoop.html
		for (int i = 0; i < 3;i++) {// line 11, invokeInLoop.html
	int j = i * 2;// line 12, invokeInLoop.html
		p("	<p>\n" + 
"	");// line 12, invokeInLoop.html
				actionRunners.put(getOut().length(), new cn.bran.play.CacheablePlayActionRunner("", Application.class, "echo", j) {
			@Override
			public void runPlayAction() throws cn.bran.play.JapidResult {
				Application.echo(j); // line 14, invokeInLoop.html
			}
		}); p("\n");// line 14, invokeInLoop.html
		p("\n" + 
"	</p>\n");// line 14, invokeInLoop.html
		}// line 16, invokeInLoop.html
		p("<br/>\n" + 
"\n");// line 16, invokeInLoop.html
		p("\n" + 
"\n");// line 19, invokeInLoop.html
		for (final Post p : posts) {// line 21, invokeInLoop.html
		p("    another notation for invoking actions:  \n" + 
"    <p>\n" + 
"    ");// line 21, invokeInLoop.html
				actionRunners.put(getOut().length(), new cn.bran.play.CacheablePlayActionRunner("", Application.class, "echoPost", p) {
			@Override
			public void runPlayAction() throws cn.bran.play.JapidResult {
				Application.echoPost(p); // line 24, invokeInLoop.html
			}
		}); p("\n");// line 24, invokeInLoop.html
		p("    </p>\n");// line 24, invokeInLoop.html
		}// line 26, invokeInLoop.html
		;// line 26, invokeInLoop.html
		
		endDoLayout(sourceTemplate);
	}

}