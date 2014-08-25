/**
 * Copyright 2010 Bing Ran<bing_ran@hotmail.com>
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at http://www.apache.org/licenses/LICENSE-2.0.
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package cn.bran.japid.classmeta;

import japa.parser.ast.body.Parameter;

import java.util.List;

import cn.bran.japid.compiler.JavaSyntaxTool;
import cn.bran.japid.tags.Each;


/**
 * used to wrapped the body of an invocation of a user defined tag based on tag template file
 * 
 * @author bran
 *
 */
public class InnerClassMeta {
	private static final String EXTRA_LOOP_ATTRS = ", final int _size, final int _index, final boolean _isOdd, final String _parity, final boolean _isFirst, final boolean _isLast";
	String tagName;
	// the sequence of the same tag called in a single template
	int counter;
	// like in a function call
	String renderParams;
	String renderBody;
//	private String interfaceName;
	public InnerClassMeta(String tagName, int counter, String callbackArgs, String renderBody) {
		this.tagName = tagName.replace('/', '.');
		this.counter = counter;
		this.renderParams = JavaSyntaxTool.boxPrimitiveTypesInParams(callbackArgs);
		this.renderBody = renderBody;
	}
//
//	/**
//	 * something like this: 	
//	 * <pre>
//	 * class Display1_Body implements DoBodyInterface{
//			void render(String title) {
//				pln ("The real title is: ", title);
//			}
//	 *	}
//	</pre>
//	 * @deprecated this method is the old way of declaring inner class. Now use the getAnonymous() in inline fashion. 
//	 */
//	@Override
//	public String toString() {
//		ExprParser ep = new ExprParser(this.renderArgs);
//		List<String> argTokens = ep.split();
//		// something String a Date b
//		assert(argTokens.size() % 2 == 0);
//		
//		String[] argTypes = new String[argTokens.size() /2];
//		
//		String classParams = "";
//		for (int i = 0; i < argTypes.length; i++) {
//			classParams += ", " + argTokens.get(i * 2);
//		}
//		
//		if (Each.class.getSimpleName().equals(tagName)) {
//			// append extra argument to the render method
//			renderArgs += EXTRA_LOOP_ATTRS;
//		}
//		
//		// remove the leading ,
//		
//		
//		if (classParams.startsWith(","))
//				classParams = "<" + classParams.substring(1) + ">";
//
//		StringBuilder sb = new StringBuilder();
//		line(sb, "class " + getVarRoot() + counter + "DoBody implements " + tagName + ".DoBody" +  classParams + "{");
//		line(sb, "\tpublic void render(" + renderArgs  + ") {");
//		line(sb, "\t\t" + renderBody);
//		line(sb, "\t}");
//		line(sb, "}");
//		
//		// bodyclass instance
//		String bodyClassName = getVarRoot() + counter +  "DoBody"; 
//		String bodyField = "private " + bodyClassName +" _" + bodyClassName + 
//			" = new " + bodyClassName + "();";
//		line(sb, "\t" + bodyField);
//
//		return sb.toString();
//	}
	
	private void line (StringBuilder sb, String line) {
		sb.append(line + "\n");
	}
	
	public String getVarRoot() {
		return tagName.replace('.', '_').replace('/', '_');
	}

	/**
	 * get something like this 
	 * <pre>
	 * 		new Display.DoBody<String>() {
	 *			public void render(String title) {
	 *				p(" The real title is: ");
	 *				p(title);
	 *			}
	 *		});
	 * </pre>
	 * @return
	 */
	public String getAnonymous(String lineMarker) {
		List<Parameter> params = JavaSyntaxTool.parseParams(this.renderParams);
		
		String[] argTypes = new String[params.size()];
		
		String generics = "";
		for (int i = 0; i < argTypes.length; i++) {
			generics += ", " + params.get(i).getType();
		}
		if (generics.startsWith(","))
			generics = "<" + generics.substring(1).trim() + ">";
		
		if (Each.class.getSimpleName().equalsIgnoreCase(tagName)) {
			// append extra argument to the render method
			tagName = Each.class.getSimpleName();
			renderParams += EXTRA_LOOP_ATTRS;
		}
		
		String paramList = renderParams;
		String renderArgsWithFinal = JavaSyntaxTool.addFinalToAllParams(paramList);
		
		StringBuilder sb = new StringBuilder();
		line(sb, "new " + tagName + ".DoBody" +  generics + "(){ " + lineMarker);
		line(sb, "public void render(" + renderArgsWithFinal  + ") { " + lineMarker);
		line(sb, renderBody);
		line(sb, "}");
		String bufferString = "\r\n" + 
				"StringBuilder oriBuffer;\r\n" + 
				"@Override\r\n" + 
				"public void setBuffer(StringBuilder sb) {\r\n" + 
				"	oriBuffer = getOut();\r\n" + 
				"	setOut(sb);\r\n" + 
				"}\r\n" + 
				"\r\n" + 
				"@Override\r\n" + 
				"public void resetBuffer() {\r\n" + 
				"	setOut(oriBuffer);\r\n" + 
				"}\r\n";
		line(sb, bufferString);
		line(sb, "}");
		
		return sb.toString();
	}
}
