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
// NOTE: This file was generated from: japidviews/more/MyController/scriptlineLayout.html
// Change to this file will be lost next time the template file is compiled.
//
@cn.bran.play.NoEnhance
public abstract class scriptlineLayout extends cn.bran.play.JapidTemplateBase
{
	public static final String sourceTemplate = "japidviews/more/MyController/scriptlineLayout.html";
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


	public scriptlineLayout() {
	super((StringBuilder)null);
	initHeaders();
	}
	public scriptlineLayout(StringBuilder out) {
		super(out);
		initHeaders();
	}
	public scriptlineLayout(cn.bran.japid.template.JapidTemplateBaseWithoutPlay caller) {
		super(caller);
	}

	@Override public void layout() {
		beginDoLayout(sourceTemplate);
p("the meta is  \"");// line 1, scriptlineLayout.html
		meta();p("\"\n" + 
"\n");// line 1, scriptlineLayout.html
		doLayout();// line 3, scriptlineLayout.html
		
		endDoLayout(sourceTemplate);
	}

	 protected void meta() {};

	protected abstract void doLayout();
}