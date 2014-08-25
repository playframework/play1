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
package cn.bran.play;

import java.io.File;
import java.io.IOException;
import java.util.TreeMap;

import org.apache.commons.io.FileUtils;

import play.Play;
import play.Play.Mode;
import play.classloading.ApplicationClasses.ApplicationClass;
import play.exceptions.TemplateExecutionException;
import cn.bran.japid.classmeta.MimeTypeEnum;
import cn.bran.japid.template.JapidTemplateBaseWithoutPlay;

/**
 * a java based template using StringBuilder as the content buffer
 * 
 * @author bran
 * 
 */
public abstract class JapidTemplateBase extends JapidTemplateBaseWithoutPlay {
	public JapidTemplateBase(StringBuilder out) {
		super(out);
		initme();
	}

	/**
	 * @author Bing Ran (bing.ran@gmail.com)
	 */
	private void initme() {
		if (actionRunners == null) {
			actionRunners = new TreeMap<Integer, cn.bran.japid.template.ActionRunner>();
		}
	}

	public JapidTemplateBase(JapidTemplateBaseWithoutPlay caller) {
		super(caller);
		if (caller instanceof JapidTemplateBase){
			setActionRunners(((JapidTemplateBase)caller).getActionRunners());
		}
		initme();
	}

	/**
	 * to keep track of all the action invocations by #{invoke} tag
	 */
	protected TreeMap<Integer, cn.bran.japid.template.ActionRunner> actionRunners;// = new TreeMap<Integer, cn.bran.japid.template.ActionRunner>();

	public TreeMap<Integer, cn.bran.japid.template.ActionRunner> getActionRunners() {
		return actionRunners;
	}

	public JapidTemplateBaseWithoutPlay setActionRunners(
			TreeMap<Integer, cn.bran.japid.template.ActionRunner> actionRunners) {
		this.actionRunners = actionRunners;
		return this;
	}

	protected cn.bran.japid.template.RenderResult getRenderResult() {
		return new cn.bran.japid.template.RenderResultPartial(getHeaders(), getOut(), renderingTime, actionRunners, sourceTemplate);
	}

}
