package cn.bran;

import org.junit.Test;

import cn.bran.japid.classmeta.AbstractTemplateClassMetaData;
import cn.bran.japid.classmeta.TemplateClassMetaData;
import cn.bran.japid.compiler.Tag;


public class TemplateClassMetaDataTest {

	@Test
	public void testToString() {
		TemplateClassMetaData m = new TemplateClassMetaData();
		m.setHasActionInvocation();
		m.packageName = "tag";
		m.setClassName("Child_html");
		m.superClass = "Layout_html";
		m.renderArgs = "String blogTitle, Post frontPost";
		m.addSetTag("title", "pln(\"the title  is \"); p(blogTitle);", new Tag.TagSet());
		m.addSetTag("footer", "\tpln(\"My Footer\")", new Tag.TagSet());
		m.addCallTagBodyInnerClass("Display", 1, "String title, String hi", "p(\"The real title is: \"); p(title);");
		m.body = "pln();\n\t if (frontPost != null) { new tag.Display().render(frontPost, \"home\", new Display1()); } \n pln(\"<p>cool</p>	  \");";
		System.out.println(m.toString());
	}
	


}
