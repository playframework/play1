//version: 0.9.37
package japidviews;
import java.util.*;
import java.io.*;
import cn.bran.japid.tags.Each;
import japidviews._layouts.*;
import japidviews._tags.*;
//
// NOTE: This file was generated from: japidviews/open_for.html
// Change to this file will be lost next time the template file is compiled.
//
public class open_for extends cn.bran.japid.template.JapidTemplateBaseWithoutPlay
{
	public static final String sourceTemplate = "japidviews/open_for.html";
	{
	}
	public open_for() {
	super((StringBuilder)null);
	}
	public open_for(StringBuilder out) {
		super(out);
	}
	public open_for(cn.bran.japid.template.JapidTemplateBaseWithoutPlay caller) {
		super(caller);
	}

/* based on https://github.com/branaway/Japid/issues/12
 */
	public static final String[] argNames = new String[] {/* args of the template*/ };
	public static final String[] argTypes = new String[] {/* arg types of the template*/ };
	public static final Object[] argDefaults= new Object[] { };
	public static java.lang.reflect.Method renderMethod = getRenderMethod(japidviews.open_for.class);

	{
		setRenderMethod(renderMethod);
		setArgNames(argNames);
		setArgTypes(argTypes);
		setArgDefaults(argDefaults);
		setSourceTemplate(sourceTemplate);
	}
////// end of named args stuff

	public String render() {
		try {super.layout();} catch (RuntimeException __e) { super.handleException(__e);} // line 0, japidviews/open_for.html
		return getRenderResult().toString();
	}

	public static String apply() {
		return new open_for().render();
	}

	@Override protected void doLayout() {
		beginDoLayout(sourceTemplate);
;// line 1, open_for.html
		final String[] ss = {"a", "b", "c"};// line 1, open_for.html
new Runnable() {public void run() {
int _size = -100; int _index = 0; boolean _isOdd = false; String _parity = ""; boolean _isFirst = true; Boolean _isLast = _index == _size;
for (String s : ss) { // line 2, open_for.html
	_index++; _isOdd = !_isOdd; _parity = _isOdd? "odd" : "even"; _isFirst = _index == 1; if (_size == -100) _size = getCollectionSize(ss); _isLast = (_size < 0 ? null : _index == _size);
// line 2, open_for.html
		p("	value:");// line 2, open_for.html
		p(s);// line 3, open_for.html
		p(", index: ");// line 3, open_for.html
		p(_index);// line 3, open_for.html
		p(", isOdd: ");// line 3, open_for.html
		p(_isOdd);// line 3, open_for.html
		p(", parity: ");// line 3, open_for.html
		p(_parity);// line 3, open_for.html
		p(", isFirst: ");// line 3, open_for.html
		p(_isFirst);// line 3, open_for.html
		p(", isLast: ");// line 3, open_for.html
		p(_isLast);// line 3, open_for.html
		p(", size: ");// line 3, open_for.html
		p(_size);// line 3, open_for.html
		p(" ");// line 3, open_for.html
		p("\n");// line 3, open_for.html
		p("	\n" + 
"	");// line 3, open_for.html
		final List<String> lss = new ArrayList(){{add("la");add("lb");add("lc");}};// line 5, open_for.html
	
	new Runnable() {public void run() {
int _size = -100; int _index = 0; boolean _isOdd = false; String _parity = ""; boolean _isFirst = true; Boolean _isLast = _index == _size;
for (String ls : lss) { // line 7, open_for.html
	_index++; _isOdd = !_isOdd; _parity = _isOdd? "odd" : "even"; _isFirst = _index == 1; if (_size == -100) _size = getCollectionSize(lss); _isLast = (_size < 0 ? null : _index == _size);
// line 7, open_for.html
		p("		value:");// line 7, open_for.html
		p(ls);// line 8, open_for.html
		p(", index: ");// line 8, open_for.html
		p(_index);// line 8, open_for.html
		p(", isOdd: ");// line 8, open_for.html
		p(_isOdd);// line 8, open_for.html
		p(", parity: ");// line 8, open_for.html
		p(_parity);// line 8, open_for.html
		p(", isFirst: ");// line 8, open_for.html
		p(_isFirst);// line 8, open_for.html
		p(", isLast: ");// line 8, open_for.html
		p(_isLast);// line 8, open_for.html
		p(", size: ");// line 8, open_for.html
		p(_size);// line 8, open_for.html
		p(" ");// line 8, open_for.html
		p("\n");// line 8, open_for.html
		p("	");// line 8, open_for.html
		
}
}}.run();
// line 7, open_for.html

}
}}.run();
// line 2, open_for.html
		p("\n");// line 10, open_for.html
		
		endDoLayout(sourceTemplate);
	}

}