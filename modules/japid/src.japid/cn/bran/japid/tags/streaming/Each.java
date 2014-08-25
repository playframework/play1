package cn.bran.japid.tags.streaming;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Iterator;

public class Each extends cn.bran.japid.template.JapidTemplateBaseStreaming {
	public static final String sourceTemplate = "tag/Each.html";

	public Each(OutputStream out) {
		super(out);
	}

	public void render(Iterable it, DoBody body) {
		Iterator itor = it.iterator();
		itBody(body, itor);
	}

	// TODO:  more polymorphic renders
	
	/**
	 * @param body
	 * @param it
	 */
	private void itBody(DoBody body, Iterator it) {
		int start = 0;
		int i = 0;
		while(it.hasNext()) {
			i++;
			Object o =it.next();
			body.render(o, i, i % 2 == 1, i == start + 1, !it.hasNext());
		}
	}

	@Override
	protected void doLayout() {
		// dummy
	}
	
	public static interface DoBody<E> {
		void render(E __, int _index, boolean _isOdd, boolean _first, boolean _last);
		void setBuffer(StringBuilder sb);
		void resetBuffer();

	}

}
