package japidviews;
import java.util.TreeMap;

public class devError extends cn.bran.play.JapidTemplateBase
{
	private static final long serialVersionUID = -1632766355048011190L;

	public static final String sourceTemplate = "/japidviews/devError.html";
	{
		putHeader("Content-Type", "text/html; charset=utf-8");
		setContentType("text/html; charset=utf-8");
	}

//// - add implicit fields with Play
//boolean hasHttpContext = play.mvc.Http.Context.current.get() != null ? true : false;
//
//	final Request request = hasHttpContext? Implicit.request() : null;
//	final Response response = hasHttpContext ? Implicit.response() : null;
//	final Session session = hasHttpContext ? Implicit.session() : null;
//	final Flash flash = hasHttpContext ? Implicit.flash() : null;
//	final Lang lang = hasHttpContext ? Implicit.lang() : null;
//	final play.Play _play = new play.Play(); 
//
// - end of implicit fields with Play 


	public devError() {
		super((StringBuilder)null);
	}
	public devError(StringBuilder out) {
		super(out);
	}
/* based on https://github.com/branaway/Japid/issues/12
 */
	public static final String[] argNames = new String[] {/* args of the template*/"error",  };
	public static final String[] argTypes = new String[] {/* arg types of the template*/"cn.bran.japid.exceptions.JapidTemplateException",  };
	public static final Object[] argDefaults= new Object[] {null, };
	public static java.lang.reflect.Method renderMethod = getRenderMethod(japidviews.devError.class);

	{
		setRenderMethod(renderMethod);
		setArgNames(argNames);
		setArgTypes(argTypes);
		setArgDefaults(argDefaults);
		setSourceTemplate(sourceTemplate);
	}
////// end of named args stuff

	private cn.bran.japid.exceptions.JapidTemplateException error; // line 1
	public cn.bran.japid.template.RenderResult render(cn.bran.japid.exceptions.JapidTemplateException error) {
		this.error = error;
		long __t = -1;
		try {super.layout();} catch (RuntimeException __e) { super.handleException(__e);} // line 1
		return new cn.bran.japid.template.RenderResultPartial(getHeaders(), getOut(), __t, actionRunners, sourceTemplate);
	}

	public static cn.bran.japid.template.RenderResult apply(cn.bran.japid.exceptions.JapidTemplateException error) {
		return new devError().render(error);
	}

	@Override protected void doLayout() {
		beginDoLayout(sourceTemplate);
//------
;// line 1
		p("\n");// line 1
		p("<!DOCTYPE html>\n" + 
"<html>\n" + 
"	<head>\n" + 
"		<title>");// line 2
		p(error.title);// line 6
		p("</title>\n" + 
"		<link rel=\"shortcut icon\" href=\"data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAAGXRFWHRTb2Z0d2FyZQBBZG9iZSBJbWFnZVJlYWR5ccllPAAAAlFJREFUeNqUU8tOFEEUPVVdNV3dPe8xYRBnjGhmBgKjKzCIiQvBoIaNbly5Z+PSv3Aj7DSiP2B0rwkLGVdGgxITSCRIJGSMEQWZR3eVt5sEFBgTb/dN1yvnnHtPNTPG4PqdHgCMXnPRSZrpSuH8vUJu4DE4rYHDGAZDX62BZttHqTiIayM3gGiXQsgYLEvATaqxU+dy1U13YXapXptpNHY8iwn8KyIAzm1KBdtRZWErpI5lEWTXp5Z/vHpZ3/wyKKwYGGOdAYwR0EZwoezTYApBEIObyELl/aE1/83cp40Pt5mxqCKrE4Ck+mVWKKcI5tA8BLEhRBKJLjez6a7MLq7XZtp+yyOawwCBtkiBVZDKzRk4NN7NQBMYPHiZDFhXY+p9ff7F961vVcnl4R5I2ykJ5XFN7Ab7Gc61VoipNBKF+PDyztu5lfrSLT/wIwCxq0CAGtXHZTzqR2jtwQiXONma6hHpj9sLT7YaPxfTXuZdBGA02Wi7FS48YiTfj+i2NhqtdhP5RC8mh2/Op7y0v6eAcWVLFT8D7kWX5S9mepp+C450MV6aWL1cGnvkxbwHtLW2B9AOkLeUd9KEDuh9fl/7CEj7YH5g+3r/lWfF9In7tPz6T4IIwBJOr1SJyIGQMZQbsh5P9uBq5VJtqHh2mo49pdw5WFoEwKWqWHacaWOjQXWGcifKo6vj5RGS6zykI587XeUIQDqJSmAp+lE4qt19W5P9o8+Lma5DcjsC8JiT607lMVkdqQ0Vyh3lHhmh52tfNy78ajXv0rgYzv8nfwswANuk+7sD/Q0aAAAAAElFTkSuQmCC\">\n" + 
"	    <style>\n" + 
"		    html, body, pre {\n" + 
"		        margin: 0;\n" + 
"		        padding: 0;\n" + 
"		        font-family: Monaco, 'Lucida Console';\n" + 
"		        background: #ECECEC;\n" + 
"		    }\n" + 
"		    h1 {\n" + 
"		        margin: 0;\n" + 
"		        background: #A31012;\n" + 
"		        padding: 20px 45px;\n" + 
"		        color: #fff;\n" + 
"		        text-shadow: 1px 1px 1px rgba(0,0,0,.3);\n" + 
"		        border-bottom: 1px solid #690000;\n" + 
"		        font-size: 28px;\n" + 
"		    }\n" + 
"		    p#detail {\n" + 
"		        margin: 0;\n" + 
"		        padding: 15px 45px;\n" + 
"		        background: #F5A0A0;\n" + 
"		        border-top: 4px solid #D36D6D;\n" + 
"		        color: #730000;\n" + 
"		        text-shadow: 1px 1px 1px rgba(255,255,255,.3);\n" + 
"		        font-size: 14px;\n" + 
"		        border-bottom: 1px solid #BA7A7A;\n" + 
"		    }\n" + 
"		    p#detail input {\n" + 
"		        background: -webkit-gradient(linear, 0% 0%, 0% 100%, from(#AE1113), to(#A31012));\n" + 
"                border: 1px solid #790000;\n" + 
"                padding: 3px 10px;\n" + 
"                text-shadow: 1px 1px 0 rgba(0, 0, 0, .5);\n" + 
"                color: white;\n" + 
"                border-radius: 3px;\n" + 
"                cursor: pointer;\n" + 
"                font-family: Monaco, 'Lucida Console';\n" + 
"                font-size: 12px;\n" + 
"                margin: 0 10px;\n" + 
"                display: inline-block;\n" + 
"                position: relative;\n" + 
"                top: -1px;\n" + 
"		    }\n" + 
"		    h2 {\n" + 
"		        margin: 0;\n" + 
"		        padding: 5px 45px;\n" + 
"		        font-size: 12px;\n" + 
"		        background: #333;\n" + 
"		        color: #fff;\n" + 
"		        text-shadow: 1px 1px 1px rgba(0,0,0,.3);\n" + 
"		        border-top: 4px solid #2a2a2a;\n" + 
"		    }\n" + 
"			pre {\n" + 
"				margin: 0;\n" + 
"				border-bottom: 1px solid #DDD;\n" + 
"				text-shadow: 1px 1px 1px rgba(255,255,255,.5);\n" + 
"				position: relative;\n" + 
"				font-size: 12px;\n" + 
"				overflow: hidden;\n" + 
"			}\n" + 
"			pre span.line {\n" + 
"			    text-align: right;\n" + 
"			    display: inline-block;\n" + 
"			    padding: 5px 5px;\n" + 
"			    width: 30px;\n" + 
"			    background: #D6D6D6;\n" + 
"			    color: #8B8B8B;\n" + 
"			    text-shadow: 1px 1px 1px rgba(255,255,255,.5);\n" + 
"			    font-weight: bold;\n" + 
"			}\n" + 
"			pre span.code {\n" + 
"			    padding: 5px 5px;\n" + 
"			    position: absolute;\n" + 
"			    right: 0;\n" + 
"			    left: 40px;\n" + 
"			}\n" + 
"			pre:first-child span.code {\n" + 
"			    border-top: 4px solid #CDCDCD;\n" + 
"			}\n" + 
"			pre:first-child span.line {\n" + 
"			    border-top: 4px solid #B6B6B6;\n" + 
"			}\n" + 
"			pre.error span.line {\n" + 
"			    background: #A31012;\n" + 
"			    color: #fff;\n" + 
"			    text-shadow: 1px 1px 1px rgba(0,0,0,.3);\n" + 
"			}\n" + 
"			pre.error {\n" + 
"				color: #A31012;\n" + 
"			}\n" + 
"			pre.error span.marker {\n" + 
"				background: #A31012;\n" + 
"				color: #fff;\n" + 
"				text-shadow: 1px 1px 1px rgba(0,0,0,.3);\n" + 
"			}\n" + 
"		</style>\n" + 
"	</head>\n" + 
"	<body>\n" + 
"		<h1>");// line 6
		p(escape(error.title));// line 104
		p("</h1>\n" + 
"\n" + 
"		<p id=\"detail\">\n" + 
"		    ");// line 104
		p(escape(error.description));// line 107
		p("		</p>\n" + 
"		");// line 107
		p("        <div>\n" + 
"        	");// line 113
		TreeMap<Integer, String> lines = error.interestingLines; // line 115
			for(int i : lines.keySet()) {// line 116
				String line = lines.get(i);// line 117
				if(error.errLineNum == i) {// line 118
		p("					<pre class=\"error\"><span class=\"line\">");// line 118
		p(i);// line 119
		p("</span><span class=\"code\">");// line 119
		p(escape(line));// line 119
		p("</span></pre>\n" + 
"				");// line 119
		} else {// line 120
		p("					<pre><span class=\"line\">");// line 120
		p(i);// line 121
		p("</span><span class=\"code\">");// line 121
		p(escape(line));// line 121
		p("</span></pre>\n" + 
"				");// line 121
		}// line 122
			} // line 123
		p("		</div>\n" + 
"			    \n" + 
"	</body>\n" + 
"</html>\n" + 
"\n" + 
"\n" + 
"\n" + 
"\n" + 
"\n" + 
"\n" + 
"\n");// line 123
		
		endDoLayout(sourceTemplate);
	}

}
