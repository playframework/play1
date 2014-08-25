package cn.bran.play;

import static org.junit.Assert.*;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.junit.Test;

import play.Play;

import cn.bran.japid.util.DirUtil;

public class JapidCommandsTest {

	private static final String ROOT = "JapidSample/app";

	@Test
	public void testMkdir() throws IOException {
		List<File> mkdir = DirUtil.mkdir("tests/testmkdir");
		for (File f : mkdir) {
			System.out.println("verify existence: " + f.getPath());
			assertTrue(f.exists());
		}
	}

	@Test
	public void testDelete() {
		String root = ROOT;
		String pathname = root + File.separatorChar + DirUtil.JAPIDVIEWS_ROOT;
		JapidCommands.delAllGeneratedJava(pathname);
		String[] fs = DirUtil.getAllFileNames(new File(pathname), new String[]{".java"});
		for (String s : fs) {
			if (!s.contains("_javatags"))
				fail("java templates were not cleaned: " + s);
		}
	}
	
	@Test
	public void testGen() throws IOException {
		String root = ROOT;
		Play.applicationPath = new File("JapidSample");
		JapidCommands.gen(root);
	}
	
	@Test
	public void testregen() throws IOException {
		String root = ROOT;
		Play.applicationPath = new File("JapidSample");
		JapidCommands.regen(root);
	}
	
	
}
