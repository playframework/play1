//version: 0.9.5
package japidviews.more.Perf;
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
// NOTE: This file was generated from: japidviews/more/Perf/perfmain.html
// Change to this file will be lost next time the template file is compiled.
//
@cn.bran.play.NoEnhance
public abstract class perfmain extends cn.bran.play.JapidTemplateBase
{
	public static final String sourceTemplate = "japidviews/more/Perf/perfmain.html";
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


	public perfmain() {
	super((StringBuilder)null);
	initHeaders();
	}
	public perfmain(StringBuilder out) {
		super(out);
		initHeaders();
	}
	public perfmain(cn.bran.japid.template.JapidTemplateBaseWithoutPlay caller) {
		super(caller);
	}

	private DataModel.User loggedInUser; // line 1, japidviews/more/Perf/perfmain.html
	 public void layout(DataModel.User loggedInUser) {
		this.loggedInUser = loggedInUser;
		beginDoLayout(sourceTemplate);
;// line 1, perfmain.html
		p("\n" + 
"<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\"\n" + 
"        \"http://www.w3.org/TR/html4/loose.dtd\">\n" + 
"<html>\n" + 
"<head>\n" + 
"    <title>");// line 1, perfmain.html
		title();p("</title>\n" + 
"</head>\n" + 
"<body>\n" + 
"\n");// line 7, perfmain.html
		if (loggedInUser != null) {// line 11, perfmain.html
		p("	<div>\n" + 
"	    Hello ");// line 11, perfmain.html
		p(loggedInUser.getUserName());// line 13, perfmain.html
		p(", You have ");// line 13, perfmain.html
		p(loggedInUser.getFriends().size());// line 13, perfmain.html
		p(" friends\n" + 
"	</div>\n");// line 13, perfmain.html
		}// line 15, perfmain.html
		p("\n" + 
"<h1>Entries</h1>\n" + 
"    ");// line 15, perfmain.html
		doLayout();// line 18, perfmain.html
		p("</body>\n" + 
"</html>\n");// line 18, perfmain.html
		
		endDoLayout(sourceTemplate);
	}

	 protected void title() {};

	protected abstract void doLayout();
}