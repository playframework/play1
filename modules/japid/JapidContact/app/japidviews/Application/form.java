//version: 0.9.5.2
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
import play.data.validation.Validation;
import play.i18n.Lang;
import controllers.*;
import static japidviews._javatags.JapidWebUtil.*;
import japidviews._layouts.*;
import models.*;
import play.mvc.Http.*;
//
// NOTE: This file was generated from: japidviews/Application/form.html
// Change to this file will be lost next time the template file is compiled.
//
@cn.bran.play.NoEnhance
public class form extends main
{
	public static final String sourceTemplate = "japidviews/Application/form.html";
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


	public form() {
	super((StringBuilder)null);
	initHeaders();
	}
	public form(StringBuilder out) {
		super(out);
		initHeaders();
	}
	public form(cn.bran.japid.template.JapidTemplateBaseWithoutPlay caller) {
		super(caller);
	}

/* based on https://github.com/branaway/Japid/issues/12
 */
	public static final String[] argNames = new String[] {/* args of the template*/"contact",  };
	public static final String[] argTypes = new String[] {/* arg types of the template*/"Contact",  };
	public static final Object[] argDefaults= new Object[] {null, };
	public static java.lang.reflect.Method renderMethod = getRenderMethod(japidviews.Application.form.class);

	{
		setRenderMethod(renderMethod);
		setArgNames(argNames);
		setArgTypes(argTypes);
		setArgDefaults(argDefaults);
		setSourceTemplate(sourceTemplate);
	}
////// end of named args stuff

	private Contact contact; // line 1, japidviews/Application/form.html
	public cn.bran.japid.template.RenderResult render(Contact contact) {
		this.contact = contact;
		try {super.layout();} catch (RuntimeException __e) { super.handleException(__e);} // line 1, japidviews/Application/form.html
		return getRenderResult();
	}

	public static cn.bran.japid.template.RenderResult apply(Contact contact) {
		return new form().render(contact);
	}

	@Override protected void doLayout() {
		beginDoLayout(sourceTemplate);
;// line 1, form.html
		;// line 1, form.html
p("<form action=\"");// line 4, form.html
		p(lookup("save", new Object[]{}));// line 6, form.html
		p("\" method=\"POST\">\n" + 
"    ");// line 6, form.html
		try { p(authenticityToken()); } catch (NullPointerException npe) {}// line 7, form.html
		p("    <input type=\"hidden\" name=\"contact.id\" value=\"");// line 7, form.html
		try { p(contact.id); } catch (NullPointerException npe) {}// line 8, form.html
		p("\">\n" + 
"    \n" + 
"    <p class=\"field\">\n" + 
"        <label for=\"name\">Name:</label>\n" + 
"        <input type=\"text\" id=\"name\" name=\"contact.name\" value=\"");// line 8, form.html
		try { p(contact.name); } catch (NullPointerException npe) {}// line 12, form.html
		p("\">\n" + 
"        <span class=\"error\">");// line 12, form.html
		try { p(error("contact.name")); } catch (NullPointerException npe) {}// line 13, form.html
		p("</span>\n" + 
"    </p>\n" + 
"\n" + 
"    <p class=\"field\">\n" + 
"        <label for=\"firstname\">First name:</label>\n" + 
"        <input type=\"text\" id=\"firstname\" name=\"contact.firstname\" value=\"");// line 13, form.html
		try { p(contact.firstname); } catch (NullPointerException npe) {}// line 18, form.html
		p("\">\n" + 
"        <span class=\"error\">");// line 18, form.html
		try { p(error("contact.firstname")); } catch (NullPointerException npe) {}// line 19, form.html
		p("</span>\n" + 
"    </p>\n" + 
"\n" + 
"    <p class=\"field\">\n" + 
"        <label for=\"birthdate\">Birth date:</label>\n" + 
"        <input type=\"text\" id=\"birthdate\" name=\"contact.birthdate\" value=\"");// line 19, form.html
		try { p(format(contact.birthdate, "yyyy-MM-dd")); } catch (NullPointerException npe) {}// line 24, form.html
		p("\">\n" + 
"        <span class=\"error\">");// line 24, form.html
		try { p(error("contact.birthdate")); } catch (NullPointerException npe) {}// line 25, form.html
		p("</span>\n" + 
"    </p>\n" + 
"\n" + 
"    <p class=\"field\">\n" + 
"        <label for=\"email\">Email:</label>\n" + 
"        <input type=\"text\" id=\"email\" name=\"contact.email\" value=\"");// line 25, form.html
		try { p(contact.email); } catch (NullPointerException npe) {}// line 30, form.html
		p("\">\n" + 
"        <span class=\"error\">");// line 30, form.html
		try { p(error("contact.email")); } catch (NullPointerException npe) {}// line 31, form.html
		p("</span>\n" + 
"    </p>\n" + 
"\n" + 
"    <p class=\"buttons\">\n" + 
"        <a href=\"");// line 31, form.html
		p(lookup("list", new Object[]{}));// line 35, form.html
		p("\">Cancel</a> or <input type=\"submit\" value=\"Save this contact\" id=\"saveContact\">\n" + 
"    </p>\n" + 
"    \n" + 
"    <script type=\"text/javascript\" charset=\"utf-8\">\n" + 
"        $(\"#birthdate\").datepicker({dateFormat:'yy-mm-dd', showAnim:'fadeIn'})\n" + 
"    </script>\n" + 
"</form>\n");// line 35, form.html
		
		endDoLayout(sourceTemplate);
	}

	@Override protected void title() {
		p("Form");;
	}
}