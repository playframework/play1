package controllers;

import japidviews.Application.authorPanel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import models.Category;
import models.SearchParams;
import models.japidsample.Author;
import models.japidsample.Author2;
import models.japidsample.Post;
import notifiers.TestEmailer;
import play.cache.CacheFor;
import cn.bran.japid.template.RenderResult;
import cn.bran.play.CacheableRunner;
import cn.bran.play.JapidController;
import cn.bran.play.JapidResult;
/**
 *  A sample controller that demos Japid features
 *  
 * @author Bing Ran<bing_ran@hotmail.com>
 *
 */

public class Application extends JapidController {
	public static void index() {
		renderJapid(); // use the default index.html in the japidviews/SampleController directory
//		renderJapidWith("@index.html"); // use the default index.html in the japidviews/SampleController directorypost
	}
	public static void indexAt() {
		renderJapid(); // 
	}
	public static void authorPanel(final Author a) {
		boolean calledFromView = isInvokedfromJapidView();
		System.out.println("calledFromView: " + calledFromView);
		CacheableRunner r = new CacheableRunner("10s", CacheableRunner.genCacheKey()) {
			@Override
			protected RenderResult render() {
				return new authorPanel().render(a);
			}
		};
		
		throw new JapidResult(r.run());
		//	or 		render(r);
	}
	
	public static void authorPanel2(final Author a) {
		renderJapid(a);
	}
	
	public static void cacheWithRenderJapid(final String a) {
//			CacheableRunner r = new CacheableRunner("5s", genCacheKey()) {
		CacheableRunner r = new CacheableRunner("5s") {
			@Override
			protected RenderResult render() {
				System.out.println("rerender...");
				String b = a + new Date().getSeconds();
				return getRenderResultWith("", b);
			}
		};
		
//		throw new JapidResult(r.run()).eval(); // eval effectively cancel nested finer cache control
		render(r);
	}
	
	@CacheFor("6s")
	public static void testCacheFor(String p) {
		System.out.println("rerender...");
		String b = "" + new Date().getSeconds();

		renderJapid(b); // nested cache control still in effect
//		renderJapidEager(b); // nested cache control not in effect
	}
	
	@CacheFor("3s")
	public static void every3() {
		System.out.println("every3 called");
		String b = "" + new Date().getSeconds();
		renderJapid(b); // nested cache control still in effect
	}
	
	@CacheFor("5s")
	public static void testCacheForEager(String p) {
		System.out.println("rerender...");
		String b = "" + new Date().getSeconds();
		
		renderJapidEager(b); // no nested cache control. the outer cache control overrides all
	}
	
	public static void seconds() {
		String b = "" + new Date().getSeconds();
		renderText(b);
	}
	
	@CacheFor("4s")
	public static void twoParams(String a, int b) {
		renderText(a + "=" +  b + ":" + new Date().getSeconds());
	}
	
	
	public static void foo() {
		StringBuilder sb = new StringBuilder();
		sb.append("------foo() action invoked:Hello foo!");
		RenderResult rr = new RenderResult(null, sb, 0);
		
		throw new JapidResult(rr);
		
//		runWithCache(new ActionRunner() {
//			@Override
//			public RenderResult run() {
//				return new authorPanel().render(a);
//			}
//		}, "10s", a);
	}
	
	public static void hello(String me, String you) {
		String m = "hi there and..." + me + you;
		String am = m + "!";
//		renderText("hello，Japid Play!");
		renderText(am);
	}
	
	public static void h1() {
		renderJapid("h1");
	}
	/**
	 * this method shows how to render arguments to a japid template by naming and positional convention with the 
	 * renderJapid().
	 * 
	 */
	public static void renderByPosition() {
		String s = "hello，renderByPosition！";
		int i = 100;
		Author a = new Author();
		a.name = "author1";

		Author2 a2 = new Author2();
		a2.name = "author2";
		
		renderJapid(s, i, a, a2, a2);
	}
	
	public static void renderByPositionEmpty() {
		renderJapid();
	}
	
	/**
	 * demo how to composite a page with independent segments with the #{invoke } tag
	 */
	public static void composite() {
		Post post = new Post();
		post.title = "test post";
		post.postedAt = new Date();
		post.content = "this is perfect piece of content~!";
		
		Author a = new Author();
		a.name = "me";
		a.birthDate = new Date();
		a.gender = 'm';
		
		post.setAuthor(a);
		
		renderJapid(post);
	}
	
	public static void reverseLookup0() {
		renderJapid();
	}

	public static void reverseLookup1(String[] args) {
		renderText("OK");
	}
	
	/**
	 * test the japid emailer
	 */
	
	public static void email() {
		Post p = new Post();
		p.title = "我自己";
		TestEmailer.emailme(p);
		renderText("mail sent");
	}
	
	public static void callTag() {
		dontRedirect();
		renderJapidWith("templates/callPicka");
	}
	
	public static void ct() {
		dontRedirect();
		renderJapidWith("templates/callPicka");
	}
	
	public static void yahoo() {
		Stream<String> stream = Stream.of("a", "bb", "zzzzz", "cccccc");
		String out = stream.sorted((a, b) -> a.length() - b.length()).map(a -> a + "---").collect(Collectors.joining(":::"));
		renderText(out);
	}
	
	public static void postList() {
		String title = "my Blog";
		List<Post> posts = createPosts();
		System.out.println(posts.stream().collect(Collectors.counting()));
		renderJapidWith("templates/AllPost", title, posts);
	}
	/**
	 * @return
	 */
	private static List<Post> createPosts() {
		List<Post> posts = new ArrayList<Post>();
		Author a = new Author();
		a.name = "冉兵";
		a.birthDate = new Date();
		a.gender = 'M';
		Post p = new Post();
		p.author = a;
		p.content = "long time ago......";
		p.postedAt = new Date();
		p.title = "post 1";
		posts.add(p);
		p = new Post();
		p.author = a;
		p.content = "way too long time ago...";
		p.postedAt = new Date();
		p.title = "post 2";
		posts.add(p);
		return posts;
	}
	
	public static void each() {
		List<String> list = Arrays.asList("as1", "as2", "as3", "as4", "as5", "as6");
		renderJapidWith("templates/EachCall", list);
	}
	
	/**
	 * test using primitive with renderText
	 * @param i
	 */
	public static void echo(int i) {
		renderText(i * 2);
	}
	
	public static void invokeInLoop() {
		renderJapidWith("templates/invokeInLoop", createPosts());
	}
	
	public static void echoPost(Post p) {
		renderText(p);
	}
	
	/**
	 * "official" Play treats body as a special param name to store all POST body if the content type is 
	 * application/x-www-form-urlencoded. bran's fork has changed the reserved param name to _body. 
	 * 
	 * @param f1
	 * @param f2
	 * @param body
	 */
	public static void dumpPost(String f1, String f2, String body) {
		if (f1 == null)
			f1 = "";
		
		if (f2 == null)
			f2 = "";
		
		if (body == null)
			body = "";
		else
			System.out.println("body: " + body);
		
		renderJapidWith("templates/dumpPost.html", f1, f2, body);
	}
	
	public static void in() {
		dontRedirect();
		out(1L, 2, "33");
	}
	
	public static void in2() {
		// magic redirect
		out(1L, 2, "3003");
	}
	
	public static void out(long a, int b, String c) {
		renderText("Hi out!" + a  + b + c);
	}

	public static void go(String template) {
		JapidController.renderJapidWith(template);
	}
	
	public static void decorateName(String name) {
		renderJapid(name);
	}
	public static void verbatim() {
		renderJapid();
	}
	
	public static void ifs() {
		String s = "";
		List<String> list = new ArrayList<String>();
		list.add("a");
		Object[] array = list.toArray();
		renderJapid(s, list, true, array, new int[] {}, 0, "a");
	}
	
	public static void ifs2() {
		renderJapid(2, new String[] {"as"});
	}
	
	public static void flashbad() {
		flash.error("something bad");
		// redirect to 
		flashout();
	}

	public static void flashMsg() {
		flash.put("msg", "a message");
		// redirect to 
		flashout();
	}

	public static void flashgood() {
		flash.success("cool");
		// redirect to 
		flashout();
	}
	
	public static void flashout() {
		renderJapid();
	}
	
	public static void validate(String name, Integer age) {
		   validation.required("name/姓名", name);
		   validation.required("age/年龄", age);
		   validation.min("age", age, 10);
		   renderJapidByName(named("name", name), named("age", age));
	}
	
	public static void reverseUrl() {
		renderJapid();
	}
	
	public static void search(SearchParams sp) {
		renderJapid(sp);
	}
	
	public static void groovy() {
		String a = "Groovy";
		// render with Groovy
		render(a);
	}
	

	public static void list() {
		renderJapid();
	}
	
	public static void escapedExpr() {
		renderJapid();
	}
	public static void categories() {
		Category a = new Category();
		a.name = "a";
		Category cate1 = new Category();
		cate1.name = "1";
		Category cate2 = new Category();
		cate2.name = "2";
		Category cate11 = new Category();
		cate11.name = "11";
		Category cate12 = new Category();
		cate12.name = "12";
		a.subCategories = new ArrayList<Category>();
		a.subCategories.add(cate1);
		a.subCategories.add(cate2);
		cate1.subCategories = new ArrayList<Category>();
		cate1.subCategories.add(cate11);
		cate1.subCategories.add(cate12);
		
		List<Category> cats = new ArrayList<Category>();
		cats.add(a);
		renderJapid(cats);
		
	}
	
	public static void special() {
		renderJapid();
	}
	
	public static void hellohello() {
		renderJapid();
	}

	public static void js() {
		renderJapid();
	}

	public static void ps1() {
		renderJapid();
	}

	public static void ps2(boolean boo) {
		renderJapid();
	}
}
