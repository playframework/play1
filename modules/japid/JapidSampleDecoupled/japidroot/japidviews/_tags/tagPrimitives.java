//version: 0.9.5
package japidviews._tags;
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
// NOTE: This file was generated from: japidviews/_tags/tagPrimitives.html
// Change to this file will be lost next time the template file is compiled.
//
@cn.bran.play.NoEnhance
public class tagPrimitives extends cn.bran.play.JapidTemplateBase
{
	public static final String sourceTemplate = "japidviews/_tags/tagPrimitives.html";
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


	public tagPrimitives() {
	super((StringBuilder)null);
	initHeaders();
	}
	public tagPrimitives(StringBuilder out) {
		super(out);
		initHeaders();
	}
	public tagPrimitives(cn.bran.japid.template.JapidTemplateBaseWithoutPlay caller) {
		super(caller);
	}

/* based on https://github.com/branaway/Japid/issues/12
 */
	public static final String[] argNames = new String[] {/* args of the template*/"s", "i", "ii", "d", "dd", "b", "bb", "map", "f",  };
	public static final String[] argTypes = new String[] {/* arg types of the template*/"String", "int", "Integer", "double", "Double", "boolean", "Boolean", "Map<Object, String>", "float",  };
	public static final Object[] argDefaults= new Object[] {null,null,null,null,null,null,null,null,null, };
	public static java.lang.reflect.Method renderMethod = getRenderMethod(japidviews._tags.tagPrimitives.class);

	{
		setRenderMethod(renderMethod);
		setArgNames(argNames);
		setArgTypes(argTypes);
		setArgDefaults(argDefaults);
		setSourceTemplate(sourceTemplate);
	}
////// end of named args stuff

	private String s; // line 1, japidviews/_tags/tagPrimitives.html
	private int i; // line 1, japidviews/_tags/tagPrimitives.html
	private Integer ii; // line 1, japidviews/_tags/tagPrimitives.html
	private double d; // line 1, japidviews/_tags/tagPrimitives.html
	private Double dd; // line 1, japidviews/_tags/tagPrimitives.html
	private boolean b; // line 1, japidviews/_tags/tagPrimitives.html
	private Boolean bb; // line 1, japidviews/_tags/tagPrimitives.html
	private Map<Object, String> map; // line 1, japidviews/_tags/tagPrimitives.html
	private float f; // line 1, japidviews/_tags/tagPrimitives.html
	public cn.bran.japid.template.RenderResult render(String s,int i,Integer ii,double d,Double dd,boolean b,Boolean bb,Map<Object, String> map,float f) {
		this.s = s;
		this.i = i;
		this.ii = ii;
		this.d = d;
		this.dd = dd;
		this.b = b;
		this.bb = bb;
		this.map = map;
		this.f = f;
		try {super.layout();} catch (RuntimeException __e) { super.handleException(__e);} // line 1, japidviews/_tags/tagPrimitives.html
		return getRenderResult();
	}

	public static cn.bran.japid.template.RenderResult apply(String s,int i,Integer ii,double d,Double dd,boolean b,Boolean bb,Map<Object, String> map,float f) {
		return new tagPrimitives().render(s, i, ii, d, dd, b, bb, map, f);
	}

	@Override protected void doLayout() {
		beginDoLayout(sourceTemplate);
;// line 1, tagPrimitives.html
		p("<div>");// line 12, tagPrimitives.html
		p(s);// line 13, tagPrimitives.html
		p("</div>\n" + 
"<div>");// line 13, tagPrimitives.html
		p(i);// line 14, tagPrimitives.html
		p(", ");// line 14, tagPrimitives.html
		p(ii);// line 14, tagPrimitives.html
		p("</div>\n" + 
"<div>");// line 14, tagPrimitives.html
		p(d);// line 15, tagPrimitives.html
		p(", ");// line 15, tagPrimitives.html
		p(dd);// line 15, tagPrimitives.html
		p("</div>\n" + 
"<div>");// line 15, tagPrimitives.html
		p(b);// line 16, tagPrimitives.html
		p(", ");// line 16, tagPrimitives.html
		p(bb);// line 16, tagPrimitives.html
		p("</div>\n" + 
"<div>");// line 16, tagPrimitives.html
		p(map);// line 17, tagPrimitives.html
		p("</div>\n" + 
"<div>");// line 17, tagPrimitives.html
		p(f);// line 18, tagPrimitives.html
		p("</div>\n" + 
"\n");// line 18, tagPrimitives.html
		
		endDoLayout(sourceTemplate);
	}

}