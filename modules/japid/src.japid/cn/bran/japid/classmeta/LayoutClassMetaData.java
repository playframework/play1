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
package cn.bran.japid.classmeta;

import japa.parser.ast.body.Parameter;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cn.bran.japid.compiler.JavaSyntaxTool;

public class LayoutClassMetaData extends AbstractTemplateClassMetaData {

	{
		setAbstract(true);
	}

	Set<String> getterMethods = new HashSet<String>();

	/**
	 * map the #{get} tag
	 * 
	 * @param string
	 */
	public void get(String string) {
		this.getterMethods.add(string);
	}

	/**
	 * 
	 */
	protected void childLayout() {
		p("\n\tprotected abstract void doLayout();\n");
	}

	/**
	 * #{get "block name" /} was creating abstract. Now changed to a no
	 * operation method stub so subclass can selectively override the getters in
	 * the layout
	 */
	protected void getterSetter() {
		pln();
		for (String key : getterMethods) {
			// p("\t protected abstract void " + key + "();\n");
			p("\t protected void " + key + "() {};\n");
		}
	}

	/**
	 * 
	 */
	protected void layoutMethod() {
		if (renderArgs != null) {
			// create fields for the render args and create a render method to
			List<Parameter> params = JavaSyntaxTool.parseParams(this.renderArgs);

			String renderArgsWithoutAnnos = "";
			for (Parameter p: params) {
				renderArgsWithoutAnnos += p.getType() + " " + p.getId() + ",";
			}
			if (renderArgsWithoutAnnos.endsWith(",")){
				renderArgsWithoutAnnos = renderArgsWithoutAnnos.substring(0, renderArgsWithoutAnnos.length() - 1);
			}

			
			for (Parameter p : params) {
//				pln(TAB + "private " + p.getType() + " " + p.getId() + ";");
				addField(p);
			}

			pln("\t public void layout(" + renderArgsWithoutAnnos + ") {");
			// assign the params to fields
			for (Parameter p : params) {
				pln("\t\tthis." + p.getId() + " = " + p.getId() + ";");
			}
		}
		else {
			pln("\t@Override public void layout() {");
		}

		restOfBody();
	}

	@Override
	void renderMethod() {
		// no such method in layout
	}

	@Override
	public void merge(AbstractTemplateClassMetaData a) {
		super.merge(a);
		if (a instanceof LayoutClassMetaData) {
			LayoutClassMetaData b = (LayoutClassMetaData) a;
			this.getterMethods.addAll(b.getterMethods);
		}
		else {
			throw new RuntimeException("cannot merge metadata from another type to LayoutClassMetaData: " + a.className);
		}
	}
}
