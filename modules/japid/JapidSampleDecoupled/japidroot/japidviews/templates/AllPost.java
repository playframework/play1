//version: 0.9.5
package japidviews.templates;
import java.util.*;
import java.io.*;
import cn.bran.japid.tags.Each;
import	 	models.japidsample.Post;
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
// NOTE: This file was generated from: japidviews/templates/AllPost.html
// Change to this file will be lost next time the template file is compiled.
//
@cn.bran.play.NoEnhance
public class AllPost extends Layout
{
	public static final String sourceTemplate = "japidviews/templates/AllPost.html";
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


	public AllPost() {
	super((StringBuilder)null);
	initHeaders();
	}
	public AllPost(StringBuilder out) {
		super(out);
		initHeaders();
	}
	public AllPost(cn.bran.japid.template.JapidTemplateBaseWithoutPlay caller) {
		super(caller);
	}

/* based on https://github.com/branaway/Japid/issues/12
 */
	public static final String[] argNames = new String[] {/* args of the template*/"blogTitle", "allPost",  };
	public static final String[] argTypes = new String[] {/* arg types of the template*/"String", "List<Post>",  };
	public static final Object[] argDefaults= new Object[] {null,null, };
	public static java.lang.reflect.Method renderMethod = getRenderMethod(japidviews.templates.AllPost.class);

	{
		setRenderMethod(renderMethod);
		setArgNames(argNames);
		setArgTypes(argTypes);
		setArgDefaults(argDefaults);
		setSourceTemplate(sourceTemplate);
	}
////// end of named args stuff

	private String blogTitle; // line 3, japidviews/templates/AllPost.html
	private List<Post> allPost; // line 3, japidviews/templates/AllPost.html
	public cn.bran.japid.template.RenderResult render(String blogTitle,List<Post> allPost) {
		this.blogTitle = blogTitle;
		this.allPost = allPost;
		setStopwatchOn();
		try {super.layout();} catch (RuntimeException __e) { super.handleException(__e);} // line 3, japidviews/templates/AllPost.html
		return getRenderResult();
	}

	public static cn.bran.japid.template.RenderResult apply(String blogTitle,List<Post> allPost) {
		return new AllPost().render(blogTitle, allPost);
	}

	@Override protected void doLayout() {
		beginDoLayout(sourceTemplate);
;// line 1, AllPost.html
;// line 2, AllPost.html
		;// line 7, AllPost.html

new Runnable() {public void run() {
int _size = -100; int _index = 0; boolean _isOdd = false; String _parity = ""; boolean _isFirst = true; Boolean _isLast = _index == _size;
for (Post p : allPost) { // line 12, AllPost.html
	_index++; _isOdd = !_isOdd; _parity = _isOdd? "odd" : "even"; _isFirst = _index == 1; if (_size == -100) _size = getCollectionSize(allPost); _isLast = (_size < 0 ? null : _index == _size);
// line 12, AllPost.html
		p("\n" + 
"        ");// line 12, AllPost.html
		p("\n" + 
"\n" + 
"	    ");// line 16, AllPost.html
		new Display(AllPost.this).render( // line 18, AllPost.html
new Display.DoBody<String>(){ // line 18, AllPost.html
public void render(final String title) { // line 18, AllPost.html
// line 18, AllPost.html
		p("		   The real title is: ");// line 18, AllPost.html
		p(title);// line 19, AllPost.html
		p("\n" + 
"	    ");// line 19, AllPost.html
		
}

StringBuilder oriBuffer;
@Override
public void setBuffer(StringBuilder sb) {
	oriBuffer = getOut();
	setOut(sb);
}

@Override
public void resetBuffer() {
	setOut(oriBuffer);
}

}
, named("post", p), named("as", "home222"));// line 18, AllPost.html

}
}}.run();
// line 12, AllPost.html

new Tag2(AllPost.this).render(named("msg", blogTitle), named("age", 100)); // line 23, AllPost.html// line 23, AllPost.html
		p("\n" + 
"<p>cool cool!</p>");// line 23, AllPost.html
		
		endDoLayout(sourceTemplate);
	}

	@Override protected void title() {
		p( "Home of " + blogTitle);;
	}
}