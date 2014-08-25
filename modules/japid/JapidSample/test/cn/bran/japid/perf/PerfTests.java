package cn.bran.japid.perf;

import japidviews.templates.AllPost;
import japidviews.templates.EachCall;
import japidviews.templates.Posts;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import models.japidsample.Author;
import models.japidsample.Post;

import org.junit.Test;

import cn.bran.japid.template.RenderResult;

public class PerfTests {
	/**
	 * this is the baseline
	 * 
	 * @throws UnsupportedEncodingException
	 * @throws IOException
	 */
	@Test
	public void testSimpleJavaPerf() throws UnsupportedEncodingException, IOException {
		List<Post> posts = createPosts();

		ByteArrayOutputStream baos = new ByteArrayOutputStream(10000);
		// warm up
		for (int i = 0; i < 15; i++) {
			// a simple loop without templating
			baos.reset();
			StringBuffer sb = new StringBuffer(1000);
			for (Post pp : posts) {
				sb.append("你");
				sb.append("Title: " + pp.getTitle());
				sb.append("Author name: " + pp.getAuthor().name);
				sb.append(pp.getAuthor().gender);
				sb.append(pp.getPostedAt());
				sb.append("\n" + "	the real title: 你好\n" + "");
			}
			String string = sb.toString();
			baos.write(string.getBytes("UTF-8"));
		}

		long tt = System.currentTimeMillis();
		for (int i = 0; i < 100; i++) {
			// a simple loop without templating
			baos.reset();
			long t = System.currentTimeMillis();
			// StringBuffer sb = new StringBuffer(1000);
			StringBuilder sb = new StringBuilder(1000);
			// StringBundler sb = new StringBundler();
			for (Post pp : posts) {
				sb.append("你");
				sb.append("Title: " + pp.getTitle());
				sb.append("Author name: " + pp.getAuthor().name);
				sb.append(pp.getAuthor().gender);
				sb.append(pp.getPostedAt());
				sb.append("\n" + "	the real title: 你好\n" + "");
			}
			String string = sb.toString();
			baos.write(string.getBytes("UTF-8"));
			System.out.println(System.currentTimeMillis() - t);
		}
		System.out.println("total time: " + (System.currentTimeMillis() - tt));
	}

	@Test
	public void testJapid() throws InterruptedException {
		List<Post> posts = createPosts();
		// AllPost te = new AllPost(new
		// org.apache.commons.io.output.ByteArrayOutputStream());
		AllPost te = new AllPost(new StringBuilder(8000));
		long t = System.currentTimeMillis();
		long tt = System.currentTimeMillis();
		for (int i = 0; i < 100; i++) {
			// System.out.println("run templating: " + i);
			// StringWriter out = new StringWriter(1000);
			// PrintWriter printWriter = new PrintWriter(out);
			te = new AllPost(new StringBuilder(1000));
			t = System.currentTimeMillis();
			te.render("抬头", posts);
			System.out.println(System.currentTimeMillis() - t);
			// System.out.println(out.toString());
			// Thread.sleep(5);
		}
		System.out.println("total time: " + (System.currentTimeMillis() - tt));
	}

	/**
	 * @return
	 */
	private static List<Post> createPosts() {
		Author a = new Author();
		a.name = "作者";
		a.birthDate = new Date();
		a.gender = 'm';

		final Post p = new Post();
		p.setAuthor(a);
		p.setContent("这是一个很好的内容开始和结束都很好. ");
		p.setPostedAt(new Date());
		p.setTitle("测试");

		List<Post> posts = new ArrayList<Post>();

		for (int i = 0; i < 500; i++) {
			posts.add(p);
		}
		return posts;
	}

	@Test
	public void simleRun() throws UnsupportedEncodingException {
		Author a = new Author();
		a.name = "作者";
		a.birthDate = new Date();
		a.gender = 'm';

		final Post p = new Post();
		p.setAuthor(a);
		p.setContent("这是一个很好的内容开始和结束都很好. ");
		p.setPostedAt(new Date());
		p.setTitle("测试");

		List<Post> posts = new ArrayList<Post>();

		for (int i = 0; i < 5; i++) {
			posts.add(p);
		}

		long t = System.currentTimeMillis();
		RenderResult r = new AllPost().render("抬头", posts);
		System.out.println(System.currentTimeMillis() - t);
		System.out.println(r.getContent().toString());
	}

	public static void main(String[] args) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream(1000000);
		List<String> ps = Arrays.asList(new String[] {"a", "b", "c"});
		int in = 0;
		int c = 0;
		while ('q' != in) {
			for (c = 0; c < 50000; c++) {
				EachCall call = new EachCall();
				String string = call.render(ps).getContent().toString();
				baos.write(c + string.length());
			}
			System.out.println("ready:");
			in = System.in.read();
			System.out.println("got: " + (char)in);
		}
	}

	@Test
	public void testSinglePageJapid() throws Exception {
		final List<Post> posts = createPosts();
		// execute the template
		long tt = System.currentTimeMillis();
		ByteArrayOutputStream baos = new ByteArrayOutputStream(10000);
		Posts te = new Posts();
		te.render("抬头", posts);
		te = new Posts();
		te.render("抬头", posts);
		te = new Posts();
		te.render("抬头", posts);
		baos.write(te.toString().getBytes("UTF-8"));
		te = new Posts();
		te.render("抬头", posts);
		baos.write(te.toString().getBytes("UTF-8"));
		baos.write(te.toString().getBytes("UTF-8"));
		for (int i = 0; i < 1000; i++) {
			baos.reset();
			long t = System.currentTimeMillis();
			// System.out.println("run templating: " + i);
			/* Merge data-model with template */

			// StringWriter out = new StringWriter(1000);
			// PrintWriter printWriter = new PrintWriter(out);
			te = new Posts();
			te.render("抬头", posts);
			baos.write(te.toString().getBytes("UTF-8"));
			// System.out.println(out.toString());
			// out.flush();
			// System.out.println(System.currentTimeMillis() - t);
			// System.out.println(out.toString());
			// Thread.sleep(5);
		}
		System.out.println("total time: " + (System.currentTimeMillis() - tt));

	}

	// @Test
	// public void testFreeMarker() throws Exception {
	// final List<Post> posts = createPosts();
	// Map<String, Object> m = new HashMap<String, Object>() {
	// {
	// put("posts", posts);
	// }
	// };
	//
	// /* ------------------------------------------------------------------- */
	// /* You should do this ONLY ONCE in the whole application life-cycle: */
	//
	// /* Create and adjust the configuration */
	// Configuration cfg = new Configuration();
	// cfg.setDirectoryForTemplateLoading(new
	// File("D:\\eclipse-workspace\\Japid\\tests\\"));
	// cfg.setObjectWrapper(new DefaultObjectWrapper());
	//
	// /* ------------------------------------------------------------------- */
	// /* You usually do these for many times in the application life-cycle: */
	//
	// /* Get or create a template */
	// Template temp = cfg.getTemplate("posts.ftl");
	//
	// // execute the template
	// long tt = System.currentTimeMillis();
	// for (int i = 0; i < 100; i++) {
	// long t = System.currentTimeMillis();
	// // System.out.println("run templating: " + i);
	// /* Merge data-model with template */
	// Writer out = new StringWriter();
	// temp.process(m, out);
	// // System.out.println(out.toString());
	// // out.flush();
	// System.out.println(System.currentTimeMillis() - t);
	// // System.out.println(out.toString());
	// // Thread.sleep(5);
	// }
	// System.out.println("total time: " + (System.currentTimeMillis() - tt));
	//
	// }

	/**
	 * one od the idea to improve template performace is to compile the static
	 * content as UTF-8 encoded byte array instead of keep as string, which
	 * eventually will be converted to bytearray at runtime.
	 * 
	 * initial test show that 1000 iterations of convert a 40 chinese char
	 * string to byte array costs about 12 ms whereas direct array write costs
	 * only 2 ms, a 6 folds better performance. With ascii chars the gap is
	 * about 4 folds.
	 * 
	 * @throws IOException
	 * 
	 */
	@Test
	public void testByteArrayVsString() throws IOException {
		// String ss = "asdfasdfasdfasdfasdfasdfasdfasdf";
		String ss = "仰望着天空寻找一位失去的故友悄无声息的离开了也带上了命运";

		byte[] ba = ss.getBytes("UTF-8");

		long t1 = 0;
		long t2 = 0;

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		// warm up
		baos.write(ba);
		baos.write(ba);
		baos.write(ba);
		baos.write(ba);
		Charset UTF8 = Charset.forName("UTF-8");
		for (int i = 0; i < 1000; i++) {
			{
				baos.reset();
				long t0 = System.nanoTime();
				baos.write(ba);
				t1 += System.nanoTime() - t0;
			}
			{
				baos.reset();
				long t0 = System.nanoTime();
				// baos.write(ss.getBytes(UTF8));
				baos.write(ss.getBytes("UTF-8")); // seems to be faster than
				// using the Charset object?
				t2 += System.nanoTime() - t0;
			}
		}
		System.out.println("raw byte copy took nano: " + t1);
		System.out.println("String to bytes took nano: " + t2);

		List<byte[]> statics = new ArrayList<byte[]>();
		statics.add(new byte[] { 12, 23 });
		statics.add(new byte[] { 12, 23 });
	}

	@Test
	public void strings() {
		String ss = "仰望着天空寻找一位失去的故友悄无声息的离开了也带上了命运";
		Charset UTF8 = Charset.forName("UTF-8");
		CharsetEncoder enc = UTF8.newEncoder();
		// enc.

	}

	@Test
	public void bytes() throws UnsupportedEncodingException {
		String src = "你我\n他";
		byte[] bytes = src.getBytes("UTF-8");
		System.out.println(bytesSrcNotation(bytes));

	}

	private String bytesSrcNotation(byte[] ba) {
		StringBuffer sb = new StringBuffer();
		sb.append("new byte[]{");
		for (byte b : ba) {
			sb.append(b).append(',');
		}
		sb.append("}");
		return sb.toString();
	}

	// @Test public void testUrlMapper() {
	// BranTemplateBase.urlMapper = new DummyUrlMapper();
	// ByteArrayOutputStream baos = new ByteArrayOutputStream();
	// Actions ac = new Actions(baos);
	// Post p = new Post();
	// p.title = "hello";
	// ac.render(p);
	// System.out.println(baos.toString());
	// }

	// @Test public void testMessage() {
	// // BranTemplateBase.urlMapper = new DummyUrlMapper();
	// BranTemplateBase.messageProvider = new SimpleMessageProvider();
	//
	// ByteArrayOutputStream baos = new ByteArrayOutputStream();
	// Msg ac = new Msg(baos);
	// ac.render();
	// System.out.println(baos.toString());
	// }

	/**
	 * System.nanoTime is about 40 X slower than currentTimeMillies(), which
	 * takes about 40ns per call on my machine Windows
	 * 
	 * On Ubuntu, both are abot the same: 700 ns. The currentTimeMillies() is
	 * very slow compared with windows.
	 */
	@Test
	public void testSysMilliNano() {
		int COUNT = 1000000; // 1 M
		long start = System.nanoTime();
		long end = start;
		for (int i = 0; i < COUNT; i++) {
			end = System.nanoTime();
		}
		System.out.println("nanoTime:          " + (end - start) / COUNT + " ns");

		long dummy = 0;
		start = System.nanoTime();
		for (int i = 0; i < COUNT; i++) {
			dummy = System.currentTimeMillis();
		}
		end = System.nanoTime();
		System.out.println("currentTimeMillis: " + (end - start) / COUNT + " ns");
	}
}
