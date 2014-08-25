//version: 0.9.35
package japidviews;

import java.util.List;

import play.exceptions.PlayException;
import play.exceptions.SourceAttachment;
//
// NOTE: This file was generated from: japidviews/myerror500.html
// Change to this file will be lost next time the template file is compiled.
//

@cn.bran.play.NoEnhance
public class error500ForPlay extends cn.bran.play.JapidTemplateBase {
	public static final String sourceTemplate = "japidviews/myerror500.html";
	{
		putHeader("Content-Type", "text/html; charset=utf-8");
		setContentType("text/html; charset=utf-8");
	}

	// - add implicit fields with Play

	final play.mvc.Http.Request request = play.mvc.Http.Request.current();
	final play.mvc.Http.Response response = play.mvc.Http.Response.current();
	final play.mvc.Scope.Session session = play.mvc.Scope.Session.current();
	final play.mvc.Scope.RenderArgs renderArgs = play.mvc.Scope.RenderArgs.current();
	final play.mvc.Scope.Params params = play.mvc.Scope.Params.current();
	final play.data.validation.Validation validation = play.data.validation.Validation.current();
	final cn.bran.play.FieldErrors errors = new cn.bran.play.FieldErrors(validation);
	final play.Play _play = new play.Play();

	// - end of implicit fields with Play

	public error500ForPlay() {
		super((StringBuilder)null);
	}

	public error500ForPlay(StringBuilder out) {
		super(out);
	}

	/*
	 * based on https://github.com/branaway/Japid/issues/12
	 */
	public static final String[] argNames = new String[] {/* args of the template */"exp", };
	public static final String[] argTypes = new String[] {/*
														 * arg types of the
														 * template
														 */"Exception", };
	public static final Object[] argDefaults = new Object[] { null, };
	public static java.lang.reflect.Method renderMethod = getRenderMethod(japidviews.error500ForPlay.class);

	{
		setRenderMethod(renderMethod);
		setArgNames(argNames);
		setArgTypes(argTypes);
		setArgDefaults(argDefaults);
		setSourceTemplate(sourceTemplate);
	}
	// //// end of named args stuff

	private Exception exp; // line 2, japidviews/myerror500.html

	public cn.bran.japid.template.RenderResult render(Exception exp) {
		this.exp = exp;
		long __t = -1;
		try {
			super.layout();
		} catch (RuntimeException __e) {
			super.handleException(__e);
		} // line 2, japidviews/myerror500.html
		return new cn.bran.japid.template.RenderResultPartial(getHeaders(), getOut(), __t, actionRunners,
				sourceTemplate);
	}

	public static cn.bran.japid.template.RenderResult apply(Exception exp) {
		return new error500ForPlay().render(exp);
	}

	@Override
	protected void doLayout() {
		beginDoLayout(sourceTemplate);
		// ------
		;// line 1, myerror500.html
		;// line 1, myerror500.html
		p("<style type=\"text/css\">\n" + "    html, body {\n" + "        margin: 0;\n" + "        padding: 0;\n"
				+ "        font-family: Helvetica, Arial, Sans;\n" + "        background: #EEEEEE;\n" + "    }\n"
				+ "    .block {\n" + "        padding: 20px;\n" + "        border-bottom: 1px solid #aaa;\n"
				+ "    }\n" + "    #header h1 {\n" + "        font-weight: normal;\n" + "        font-size: 28px;\n"
				+ "        margin: 0;\n" + "    }\n" + "    #more {\n" + "        color: #666;\n"
				+ "        font-size: 80%;\n" + "        border: none;\n" + "    }\n" + "    #header {\n"
				+ "        background: #fcd2da;\n" + "    }\n" + "    #header p {\n" + "        color: #333;\n"
				+ "    }\n" + "    #source {\n" + "        background: #f6f6f6;\n" + "    }\n" + "    #source h2 {\n"
				+ "        font-weight: normal;\n" + "        font-size: 18px;\n" + "        margin: 0 0 10px 0;\n"
				+ "    }\n" + "    #source .lineNumber {\n" + "        float: left;\n" + "        display: block;\n"
				+ "        width: 40px;\n" + "        text-align: right;\n" + "        margin-right: 10px;\n"
				+ "        font-size: 14px;\n" + "        font-family: monospace;\n" + "        background: #333;\n"
				+ "        color: #fff;\n" + "    }\n" + "    #source .line {\n" + "        clear: both;\n"
				+ "        color: #333;\n" + "        margin-bottom: 1px;\n" + "    }\n" + "    #source pre {\n"
				+ "        font-size: 14px;\n" + "        margin: 0;\n" + "        overflow-x: hidden;\n" + "    }\n"
				+ "    #source .error {\n" + "        color: #c00 !important;\n" + "    }\n"
				+ "    #source .error .lineNumber {\n" + "        background: #c00;\n" + "    }\n"
				+ "    #source a {\n" + "        text-decoration: none;\n" + "    }\n" + "    #source a:hover * {\n"
				+ "        cursor: pointer !important;\n" + "    }\n" + "    #source a:hover pre {\n"
				+ "        background: #FAFFCF !important;\n" + "    }\n" + "    #source em {\n"
				+ "        font-style: normal;\n" + "        text-decoration: underline;\n"
				+ "        font-weight: bold;\n" + "    }\n" + "    #source strong {\n"
				+ "        font-style: normal;\n" + "        font-weight: bold;\n" + "    }\n" + "</style>\n");// line
																												// 2,
																												// myerror500.html

		if (asBoolean(exp instanceof PlayException)) {// line 87,
														// myerror500.html
			PlayException exception = (PlayException) exp;// line 88,
															// myerror500.html
			Integer lineNumber = exception.getLineNumber();// line 89,
															// myerror500.html
			String mode = play.Play.mode.name();// line 90, myerror500.html
			List<String> source = exception.isSourceAvailable() ? ((SourceAttachment) exception).getSource() : null;// line
																													// 91,
																													// myerror500.html
			int sourceSize = source == null ? -1 : source.size();// line 92,
																	// myerror500.html
			p("    <div id=\"header\" class=\"block\">\n" + "        <h1>\n" + "            ");// line
																								// 92,
																								// myerror500.html
			p((exception.getErrorTitle()));// line 95, myerror500.html
			p(" \n" + "        </h1>\n" + "        ");// line 95,
														// myerror500.html
			if (asBoolean(mode.equals("DEV"))) {// line 97, myerror500.html
				p("	        <p>\n" + "	            ");// line 97,
														// myerror500.html
				p((exception.getErrorDescription()));// line 99, myerror500.html
				p("	        </p>\n" + "        ");// line 99, myerror500.html
			}// line 101, myerror500.html
			if (asBoolean(mode.equals("PROD"))) {// line 102, myerror500.html
				p("	        <p>\n"
						+ "	            ... Error details are not displayed when Play! is in PROD mode. Check server logs for detail.\n"
						+ "	        </p>\n" + "        ");// line 102,
															// myerror500.html
			}// line 106, myerror500.html
			p("    </div>\n" + "    ");// line 106, myerror500.html
			if (asBoolean(exception.isSourceAvailable() && lineNumber != null && mode.equals("DEV"))) {// line
																										// 108,
																										// myerror500.html
				p("	    <div id=\"source\" class=\"block\">\n" + "	        <h2>In ");// line
																						// 108,
																						// myerror500.html
				p(exception.getSourceFile());// line 110, myerror500.html
				p(" (around line ");// line 110, myerror500.html
				p(lineNumber);// line 110, myerror500.html
				p(")</h2>\n" + "            \n" + "            ");// line 110,
																	// myerror500.html
				int from = lineNumber - 5 >= 0 && lineNumber <= sourceSize ? lineNumber - 5 : 0;// line
																								// 112,
																								// myerror500.html
				int to = lineNumber + 5 < sourceSize ? lineNumber + 5 : sourceSize - 1;// line
																						// 113,
																						// myerror500.html

				for (int i = from; i < to; i++) {// line 115, myerror500.html
					String line = source.get(i);// line 116, myerror500.html
					p("                <div class=\"line ");// line 116,
															// myerror500.html
					p(lineNumber.equals(i + 1) ? "error" : "");// line 117,
																// myerror500.html
					p("\">\n" + "                    <span class=\"lineNumber\">");// line
																					// 117,
																					// myerror500.html
					p(i + 1);// line 118, myerror500.html
					p(":</span>\n" + "                    <pre>&nbsp;");// line
																		// 118,
																		// myerror500.html
					p(escape(line));// line 119, myerror500.html
					p("</pre>\n" + "                </div>\n" + "	        ");// line
																				// 119,
																				// myerror500.html
				}// line 121, myerror500.html
				p("	    </div>\n" + "    ");// line 121, myerror500.html
			}// line 123, myerror500.html

			String moreHtml = exception.getMoreHTML();// line 125,
														// myerror500.html
			if (asBoolean(moreHtml)) {// line 126, myerror500.html
				p("        <div id=\"specific\" class=\"block\">\n" + "            ");// line
																						// 126,
																						// myerror500.html
				p(moreHtml);// line 128, myerror500.html
				p("        </div>\n" + "    ");// line 128, myerror500.html
			}// line 130, myerror500.html
			p("    <div id=\"more\" class=\"block\">\n" + "        This exception has been logged with id <strong>");// line
																														// 130,
																														// myerror500.html
			p(exception.getId());// line 132, myerror500.html
			p("</strong>\n" + "    </div>\n");// line 132, myerror500.html
		} else {// line 134, myerror500.html
			p("    <div id=\"header\" class=\"block\">\n" + "        <h1>");// line
																			// 134,
																			// myerror500.html
			try {
				Object o = exp.getMessage();
				if (o.toString().length() == 0) {
					;
				} else {
					p(o);
				}
			} catch (NullPointerException npe) {
				;
			}// line 136, myerror500.html
			p("</h1>\n" + "    </div>\n");// line 136, myerror500.html
		}// line 138, myerror500.html
		;// line 138, myerror500.html

		endDoLayout(sourceTemplate);
	}

}