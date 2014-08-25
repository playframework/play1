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

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.lang.String;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * to wrap the result of Japid template rendering. 
 * 
 * Objects of this class can be cached.
 * 
 * @author Bing Ran<bing_ran@hotmail.com>
 * 
 */
public class RenderResult implements Externalizable {
	private static final String _NULL = "_null_";
	private StringBuilder content; // bran can this
	long renderTime; // in us, (micro-second) for recording the time to render.
	private Map<String, String> headers = new HashMap<String, String>();
	
	public RenderResult(Map<String, String> headers , StringBuilder content, long renderTime) {
		this.content = content;
		this.renderTime = renderTime;
		this.headers = headers;
	}

	public RenderResult() {
	}
	


	/**
	 * get the interpolated content in StringBuilder. In case of nested action
	 * calls, all the content tiles are generated and interpolated
	 * 
	 * @return the fully interpolated content
	 */
	public StringBuilder getContent() {
		return content;
	}

	/**
	 * get the text result
	 * @return
	 */
	public String getText() {
		return content.toString();
	}
	
	public long getRenderTime() {
		return this.renderTime;
	}

//	public void setHeaders(Map<String, String> headers) {
//		this.headers = headers;
//	}

	@Override
	public String toString() {
		return getContent().toString();
	}

	/**
	 * print headers and body, seprated by a blank line
	 * 
	 * @author Bing Ran (bing.ran@gmail.com)
	 * @return
	 */
	public String toStringWithHeaders() {
		// print the headers:
		StringBuffer sb = new StringBuffer();
		if (headers != null) {
			for (String it : headers.keySet()) {
				sb.append(it).append(": ").append(headers.get(it)).append("\n");
			}
		}
		if (sb.toString().endsWith("\n")) {
			sb.append("\n"); 
		}
		
		sb.append(toString());
		return sb.toString();
	}

	public Map<String, String> getHeaders() {
		return this.headers;
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		String contentString = content == null? _NULL : content.toString();
		out.writeUTF(contentString);
		out.writeLong(renderTime);
		out.writeObject(headers);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		String contentString = in.readUTF();
		if (_NULL.equals(contentString)) {
			this.content = null;
		}
		else {
			this.content = new StringBuilder(contentString);
		}
		renderTime = in.readLong();
		headers = (Map<String, String>) in.readObject();
	}

	public String getContentType() {
		return getHeaders().get("Content-Type");
	}
}
