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

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import cn.bran.japid.compiler.JavaSyntaxTool;
import cn.bran.japid.compiler.NamedArgRuntime;
import cn.bran.japid.compiler.Tag;
import cn.bran.japid.compiler.Tag.TagSet;

/**
 * the class meta data for templates that are directly renderable, meaning this
 * is not for Layout template nor for tag template
 * 
 * special:
 * <ul>
 * <li>no dobody</li>
 * <li>can #{extends 'layout.html'}</li>
 * <li>take script params, like in tag template</li>
 * <li>the whole body is wrapped as body(), to be called from layout class</li>
 * <li>support #{set}</li>
 * </ul>
 * ,
 * 
 * 
 * 
 * @author Bing Ran<bing_ran@hotmail.com>
 * 
 */
public class TemplateClassMetaData extends AbstractTemplateClassMetaData {

	/**
	 * 
	 */
	private static final String COMMA = ", ";
	// there are the "#{set var:val /}
	// <methName, methodBody
	Map<String, String> setMethods = new HashMap<String, String>();
	Map<String, TagSet> setTags = new HashMap<String, TagSet>();

	// Experiment: allow any template to be callable as a tag and can handle
	// doBody
	// null: no doBody, "": there is doBody but no parameters passed back
	private String doBodyArgsString;
	private char[] doBodyGenericTypeParams = new char[] { 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J' };

	//
	public void addSetTag(String setMethodName, String methodBody, TagSet tag) {
		setMethods.put(setMethodName, methodBody);
		setTags.put(setMethodName, tag);
	}

	// for the doBody in the tag template
	public void addDoBodyInterface(String bodyArgsString) {
		this.doBodyArgsString = bodyArgsString;
	}

	public void doBody(String tagArgs) {
		tagArgs = tagArgs == null ? "" : tagArgs.trim();
		this.addDoBodyInterface(tagArgs);
	}

	// public void addDefTag(String key, String string) {
	// // TODO Auto-generated method stub
	// }

	/**
	 * 
	 */
	@Override
	protected void getterSetter() {
		// `set title = "something"
		pln();
		for (Entry<String, String> en : setMethods.entrySet()) {
			String meth = en.getKey();
			String setBody = en.getValue();
			pln("\t@Override protected void " + meth + "() {");
			// local tag defs
			// TagSet set = setTags.get(meth);
			// for (Tag t: set.tags) {
			// declareTagInstance(t);
			// }
			pln("\t\t" + setBody + ";");
			pln("\t}");
		}
	}

	/**
	 * the main part of the render logic
	 */
	protected void layoutMethod() {
		// doLayout body
		pln(TAB + "@Override protected void doLayout() {");

		restOfBody();
	}

	/**
	 * the entry point of the template: render(...). Concrete views have this
	 * method while the layouts do not.
	 */
	protected void renderMethod() {
		String resultType = useWithPlay ? RENDER_RESULT : "String";

		String paramNameArray = "";
		String paramTypeArray = "";
		String paramDefaultsArray = "";
		String currentClassFQN = (this.packageName == null ? "" : this.packageName + ".") + this.className;
		List<Parameter> params = JavaSyntaxTool.parseParams(this.renderArgs);
		String renderArgsWithoutAnnos = "";

		// / named param stuff
		for (Parameter p : params) {
			paramNameArray += "\"" + p.getId() + "\", ";
			paramTypeArray += "\"" + p.getType() + "\", ";
			String defa = JavaSyntaxTool.getDefault(p);
			paramDefaultsArray += defa + ",";
			renderArgsWithoutAnnos += p.getType() + " " + p.getId() + ",";
		}
		if (renderArgsWithoutAnnos.endsWith(",")) {
			renderArgsWithoutAnnos = renderArgsWithoutAnnos.substring(0, renderArgsWithoutAnnos.length() - 1);
		}

		String nameParamCode = String.format(NAMED_PARAM_CODE, paramNameArray, paramTypeArray, paramDefaultsArray, currentClassFQN);
		pln(nameParamCode);

		if (doBodyArgsString != null)
			pln("	{ setHasDoBody(); }");

		if (renderArgs != null) {

			for (Parameter p : params) {
				addField(p);
			}

			// set the render(xxx)
			if (doBodyArgsString != null) {
				pln(String.format(NAMED_PARAM_WITH_BODY, getLineMarker()));
				// the template can be called with a callback body
				// the field
				pln(TAB + "private DoBody body;");
				doBodyInterface();

				// now the render(...)
				pln("\tpublic " + resultType + " render(" + renderArgsWithoutAnnos + ", DoBody body) {");
				pln("\t\t" + "this.body = body;");
				// assign the params to fields
				for (Parameter p : params) {
					pln("\t\tthis." + p.getId() + " = " + p.getId() + ";");
				}
				restOfRenderBody(resultType);
			}

			// a version without the body part to allow optional body
			pln("\tpublic " + resultType + " render(" + renderArgsWithoutAnnos + ") {");
			// assign the params to fields
			for (Parameter p : params) {
				pln("\t\tthis." + p.getId() + " = " + p.getId() + ";");
			}
			restOfRenderBody(resultType);
		} else {
			if (doBodyArgsString != null) {
				pln(String.format(NAMED_PARAM_WITH_BODY, getLineMarker()));
				// the field
				pln(TAB + "DoBody body;");
				doBodyInterface();

				// now the render(...)
				pln("\tpublic " + resultType + " render(DoBody body) {");
				pln("\t\t" + "this.body = body;");
				restOfRenderBody(resultType);
			}
			// the parameter-less render()
			pln("\tpublic " + resultType + " render() {");
			restOfRenderBody(resultType);
		}
		// the static apply method
		String args = "";
		for (Parameter p : params) {
			args += p.getId() + COMMA;
		}
		if (args.endsWith(COMMA)) {
			args = args.substring(0, args.lastIndexOf(COMMA));
		}
		String applyMethod = isAbstract? 
				String.format(APPLY_METHOD_ABSTRACT, resultType, renderArgsWithoutAnnos, this.className, args) : 
					String.format(APPLY_METHOD, resultType, renderArgsWithoutAnnos, this.className, args);
		pln("\n" + applyMethod);

	}

	static final String APPLY_METHOD = 
			"	public static %s apply(%s) {\n" + 
			"		return new %s().render(%s);\n" + 
			"	}\n" ;
	
	static final String APPLY_METHOD_ABSTRACT = 
			"	public static %s apply(%s) {\n" + 
					"		throw new RuntimeException(\"Cannot run an Japid template annotated as abstract.\");\n" + 
					"	}\n" ;
	

	private void restOfRenderBody(String resultType) {
		if (stopWatch)
			pln("\t\tsetStopwatchOn();");

//		pln("\t\tstartRendering(); ");
		pln("\t\ttry {super.layout(" + superClassRenderArgs +  ");} catch (RuntimeException __e) { super.handleException(__e);} " + getLineMarker());

		if (useWithPlay) {
			pln("\t\treturn getRenderResult();");
		} else {
			pln("\t\treturn getRenderResult().toString();");
		}
		pln("\t}");
	}

	private void doBodyInterface() {
		// let do the doDody callback interface
		// doBody interface:
		if (doBodyArgsString != null) {
			List<String> args = JavaSyntaxTool.parseArgs(doBodyArgsString);

			String genericTypeList = "";
			String renderArgList = "";
			int i = 0;
			for (String arg : args) {
				char c = doBodyGenericTypeParams[i++];
				genericTypeList += "," + c;
				renderArgList += "," + c + " " + Character.toLowerCase(c);
			}
			if (genericTypeList.startsWith(",")) {
				// remove the first comma
				genericTypeList = "<" + genericTypeList.substring(1) + ">";
				renderArgList = renderArgList.substring(1);
			}
			pln("public static interface DoBody", genericTypeList, " {");
			pln("		void render(" + renderArgList + ");");
			pln("		void setBuffer(StringBuilder sb);\n" + "		void resetBuffer();\n" + "}");

			// add a convenient method to get the render result from the doBody object
			String renderArgs = renderArgList.replaceAll("[A-Z]", "");
			pln(genericTypeList, " String renderBody(" + renderArgList + ") {\n" + 
					"		StringBuilder sb = new StringBuilder();\n" + 
					"		if (body != null){\n" + 
					"			body.setBuffer(sb);\n" + 
					"			body.render(" + renderArgs + ");\n" + 
					"			body.resetBuffer();\n" + 
					"		}\n" + 
					"		return sb.toString();\n" + 
					"	}");
			

		}
	}

	@Override
	void childLayout() {
		// concrete views do not have this
	}

	protected static final String NAMED_PARAM_CODE = "" +
			"/* based on https://github.com/branaway/Japid/issues/12\n" + 
			" */\n" +
			"\tpublic static final String[] argNames = new String[] {/* args of the template*/%s };\n" + 
			"\tpublic static final String[] argTypes = new String[] {/* arg types of the template*/%s };\n" + 
			"\tpublic static final Object[] argDefaults= new Object[] {%s };\n"  + 
			"\tpublic static java.lang.reflect.Method renderMethod = getRenderMethod(%s.class);\n\n" + 
			"\t{\n" + 
			"\t\tsetRenderMethod(renderMethod);\n" + 
			"\t\tsetArgNames(argNames);\n" + 
			"\t\tsetArgTypes(argTypes);\n" + 
			"\t\tsetArgDefaults(argDefaults);\n" +
			"\t\tsetSourceTemplate(sourceTemplate);\n" + 
			"\t}\n" +
			"" + 
			"////// end of named args stuff\n";

	protected static final String NAMED_PARAM_WITH_BODY = "public cn.bran.japid.template.RenderResult render(DoBody body, cn.bran.japid.compiler.NamedArgRuntime... named) {\n"
			+ "    Object[] args = buildArgs(named, body);\n"
			+ "    try {return runRenderer(args);} catch(RuntimeException e) {handleException(e); throw e;} %s\n"
			+ "}\n";

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * cn.bran.japid.classmeta.AbstractTemplateClassMetaData#merge(cn.bran.japid
	 * .classmeta.AbstractTemplateClassMetaData)
	 */
	@Override
	public void merge(AbstractTemplateClassMetaData a) {
		super.merge(a);
		if (a instanceof TemplateClassMetaData) {
			TemplateClassMetaData b = (TemplateClassMetaData) a;
			this.setMethods.putAll(b.setMethods);
			this.setTags.putAll(b.setTags);
		}
		else {
			throw new RuntimeException("cannot merge metadata from another type to AbstractTemplateClassMetaData: " + a.className);
		}

	}

}
