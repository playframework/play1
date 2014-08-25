//version: 0.9.37
package japidviews._tags;
import java.util.*;
import java.io.*;
import cn.bran.japid.tags.Each;
import japidviews._layouts.*;
import japidviews._tags.*;
//
// NOTE: This file was generated from: japidviews/_tags/taggy.html
// Change to this file will be lost next time the template file is compiled.
//
public class taggy extends cn.bran.japid.template.JapidTemplateBaseWithoutPlay
{
	public static final String sourceTemplate = "japidviews/_tags/taggy.html";
	{
	}
	public taggy() {
	super((StringBuilder)null);
	}
	public taggy(StringBuilder out) {
		super(out);
	}
	public taggy(cn.bran.japid.template.JapidTemplateBaseWithoutPlay caller) {
		super(caller);
	}

/* based on https://github.com/branaway/Japid/issues/12
 */
	public static final String[] argNames = new String[] {/* args of the template*/"a",  };
	public static final String[] argTypes = new String[] {/* arg types of the template*/"String",  };
	public static final Object[] argDefaults= new Object[] {null, };
	public static java.lang.reflect.Method renderMethod = getRenderMethod(japidviews._tags.taggy.class);

	{
		setRenderMethod(renderMethod);
		setArgNames(argNames);
		setArgTypes(argTypes);
		setArgDefaults(argDefaults);
		setSourceTemplate(sourceTemplate);
	}
////// end of named args stuff

	private String a; // line 1, japidviews/_tags/taggy.html
	public String render(String a) {
		this.a = a;
		try {super.layout();} catch (RuntimeException __e) { super.handleException(__e);} // line 1, japidviews/_tags/taggy.html
		return getRenderResult().toString();
	}

	public static String apply(String a) {
		return new taggy().render(a);
	}

	@Override protected void doLayout() {
		beginDoLayout(sourceTemplate);
;// line 1, taggy.html
		p("[");// line 1, taggy.html
		p(a);// line 2, taggy.html
		p("]-->\n");// line 2, taggy.html
		new taddy(taggy.this).render(// line 3, taggy.html
new taddy.DoBody<String[]>(){ // line 3, taggy.html
public void render(final String[] ss) { // line 3, taggy.html
// line 3, taggy.html
    new Runnable() {public void run() {
int _size = -100; int _index = 0; boolean _isOdd = false; String _parity = ""; boolean _isFirst = true; Boolean _isLast = _index == _size;
for (String s : ss) { // line 4, taggy.html
	_index++; _isOdd = !_isOdd; _parity = _isOdd? "odd" : "even"; _isFirst = _index == 1; if (_size == -100) _size = getCollectionSize(ss); _isLast = (_size < 0 ? null : _index == _size);
// line 4, taggy.html
		p("    -> ");// line 4, taggy.html
		p(s);// line 5, taggy.html
		p("\n" + 
"    ");// line 5, taggy.html
		
}
}}.run();
// line 4, taggy.html

}

StringBuilder oriBuffer;
@Override
public void setBuffer(StringBuilder sb) {
	oriBuffer = getOut();
	setOut(sb);
}

@Override
public void resetBuffer() {
	setOut(oriBuffer);
}

}
);// line 3, taggy.html
		
		endDoLayout(sourceTemplate);
	}

}