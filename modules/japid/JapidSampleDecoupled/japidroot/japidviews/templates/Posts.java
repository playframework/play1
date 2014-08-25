//version: 0.9.5
package japidviews.templates;
import java.util.*;
import java.io.*;
import cn.bran.japid.tags.Each;
import models.japidsample.Post;
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
// NOTE: This file was generated from: japidviews/templates/Posts.html
// Change to this file will be lost next time the template file is compiled.
//
@cn.bran.play.NoEnhance
public class Posts extends cn.bran.play.JapidTemplateBase
{
	public static final String sourceTemplate = "japidviews/templates/Posts.html";
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


	public Posts() {
	super((StringBuilder)null);
	initHeaders();
	}
	public Posts(StringBuilder out) {
		super(out);
		initHeaders();
	}
	public Posts(cn.bran.japid.template.JapidTemplateBaseWithoutPlay caller) {
		super(caller);
	}

/* based on https://github.com/branaway/Japid/issues/12
 */
	public static final String[] argNames = new String[] {/* args of the template*/"blogTitle", "allPost",  };
	public static final String[] argTypes = new String[] {/* arg types of the template*/"String", "List<Post>",  };
	public static final Object[] argDefaults= new Object[] {null,null, };
	public static java.lang.reflect.Method renderMethod = getRenderMethod(japidviews.templates.Posts.class);

	{
		setRenderMethod(renderMethod);
		setArgNames(argNames);
		setArgTypes(argTypes);
		setArgDefaults(argDefaults);
		setSourceTemplate(sourceTemplate);
	}
////// end of named args stuff

	private String blogTitle; // line 2, japidviews/templates/Posts.html
	private List<Post> allPost; // line 2, japidviews/templates/Posts.html
	public cn.bran.japid.template.RenderResult render(String blogTitle,List<Post> allPost) {
		this.blogTitle = blogTitle;
		this.allPost = allPost;
		try {super.layout();} catch (RuntimeException __e) { super.handleException(__e);} // line 2, japidviews/templates/Posts.html
		return getRenderResult();
	}

	public static cn.bran.japid.template.RenderResult apply(String blogTitle,List<Post> allPost) {
		return new Posts().render(blogTitle, allPost);
	}

	@Override protected void doLayout() {
		beginDoLayout(sourceTemplate);
;// line 1, Posts.html

for (Post post: allPost) { // line 4, Posts.html
		p("	- title: ");// line 4, Posts.html
		p(post.title);// line 5, Posts.html
		p("\n" + 
"	- date: ");// line 5, Posts.html
		p(post.postedAt);// line 6, Posts.html
		p("\n" + 
"	- author ");// line 6, Posts.html
		p(post.author.name);// line 7, Posts.html
		p(" ");// line 7, Posts.html
		p(post.author.gender);// line 7, Posts.html
		p("\n" + 
"	the real title: 你好\n");// line 7, Posts.html
		}// line 9, Posts.html
		p("\n");// line 9, Posts.html
		
		endDoLayout(sourceTemplate);
	}

}