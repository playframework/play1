//version: 0.9.5.1
package japidviews._layouts;
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
import japidviews._layouts.*;
import models.*;
import play.mvc.Http.*;
//
// NOTE: This file was generated from: japidviews/_layouts/main.html
// Change to this file will be lost next time the template file is compiled.
//
public abstract class main extends cn.bran.play.JapidTemplateBase
{
	public static final String sourceTemplate = "japidviews/_layouts/main.html";
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


	public main() {
	super((StringBuilder)null);
	initHeaders();
	}
	public main(StringBuilder out) {
		super(out);
		initHeaders();
	}
	public main(cn.bran.japid.template.JapidTemplateBaseWithoutPlay caller) {
		super(caller);
	}

	@Override public void layout() {
		beginDoLayout(sourceTemplate);
p("<!DOCTYPE html>\n" + 
"<html>\n" + 
"    <head>\n" + 
"    	<title>Contact2/Japid - ");// line 1, main.html
		title();p(", by Zenexity/Bing Ran</title>\n" + 
"    	<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\"/>\n" + 
"        <link rel=\"stylesheet\" type=\"text/css\" media=\"screen\" href=\"");// line 4, main.html
		p(lookupStatic("public/stylesheets/style.css"));// line 6, main.html
		p("\" />\n" + 
"        <link rel=\"stylesheet\" type=\"text/css\" media=\"screen\" href=\"");// line 6, main.html
		p(lookupStatic("public/stylesheets/south-street/jquery-ui-1.7.2.custom.css"));// line 7, main.html
		p("\" />\n" + 
"    	<script src=\"");// line 7, main.html
		p(lookupStatic("public/javascripts/jquery-1.4.min.js"));// line 8, main.html
		p("\" type=\"text/javascript\" charset=\"utf-8\"></script>\n" + 
"    	<script src=\"");// line 8, main.html
		p(lookupStatic("public/javascripts/jquery-ui-1.7.2.custom.min.js"));// line 9, main.html
		p("\" type=\"text/javascript\" charset=\"utf-8\"></script>\n" + 
"    	<script src=\"");// line 9, main.html
		p(lookupStatic("public/javascripts/jquery.editinplace.packed.js"));// line 10, main.html
		p("\" type=\"text/javascript\" charset=\"utf-8\"></script>\n" + 
"    </head>\n" + 
"	<body>\n" + 
"	    <div id=\"zencontact\">\n" + 
"    		<header>\n" + 
"    			<img src=\"");// line 10, main.html
		p(lookupStatic("public/images/logo.png"));// line 15, main.html
		p("\" alt=\"logo\" id=\"logo\" />\n" + 
"    			<h1>Japid Contact <span>by zenexity & Bing Ran</span></h1>\n" + 
"    		</header>\n" + 
"    		<nav>\n" + 
"    			<a id=\"home\" href=\"");// line 15, main.html
		p(lookup("index", new Object[]{}));// line 19, main.html
		p("\" class=\"");// line 19, main.html
		p(selected(".*index"));// line 19, main.html
		p("\">Home</a>\n" + 
"    			<a id=\"list\" href=\"");// line 19, main.html
		p(lookup("list", new Object[]{}));// line 20, main.html
		p("\" class=\"");// line 20, main.html
		p(selected(".*list"));// line 20, main.html
		p("\">List</a>\n" + 
"    			<a id=\"new\" href=\"");// line 20, main.html
		p(lookup("form", new Object[]{}));// line 21, main.html
		p("\" class=\"");// line 21, main.html
		p(selected(".*form|.*save"));// line 21, main.html
		p("\">New</a>\n" + 
"    		</nav>\n" + 
"    		<section>\n" + 
"    		    ");// line 21, main.html
		doLayout();// line 24, main.html
		p("    		</section>\n" + 
"    		<footer>\n" + 
"    			<a href=\"http://www.w3.org/TR/html5/\">html5</a> - \n" + 
"    			<a href=\"http://www.w3.org/TR/css3-roadmap/\">css3</a> - \n" + 
"    			<a href=\"http://www.playframework.org/\">playframework with Japid8</a> \n" + 
"    		</footer>\n" + 
"		</div>\n" + 
"	</body>\n" + 
"</html>\n" + 
"\n");// line 24, main.html
		p("\n");// line 35, main.html
		// line 37, main.html
		;// line 39, main.html
		
		endDoLayout(sourceTemplate);
	}

	 protected void title() {};

	protected abstract void doLayout();
public String selected(String pattern) {
StringBuilder sb = new StringBuilder();
StringBuilder ori = getOut();
this.setOut(sb);
TreeMap<Integer, cn.bran.japid.template.ActionRunner> parentActionRunners = actionRunners;
actionRunners = new TreeMap<Integer, cn.bran.japid.template.ActionRunner>();
// line 37, main.html
		p("");// line 37, main.html
		p(request.action.matches(pattern) ? "selected" : "");// line 38, main.html
		p("");// line 38, main.html
		
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