//version: 0.9.37
package japidviews._layouts;
import java.util.*;
import java.io.*;
import cn.bran.japid.tags.Each;
import japidviews._layouts.*;
import japidviews._tags.*;
//
// NOTE: This file was generated from: japidviews/_layouts/main.html
// Change to this file will be lost next time the template file is compiled.
//
public abstract class main extends cn.bran.japid.template.JapidTemplateBaseWithoutPlay
{
	public static final String sourceTemplate = "japidviews/_layouts/main.html";
	{
	}
	public main() {
	super((StringBuilder)null);
	}
	public main(StringBuilder out) {
		super(out);
	}
	public main(cn.bran.japid.template.JapidTemplateBaseWithoutPlay caller) {
		super(caller);
	}

	private String x; // line 1, japidviews/_layouts/main.html
	 public void layout(String x) {
		this.x = x;
		beginDoLayout(sourceTemplate);
;// line 1, main.html
		p("<head>");// line 1, main.html
		title();p(" - ");// line 2, main.html
		p(x);// line 2, main.html
		p("</head>\n" + 
"<body>");// line 2, main.html
		doLayout();// line 3, main.html
		p("</body>\n");// line 3, main.html
		
		endDoLayout(sourceTemplate);
	}

	 protected void title() {};

	protected abstract void doLayout();
}