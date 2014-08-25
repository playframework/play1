/**
 * Copyright 2010 Bing Ran<bing_ran@hotmail.com> 
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not 
 * use this file except in compliance with the License. You may obtain a copy 
 * of the License at http://www.apache.org/licenses/LICENSE-2.0.
 * 
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License. 
 */
package cn.bran.japid.compiler;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cn.bran.japid.classmeta.AbstractTemplateClassMetaData;
import cn.bran.japid.classmeta.TemplateClassMetaData;
import cn.bran.japid.compiler.Tag.TagSet;
import cn.bran.japid.template.ActionRunner;
import cn.bran.japid.template.RenderResult;

/**
 * specifically for callable templates
 * 
 * tags does not extends (why not?) some decisions: flow control is not treated
 * as tags, assume ~{ if (xxx) { }~ ~{}}~ #{tags} with body will be compiled to
 * inner classes /anonymous class#{tags sdfs df /} don't create inner class
 * 
 * -- extends can take parnt.html or a java class directly.
 * @author Bing Ran<bing_ran@hotmail.com>
 * @author Play! framework original authors
 */

public class JapidTemplateCompiler extends JapidAbstractCompiler {
	private static final String DO_BODY = "doBody";
	
	// StringBuilder mainRenderBodySource = new StringBuilder();
	TemplateClassMetaData tcmd = new TemplateClassMetaData();

	@Override
	protected void startTag(Tag tag) {
		if (tag.tagName.equals(DO_BODY)) {
			String[] argPartsAndVar = JavaSyntaxTool.breakArgParts(tag.args);
			if (argPartsAndVar.length == 1){
				tcmd.doBody(tag.args);
				print("if (body != null){ body.setBuffer(getOut()); body.render(" + tag.args + "); body.resetBuffer();}");
			}
			else {
				String args = argPartsAndVar[0];
				tcmd.doBody(args);
				String localVar = argPartsAndVar[1];
				print("String " + localVar + " = renderBody(" + args + ");");
			}
			// print to the root space before move one stack up
		} else if ("set".equals(tag.tagName)) {
			if (SET_ARG_PATTERN_ONELINER.matcher(tag.args).matches()) {
				if (tag.hasBody) {
					throw new JapidCompilationException(template, parser.getLineNumber(), "set tag cannot have value both in tag and in body: " + tag + " " + tag.args);
				} else {
					int i = 0;
					
					if (SET_ARG_PATTERN_ONELINER_COLON.matcher(tag.args).matches()) {
						i = tag.args.indexOf(":");
					}
					else {
						i = tag.args.indexOf("=");
					}

					String key = tag.args.substring(0, i).trim().replace("\"", "").replace("'", "");
					String value = tag.args.substring(i + 1);
					if (JavaSyntaxTool.isValidExpr(value))
						this.tcmd.addSetTag(key, "p(" + value + ");", (TagSet) tag);
					else
						throw new JapidCompilationException(template, parser.getLineNumber(), "The value part in the set tag is not a valid expression: " + value + ". " + "The grammar is: set var_name = java_expression.");
				}
			}
			else {
				Matcher matcher = setPattern.matcher(tag.args);
				if (matcher.matches()) {
					tag.hasBody = false;
					String key = matcher.group(1);
					String value = matcher.group(2);
					this.tcmd.addSetTag(key, "p(" + value + ");", (TagSet) tag);
				}
			}
		} else if (tag.tagName.equals("def")) {
			def(tag);
		} else {
			regularTagInvoke(tag);
		}
		
		pushToStack(tag);
		markLine();
		println();
		skipLineBreak = true;

	}
	
	static Pattern setPattern = Pattern.compile("(\\w+)\\s+(.*)");
	static Pattern SET_ARG_PATTERN_ONELINER_COLON = Pattern.compile("\\w+\\s*:.*");
	static Pattern SET_ARG_PATTERN_ONELINER_EQUAL = Pattern.compile("\\w+\\s*=.*");
	static Pattern SET_ARG_PATTERN_ONELINER = Pattern.compile("\\w+\\s*[:=].*");
	
	@Override
	protected AbstractTemplateClassMetaData getTemplateClassMetaData() {
		return tcmd;
	}
	
	@Override
	protected void scriptline(String token) {
		String line = token;//.trim(); don't trim `a `t is sensitive to the space
		if (JapidAbstractCompiler.startsWithIgnoreSpace(line.trim(), DO_BODY) || line.trim().equals(DO_BODY)) {
			String args = line.trim().substring(DO_BODY.length()).trim();
			String[] argPartsAndVar = JavaSyntaxTool.breakArgParts(args);
			if (argPartsAndVar.length == 1){
				tcmd.doBody(args);
				printLine("if (body != null){ body.setBuffer(getOut()); body.render(" + args + "); body.resetBuffer();}");
			}
			else {
				args = argPartsAndVar[0];
				tcmd.doBody(args);
				String localVar = argPartsAndVar[1];
				printLine("String " + localVar + " = renderBody(" + args + ");");
			}
			
			skipLineBreak = true;
		}
		else {
			super.scriptline(token);
		}
	}

	@Override
	void endSet(TagSet tag) {
		if (tag.hasBody) {
			String key = tag.args.replace("\"", "").replace("'", "");
			this.tcmd.addSetTag(key, tag.getBodyText(), tag);
		}
	}

}