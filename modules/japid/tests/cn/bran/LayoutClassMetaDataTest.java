package cn.bran;

import static org.junit.Assert.*;

import org.junit.Test;

import cn.bran.japid.classmeta.LayoutClassMetaData;


public class LayoutClassMetaDataTest {
	@Test
	public void testPrintout() {
		LayoutClassMetaData icm = new LayoutClassMetaData();
		icm.setClassName("Layout");
		icm.packageName = "tag";
		icm.body = "p(\"something\");";
		icm.get("title");
		icm.get("footer");
		
		System.out.println(icm.toString());
	}
	
	@Test
	public void testPartialImport() {
		LayoutClassMetaData icm = new LayoutClassMetaData();
		icm.packageName = "my.pack";
		String l = icm.expandPartialImport("import 	.tags.Tag;");
		assertEquals("import " + icm.packageName + ".tags.Tag;", l);
		l = icm.expandPartialImport("import .tags.*;");
		assertEquals("import " + icm.packageName + ".tags.*;", l);
	}
}
