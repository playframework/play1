package cn.bran;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.junit.Test;

import cn.bran.japid.classmeta.AbstractTemplateClassMetaData;
import cn.bran.japid.compiler.JapidTemplateTransformer;



public class TemplateTranslatorTest {


	@Test public void testFilePathConversion() throws IOException {
		String ch = "child";
		File f = new File(ch);
		File fc = new File(".");
		String rela = JapidTemplateTransformer.getRelativePath(f, fc);
		assertEquals(ch, rela);
	}
}
