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

package cn.bran.japid.tags;

import cn.bran.japid.template.JapidTemplateBaseWithoutPlay;

/**
 * a parent for hand-written tags that don't have a layout, which is the most
 * usually case for tags. Basically all the logic is directly contained in the
 * render(...) method
 * 
 * Comparing with Play's FastTag, modeling tag as class rather than as a method
 * in the FastTag offer benefits:
 * 
 * 1. can offer polymorphic implementations 
 * 2. can offer different signatures using the tag 
 * 3. easily searchable for better organization. 
 * 4. using package as natural scoping
 * 
 * @author Bing Ran<bing_ran@hotmail.com>
 * 
 */
public abstract class SimpleTag extends JapidTemplateBaseWithoutPlay {

	public SimpleTag(StringBuilder out2) {
		super(out2);
	}

	/**
	 * a no op implementation
	 */
	@Override
	protected void doLayout() {
	}

}