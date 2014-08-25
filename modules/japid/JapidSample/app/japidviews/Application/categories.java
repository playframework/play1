//version: 0.9.5
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
import japidviews._tags.*;
import play.data.validation.Validation;
import play.i18n.Lang;
import controllers.*;
import japidviews._layouts.*;
import models.*;
import play.mvc.Http.*;
//
// NOTE: This file was generated from: japidviews/Application/categories.html
// Change to this file will be lost next time the template file is compiled.
//
@cn.bran.play.NoEnhance
public class categories extends cn.bran.play.JapidTemplateBase
{
	public static final String sourceTemplate = "japidviews/Application/categories.html";
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


	public categories() {
	super((StringBuilder)null);
	initHeaders();
	}
	public categories(StringBuilder out) {
		super(out);
		initHeaders();
	}
	public categories(cn.bran.japid.template.JapidTemplateBaseWithoutPlay caller) {
		super(caller);
	}

/* based on https://github.com/branaway/Japid/issues/12
 */
	public static final String[] argNames = new String[] {/* args of the template*/"categories",  };
	public static final String[] argTypes = new String[] {/* arg types of the template*/"List<Category>",  };
	public static final Object[] argDefaults= new Object[] {null, };
	public static java.lang.reflect.Method renderMethod = getRenderMethod(japidviews.Application.categories.class);

	{
		setRenderMethod(renderMethod);
		setArgNames(argNames);
		setArgTypes(argTypes);
		setArgDefaults(argDefaults);
		setSourceTemplate(sourceTemplate);
	}
////// end of named args stuff

	private List<Category> categories; // line 1, japidviews/Application/categories.html
	public cn.bran.japid.template.RenderResult render(List<Category> categories) {
		this.categories = categories;
		try {super.layout();} catch (RuntimeException __e) { super.handleException(__e);} // line 1, japidviews/Application/categories.html
		return getRenderResult();
	}

	public static cn.bran.japid.template.RenderResult apply(List<Category> categories) {
		return new categories().render(categories);
	}

	@Override protected void doLayout() {
		beginDoLayout(sourceTemplate);
p(" ");// line 1, categories.html
 if(asBoolean(categories)) {// line 2, categories.html
		p("     <ul>\n" + 
"       ");// line 2, categories.html
		for(Category cat: categories) {// line 4, categories.html
		p("	       <li>\n" + 
"	           <a href=\"\">");// line 4, categories.html
		p(cat.name);// line 6, categories.html
		p("</a>\n" + 
"	           ");// line 6, categories.html
		new categories(categories.this).render(cat.subCategories); // line 7, categories.html// line 7, categories.html
		p("	       </li>\n" + 
"       ");// line 7, categories.html
		}// line 9, categories.html
		p("     </ul>\n" + 
" ");// line 9, categories.html
		}// line 11, categories.html
		p(" \n" + 
" ");// line 11, categories.html
		
		endDoLayout(sourceTemplate);
	}

}