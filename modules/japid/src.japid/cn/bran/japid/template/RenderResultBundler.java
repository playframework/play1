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
package cn.bran.japid.template;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import cn.bran.japid.util.StringBundler;

/**
 * to wrap the result of Japid template rendering. 
 * 
 * Objects of this class can be cached.
 * 
 * @author Bing Ran<bing_ran@hotmail.com>
 * 
 */
public class RenderResultBundler implements Serializable {
	private StringBundler content;
	long renderTime; // in ms, for recording the time to render.
	private Map<String, String> headers = new HashMap<String, String>();

	public RenderResultBundler(Map<String, String> headers , StringBundler content, long renderTime) {
		super();
		this.content = content;
		this.renderTime = renderTime;
		this.headers = headers;
	}

	/**
	 * get the interpolated content in StringBuilder. In case of nested action
	 * calls, all the content tiles are generated and interpolated
	 * 
	 * @return the fully interpolated content
	 */
	public StringBundler getContent() {
		return content;
	}

	public long getRenderTime() {
		return this.renderTime;
	}

//	public void setHeaders(Map<String, String> headers) {
//		this.headers = headers;
//	}

	@Override
	public String toString() {
		if (content != null) {
			return content.toString();
		}
		else {
			return "RenderResult: null";
		}
	}

	public Map<String, String> getHeaders() {
		return this.headers;
	}
}
