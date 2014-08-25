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

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

import cn.bran.japid.util.WebUtils;

/**
 * a java based template that dump content to a stream.
 * 
 * @author bran
 * 
 */
public abstract class JapidTemplateBaseStreaming {
	private static final String UTF_8 = "UTF-8";

//	private PrintWriter out;
	private OutputStream out;
	protected OutputStream getOut() {
		return out;
	}
	
	public JapidTemplateBaseStreaming(OutputStream out2) {
		if (out2 == null)
			throw new RuntimeException("JapidTemplateBaseStreaming do not take null OutputStream.");
		this.out = out2;
	}
//
//	private BranTemplateBase() {
//	}
	
	
	// call this if run in PlayContainer
	public void runtimeInit() {
		// scope = Scope.
	}

	// don't use it since it will lead to new instance of stringencoder
	Charset UTF8 = Charset.forName("UTF-8");


	final protected void p(byte[] ba) {
		try {
			out.write(ba);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	final protected void p(String s) {
		try {
			writeString(s);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	final protected void pln(String s) {
		try {
			writeString(s);
			out.write('\n');
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}


	/**
	 * @param s
	 * @throws IOException
	 * @throws UnsupportedEncodingException
	 */
	private void writeString(String s) throws IOException, UnsupportedEncodingException {
//		ByteBuffer bb = StringUtils.encodeUTF8(s);
//		out.write(bb.array(), 0, bb.position());
		// ok my code is slower in large trunk of data
		if (s != null)
			out.write(s.getBytes("UTF-8"));
	}
	
	final protected void pln(byte[] ba) {
		try {
			out.write(ba);
			out.write('\n');
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	final protected void p(Object... ss) {
		for (Object s : ss) {
			if (s != null) {
				writeObject(s);
//				out.append(s);
			}
		}
	}
	/**
	 * @param s
	 */
	private void writeObject(Object s) {
		try {
			if (s instanceof byte[]) {
				out.write((byte[])s);
			}
			else {
				writeString(s.toString());
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	final protected void pln(Object... ss) {
		for (Object s : ss) {
			if (s != null)
				writeObject(s);
		}
		pln();
	}

	final protected void pln() {
		try {
			out.write('\n');
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	protected void layout() {
		doLayout();
	}
	protected abstract void doLayout();
	
	public  boolean asBoolean(Object o) {
		return WebUtils.asBoolean(o);
	}

	static protected byte[] getBytes(String src) {
		if (src == null || src.length() == 0)
			return new byte[] {};
		
		try {
			return src.getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

}
