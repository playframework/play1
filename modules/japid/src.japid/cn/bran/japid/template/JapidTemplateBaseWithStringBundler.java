package cn.bran.japid.template;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.TreeMap;

import cn.bran.japid.util.StringBundler;

public abstract class JapidTemplateBaseWithStringBundler {

	StringBundler out;
	protected Map<String, String> headers = new TreeMap<String, String>();
	{
		headers.put("Content-Type", "text/html; charset=utf-8");
	}

	/**
	 * to keep track of all the action invocations by #{invoke} tag
	 */
	protected TreeMap<Integer, cn.bran.japid.template.ActionRunner> actionRunners = new TreeMap<Integer, cn.bran.japid.template.ActionRunner>();

	public TreeMap<Integer, cn.bran.japid.template.ActionRunner> getActionRunners() {
		return actionRunners;
	}

	public void setActionRunners(TreeMap<Integer, cn.bran.japid.template.ActionRunner> actionRunners) {
		this.actionRunners = actionRunners;
	}

	public void setOut(StringBundler out) {
		this.out = out;
	}

	protected StringBundler getOut() {
		return out;
	}
	
//	public JapidTemplateBase() {
//		
//	};

	public JapidTemplateBaseWithStringBundler(int capacity) {
		this.out = new StringBundler(capacity);
	}

	// don't use it since it will lead to new instance of stringencoder
	Charset UTF8 = Charset.forName("UTF-8");

	final protected void p(String s) {
		writeString(s);
	}

	final protected void pln(String s) {
		writeString(s);
		out.append('\n');
	}

	/**
	 * @param s
	 * @throws IOException
	 * @throws UnsupportedEncodingException
	 */
	private void writeString(String s) {
		// ByteBuffer bb = StringUtils.encodeUTF8(s);
		// out.write(bb.array(), 0, bb.position());
		// ok my code is slower in large trunk of data
		if (s != null && !s.isEmpty())
			out.append(s);
	}

	// final protected void pln(byte[] ba) {
	// try {
	// out.write(ba);
	// out.write('\n');
	// } catch (IOException e) {
	// throw new RuntimeException(e);
	// }
	// }

	final protected void p(Object s) {
		if (s != null) {
			writeString(s.toString());
			// out.append(s);
		}
	}

	final protected void pln(Object s) {
		if (s != null)
			writeString(s.toString());
		pln();
	}

	final protected void pln() {
		out.append('\n');
	}

	/**
	 * The template pattern to implement the template/layout relationship. Clients call a template's 
	 * render(), which store params in fields and calls in super class's layout, which does the whole page
	 * layout and calls back child's doLayout to get the child content.
	 */
	protected void layout() {
		doLayout();
	}

	protected abstract void doLayout();

	static protected byte[] getBytes(String src) {
		if (src == null || src.length() == 0)
			return new byte[] {};

		try {
			return src.getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public String toString() {
		return this.out.toString();
	}
}
