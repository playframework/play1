//version: 0.9.5
package japidviews._layouts;
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
// NOTE: This file was generated from: japidviews/_layouts/lcomposite2.html
// Change to this file will be lost next time the template file is compiled.
//
@cn.bran.play.NoEnhance
public abstract class lcomposite2 extends cn.bran.play.JapidTemplateBase
{
	public static final String sourceTemplate = "japidviews/_layouts/lcomposite2.html";
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


	public lcomposite2() {
	super((StringBuilder)null);
	initHeaders();
	}
	public lcomposite2(StringBuilder out) {
		super(out);
		initHeaders();
	}
	public lcomposite2(cn.bran.japid.template.JapidTemplateBaseWithoutPlay caller) {
		super(caller);
	}

	@Override public void layout() {
		beginDoLayout(sourceTemplate);
p("<p>beginning: lcomposite</p>\n" + 
"\n");// line 1, lcomposite2.html
		p("\n" + 
"\n");// line 3, lcomposite2.html
				actionRunners.put(getOut().length(), new cn.bran.play.CacheablePlayActionRunner("", controllers.Application.class, "foo", "") {
			@Override
			public void runPlayAction() throws cn.bran.play.JapidResult {
				controllers.Application.foo(); // line 5, lcomposite2.html
			}
		}); p("\n");// line 5, lcomposite2.html

doLayout();// line 7, lcomposite2.html
		p("\n" + 
"<p>back to layout</p>\n" + 
"\n" + 
"\n");// line 7, lcomposite2.html
				actionRunners.put(getOut().length(), new cn.bran.play.CacheablePlayActionRunner("", controllers.Application.class, "foo", "") {
			@Override
			public void runPlayAction() throws cn.bran.play.JapidResult {
				controllers.Application.foo(); // line 12, lcomposite2.html
			}
		}); p("\n");// line 12, lcomposite2.html
		p("\n" + 
"<p>back to layout again</p>\n" + 
"\n" + 
"<p>end of lcomposite</p>\n");// line 12, lcomposite2.html
		
		endDoLayout(sourceTemplate);
	}


	protected abstract void doLayout();
}