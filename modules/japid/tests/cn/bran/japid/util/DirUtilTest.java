package cn.bran.japid.util;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

public class DirUtilTest {

	private static final String root = ".\\japidviews\\_tags\\taggy";

	@Test
	public void testMapSrcToJava() {
		String f = root + ".html";
		String j = DirUtil.mapSrcToJava(f);
		assertEquals(root + ".java", j);
	}

	@Test
	public void testOrphan() {
		File src = new File("tests/testdir");
		Set<File> orphan = DirUtil.findOrphanJava(src, src);
		assertEquals(1, orphan.size());
		File next = orphan.iterator().next();
		System.out.println(next);
		assertTrue(next.getName().endsWith("C.java"));
	}

	@Test
	public void testScanning() {
		String[] exts = new String[] { "java", "html" };
		File src = new File("tests/testdir");
		String[] fs = DirUtil.getAllFileNames(src, exts);
		for (String s : fs) {
			System.out.println(s);
		}
	}

	@Test
	public void testAllFiles() {
		String[] exts = new String[] { ".java", ".html" };
		File src = new File("tests/testdir");
		Set<File> fs = new HashSet<File>();
		fs = DirUtil.getAllFiles(src, exts, fs);
		assertEquals(10, fs.size());
		for (File f : fs) {
			String name = f.getPath();
			assertTrue(name.endsWith(".java") || name.endsWith(".html"));
			System.out.println(name);
		}
	}
	
//	@Test // XXX this test setup is fragile. fix it later
//	public void testChangedHtml() throws IOException {
//		File src = new File("tests/testdir");
//		File newer = new File ("tests/testdir/A.html");
//		DirUtil.touch(newer);
//		List<File> fs = DirUtil.findChangedSrcFiles(src);
//		for (File f: fs) {
//			System.out.println(f.getPath());
//		}
//		assertEquals(4, fs.size());
//		DirUtil.touch(new File("tests/testdir/A.java"));
//		fs = DirUtil.findChangedSrcFiles(src);
//		for (File f: fs) {
//			System.out.println(f.getPath());
//		}
//		assertEquals(2, fs.size());
//
//		File bad = new File ("tests/testdir/#A.html");
//		DirUtil.touch(bad);
//		fs = DirUtil.findChangedSrcFiles(src);
//		assertEquals(2, fs.size());
//		bad.delete();
//
//		bad = new File ("tests/testdir/.A.html");
//		DirUtil.touch(bad);
//		fs = DirUtil.findChangedSrcFiles(src);
//		assertEquals(2, fs.size());
//		bad.delete();
//
//		File good = new File ("tests/testdir/A.B.html");
//		DirUtil.touch(good);
//		fs = DirUtil.findChangedSrcFiles(src);
//		assertEquals(3, fs.size());
//		good.delete();
//	}
	
	@Test
	public void testJavaToSrc() {
		String javasrc = "my/Action_xml.java";
		String res = DirUtil.mapJavaToSrc(javasrc);
		assertEquals("my/Action.xml", res);
		
		javasrc = "my/Action_json.java";
		res = DirUtil.mapJavaToSrc(javasrc);
		assertEquals("my/Action.json", res);
	}
	
	@Test
	public void testHasTemplatesInDir() {
//		String root = "tests";
		boolean has = false;
		has  = DirUtil.containsTemplatesInDir("tests/testdir");
		assertTrue(has);
		has  = DirUtil.containsTemplatesInDir("tests/testdir/empty");
		assertFalse(has);
		
	}

	@Test
	public void testHasTemplates() {
		String root = "tests/testroot";
		boolean has = false;
		has  = DirUtil.hasTags(root);
		assertTrue(has);
		has  = DirUtil.hasJavaTags(root);
		assertFalse(has);
		has  = DirUtil.hasLayouts(root);
		assertFalse(has);
	}
	
	@Test
	public void testMapJavaFileToSrcFile() {
		File j = new File("c:\\tmp\\abc.java");
		File s = DirUtil.mapJavatoSrc(j);
		assertEquals("c:\\tmp\\abc.html", s.getPath());

		j = new File("/tmp/a/abc_xml.java");
		s = DirUtil.mapJavatoSrc(j);
		assertEquals("/tmp/a/abc.xml", s.getPath());
	}
}
