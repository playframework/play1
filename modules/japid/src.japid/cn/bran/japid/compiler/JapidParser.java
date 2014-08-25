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
package cn.bran.japid.compiler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.regex.Pattern;

/**
 * Template parser
 * 
 * @author Bing Ran<bing_ran@hotmail.com>
 * @author Play! framework original authors
 */
public class JapidParser {

	private char MARKER_CHAR = '`';
	private String MARKER_STRING = "`";
	private static final String VERBATIM2 = "verbatim";
	private String pageSource;

	public JapidParser(String pageSource) {
		// hack: allow $[] string interpolation in String literals in script block. 
		// see: https://github.com/branaway/Japid/issues/19
		pageSource = pageSource.replaceAll(JapidParser.PLACE_HOLDER_PATTERN_S, JapidParser.SUB_PATTERN_S);

		// detect marker
		// the logic is to find the first line that starts with either ` or @
		char mar = detectMarker(pageSource);
		setMarker(mar);

		this.pageSource = pageSource;
		this.len = pageSource.length();
		
	}

	/**
	 * @param pageSource
	 */
	public static char detectMarker(String pageSource) {
		BufferedReader br = new BufferedReader(new StringReader(pageSource));
		String line;
		try {
			line = br.readLine();
			while (line != null) {
				line = line.trim();
				if (line.startsWith("@")) {
					return '@';
				}
				if (line.startsWith("`")) {
					if (!line.startsWith("``") && !line.startsWith("`@")) {
						return '`';
					}
				}
				line = br.readLine();
			}
			br.close();
		} catch (IOException e) {
		}
		// default
		return '`';
	}

	public void setMarker(char m) {
		MARKER_CHAR = m;
		MARKER_STRING = new String(new char[] { m });
	}

	public void resetMarker() {
		MARKER_CHAR = '`';
		MARKER_STRING = new String(new char[] { MARKER_CHAR });
	}

	// bran:
	// keep track of nested state tokens, eg. nested function calls in
	// expressions
	// what inside is not used for now, we only are interested in the depth
	Stack<JapidParser.Token> methodCallStackInExpr = new Stack<JapidParser.Token>();

	//
	public enum Token {

		EOF, //
		PLAIN, //
		// PLAIN_LEADINGSPACE, // the part after a new line and before any
		// none-space characters
		SCRIPT, // %{...}% or {%...%} bran: or ~{}~, ~[]~ the open wings
				// directives
		SCRIPT_LINE, // line started with marker. will turn the rest if the line
						// in to
						// script.
		EXPR, // ${...}
		EXPR_ESCAPED, // ~{}
		START_TAG, // #{...}
		END_TAG, // #{/...}
		MESSAGE, // &{...}
		ACTION, // @{...}
		ABS_ACTION, // @@{...}
		// bran: to indicate { in action arguments
		ACTION_CURLY, // @@{...}
		COMMENT, // *{...}*
		// bran expression without using {}, such as ~_;
		EXPR_WING, // ~{...}
		EXPR_NATURAL, // $xxx
		EXPR_NATURAL_ESCAPED, // ~xxx
//		EXPR_NATURAL_METHOD_CALL, // bran function call in expression:
//		// ~user?.name.format( '###' )
//		EXPR_NATURAL_ARRAY_OP, // bran : ~myarray[-1].val
//		EXPR_NATURAL_STRING_LITERAL, // bran ~user?.name.format( '#)#' ) or
		// $'hello'.length
		TEMPLATE_ARGS, // bran ~( )
		CLOSING_BRACE, // a closing curly brace after leading space. Used? Good
						// idea?
		VERBATIM, // raw text until another marker
	}

	// end2/begin2: for mark the current returned token
	// begin is the start pos of next token
	// end is the next pos pointer
	private int end, begin, end2, begin2, len;
	private JapidParser.Token state = Token.PLAIN;
	private JapidParser.Token lastState;
	public boolean verbatim;
	public static final String SUB_PATTERN_S = "\"\\+$1\\+\"";
	public static final String PLACE_HOLDER_PATTERN_S = "\\$\\[(.+?)\\]\\$"; // none-greedy match 

	private JapidParser.Token found(JapidParser.Token newState, int skip) {
		begin2 = begin;
		end2 = --end;
		begin = end += skip;
		lastState = state == Token.EXPR_NATURAL ? Token.EXPR : state;
		state = newState;
		return lastState;
	}

	private void skip(int skip) {
		end2 = --end;
		end += skip;
	}

	public Integer getLineNumber() {
		String token = pageSource.substring(0, begin2);
		if (token.indexOf("\n") == -1) {
			return 1;
		} else {
			return token.split("\n").length;
		}
	}

	public String getToken() {
		String tokenString = pageSource.substring(begin2, end2);
		if (lastState == Token.PLAIN) {
			// un-escape special sequence
			tokenString = tokenString.replace("``", "`").replace("`@", "@");
			tokenString = escapeSpecialWith(tokenString, '~');
		}
		else {
			tokenString = escapeSpecialWith(tokenString, (char) 0);
		}
		return tokenString;
	}

	public static String escapeSpecialWith(String src, char marker) {
		int len = src.length();
		StringBuffer buf = new StringBuffer(len);
		if (len < 2)
			return src;
		for (int i = 0; i < len - 1; i++) {
			char c = src.charAt(i);
			char c1 = src.charAt(i + 1);
			char c2 = 0;
			if (i < len - 2) {
				c2 = src.charAt(i + 2);
			}
			if (c == marker) {
				switch (c1) {
				case '`':
					buf.append('`');
					i++;
					break;
				case '~':
					buf.append('~');
					i++;
					break;
				case '@':
					buf.append('@');
					i++;
					break;
				case '#':
					buf.append('#');
					i++;
					break;
				case '$':
					buf.append('$');
					i++;
					break;
				case '%':
					buf.append('%');
					i++;
					break;
				case '&':
					buf.append('%');
					i++;
					break;
				case '*':
					buf.append('*');
					i++;
					break;
				default:
					buf.append(marker);
				}
			} else {
				// detect line continue sign
				if (c == '\\') {
					if (c1 == '\r') {
						if (c2 == '\n') {
							i ++; i++;
							continue;
						} else {
							i++;
							continue;
						}
					} else if (c1 == '\n') {
						i++;
						continue;
					}
				}

				buf.append(c);
			}
		}
		buf.append(src.charAt(len - 1));
		return buf.toString();
	}

	public String checkNext() {
		if (end2 < pageSource.length()) {
			return pageSource.charAt(end2) + "";
		}
		return "";
	}

	public JapidParser.Token nextToken() {
		for (;;) {

			// how many more chars to be processed 
			int left = len - end;
			if (left == 0) {
				end++;
				return found(Token.EOF, 0);
			}

			char c = pageSource.charAt(end++);
			char c1 = left > 1 ? pageSource.charAt(end) : 0;
			char c2 = left > 2 ? pageSource.charAt(end + 1) : 0;

			// detect line continue sign
			if (c == '\\') {
				if (c1 == '\r') {
					if (c2 == '\n') {
						skip(3);
						continue;
					} else {
						skip(2);
						continue;
					}
				} else if (c1 == '\n') {
					skip(2);
					continue;
				}
			}

			switch (state) {
			case PLAIN:
				if (c == '%' && c1 == '{') {
					return found(Token.SCRIPT, 2);
				}
				if (c == '{' && c1 == '%') {
					return found(Token.SCRIPT, 2);
				}
				// bran open wings
				// breaking changes. now used as escaped expression
				// if (c == '~' && c1 == '{') {
				// // deprecated use ~[
				// return found(Token.SCRIPT, 2);
				// }
				if (c == '~')
					if (c1 == '`' ||
								c1 == '~' ||
								c1 == '@' ||
								c1 == '#' ||
								c1 == '$' ||
								c1 == '%' ||
								c1 == '&' ||
								c1 == '*') {
						skip(2);
						break;
					}

				if (c == '~' && c1 == '[') {
					return found(Token.SCRIPT, 2);
				}

				if (c == '$' && c1 == '{') {
					return found(Token.EXPR, 2);
				}
				if (c == '~' && c1 == '{') {
					return found(Token.EXPR_ESCAPED, 2);
				}

				if (c == '~' && c1 == '(') {
					// deprecated in favor of args directive in a script
					return found(Token.TEMPLATE_ARGS, 2);
				}
				// bran: shell like expression: ~_, ~user.name (this one is
				// diff
				// from sh, which requires ${user.name}
				//
				if (c == '~' && c1 != '~' && (Character.isJavaIdentifierStart(c1) || '\'' == c1 || '\"' == c1)) {
					return found(Token.EXPR_NATURAL_ESCAPED, 1);
					// return found(Token.EXPR_NATURAL, 1);
				}
				if (c == '$' && c1 != '$' && (Character.isJavaIdentifierStart(c1) || '\'' == c1 || '\"' == c1)) {
					return found(Token.EXPR_NATURAL, 1);
				}
				if (c == '#' && c1 == '{' && c2 == '/') {
					return found(Token.END_TAG, 3);
				}
				if (c == '#' && c1 == '{') {
					return found(Token.START_TAG, 2);
				}
				if (c == '&' && c1 == '{') {
					return found(Token.MESSAGE, 2);
				}
				if (c == '@' && c1 == '@' && c2 == '{') {
					return found(Token.ABS_ACTION, 3);
				}
				if (c == '@' && c1 == '{') {
					return found(Token.ACTION, 2);
				}
				if (c == '*' && c1 == '{') {
					return found(Token.COMMENT, 2);
				}

				if (c == '`')
					if (c1 == '`' ||
								c1 == '~' ||
								c1 == '@' ||
								c1 == '#' ||
								c1 == '$' ||
								c1 == '%' ||
								c1 == '&' ||
								c1 == '*') {
						skip(2);
						break;
					}

				if (c == MARKER_CHAR)
					if (c1 == MARKER_CHAR) {
						skip(2);
					} else if (c1 == '(') {
						return found(Token.TEMPLATE_ARGS, 2);
					} else if (nextMatch(VERBATIM2)) {
						return found(Token.VERBATIM, VERBATIM2.length() + 1);
					} else {
						return found(Token.SCRIPT_LINE, 1);
					}
				// was trying to implement an escape-less }, but it may be
				// too
				// confusing with json, javascript syntax etc.
				// so it's disabled for now.
				// if (c == '}') {
				// String curToken = getPrevTokenString();
				// boolean allLeadingSpace =
				// allLeadingSpaceInline(curToken);
				// if (allLeadingSpace) {
				// return found(Token.CLOSING_BRACE, 0);
				// }
				// }
				break;
			case CLOSING_BRACE:
				if (c == '\n') {
					return found(Token.PLAIN, 1);
				} else
					return found(Token.SCRIPT_LINE, 1);
			case SCRIPT:
				// the parsing is fragile
				if (c == '}' && c1 == '%') {
					return found(Token.PLAIN, 2);
				}
				if (c == '%' && c1 == '}') {
					return found(Token.PLAIN, 2);
				}
				// bran
				if (c == '}' && c1 == '~') {
					return found(Token.PLAIN, 2);
				}
				if (c == ']' && c1 == '~') {
					return found(Token.PLAIN, 2);
				}
				break;
			case VERBATIM:
				if (c == MARKER_CHAR) {
					String currentLine = getCurrentLine();
					if (currentLine.trim().equals(MARKER_STRING)) {
						int skip = currentLine.length() - currentLine.indexOf(MARKER_CHAR);
						return found(Token.PLAIN, skip);
					}
				}
				break;
			case SCRIPT_LINE:
				if (c == '\r') {
					if (c1 == '\n') {
						return found(Token.PLAIN, 2);
					} else
						return found(Token.PLAIN, 1);
				} else if (c == '\n') {
					return found(Token.PLAIN, 1);
				} else if (c == MARKER_CHAR) {
					return found(Token.PLAIN, 1);
				}
				break;
			case COMMENT:
				if (c == '}' && c1 == '*') {
					return found(Token.PLAIN, 2);
				}
				break;
			case START_TAG:
				if (c == '}') {
					return found(Token.PLAIN, 1);
				}
				if (c == '/' && c1 == '}') {
					return found(Token.END_TAG, 1);
				}
				break;
			case END_TAG:
				if (c == '}') {
					return found(Token.PLAIN, 1);
				}
				break;
			case EXPR:
			case EXPR_ESCAPED:
				if (c == '}') {
					return found(Token.PLAIN, 1);
				}
				break;
			case TEMPLATE_ARGS:
				if (c == ')'){
					String seg = getCurrentPartialToken();
					if (JavaSyntaxTool.isValidParamList(seg)) {
						// eat the rest of the line including the return if it's all white space
						String lineRest = getRestLine();
						if (lineRest.trim().length() == 0) {
							String lineRestIncludingLineBreaks = getRestLineIncludingLineBreaks();
							return found(Token.PLAIN, 1 + lineRestIncludingLineBreaks.length());
						}
						return found(Token.PLAIN, 1);
					}
				}
				break;
			// bran
			// special characters considered an expression: '?.()
			// break characters: space, other punctuations, new lines,
			// returns
			case EXPR_NATURAL:
			case EXPR_NATURAL_ESCAPED:
				//////// using syntax tool to find the end of expression smartly
				String restline = c + getRestLine();
				String longestExpr = JavaSyntaxTool.matchLongestPossibleExpr(restline);
				skip(longestExpr.length() + 1);

//				int nowleft = len - end + 1;
//				if (nowleft == 0) {
//					end++;
//					return found(Token.EOF, 0);
//				}
//				else {
					return found(Token.PLAIN, 0);
//				}
//				
//				
//				////////
//				
//				
//				if ('(' == c) {
//					skipAhead(Token.EXPR_NATURAL_METHOD_CALL, 1);
//				} else if ('[' == c) {
//					skipAhead(Token.EXPR_NATURAL_ARRAY_OP, 1);
//				} else if ('\'' == c) {
//					// \' is valid only at the beginning
//					// FIXME
//					// start of literal
//					skipAhead(Token.EXPR_NATURAL_STRING_LITERAL, 1);
//				} else if (Character.isWhitespace(c)) {
//					// state = Token.EXPR;
//					return found(Token.PLAIN, 0); // it ea
//				} else if (!Character.isJavaIdentifierPart(c)) {
//					if (c != '?' && c != '.' && c != ':' && c != '=') {
//						// state = Token.EXPR;
//						return found(Token.PLAIN, 0); // it ea
//					} else if (!Character.isJavaIdentifierStart(c1)) {
//						if (c == '=' && c1 == '=') {
//							if (Character.isWhitespace(c2)) {
//								// state = Token.EXPR;
//								return found(Token.PLAIN, 0); // it ea
//							} else {
//								skip(2);
//							}
//						} else {
//							// state = Token.EXPR;
//							return found(Token.PLAIN, 0); // it ea
//						}
//					}
//				}
//				break;
//			case EXPR_NATURAL_METHOD_CALL:
//				if ('(' == c) {
//					// nested call
//					skipAhead(Token.EXPR_NATURAL_METHOD_CALL, 1);
//				} else if (')' == c) {
//					state = this.emthodCallStackInExpr.pop();
//					skip(1);
//				}
//				break;
//			case EXPR_NATURAL_ARRAY_OP:
//				if ('[' == c) {
//					// nested call
//					skipAhead(Token.EXPR_NATURAL_ARRAY_OP, 1);
//				} else if (']' == c) {
//					state = this.emthodCallStackInExpr.pop();
//					skip(1);
//				}
//				break;
//			case EXPR_NATURAL_STRING_LITERAL:
//				if ('\\' == c && '\'' == c1) {
//					// the escaped ' in a literal string
//					skip(2);
//				}
//				if ('\'' == c) {
//					// end of literal
//					state = this.emthodCallStackInExpr.pop();
//					skip(1);
//				}
//				break;
			case ACTION:
				if (c == '}') {
					return found(Token.PLAIN, 1);
				} else if (c == '{') { // bran: weak logic: assuming no "{"
										// in
										// string literals
					skipAhead(Token.ACTION_CURLY, 1);
				}
				break;
			case ABS_ACTION:
				if (c == '}') {
					return found(Token.PLAIN, 1);
				} else if (c == '{') {
					skipAhead(Token.ACTION_CURLY, 1);
				}
				break;
			case ACTION_CURLY:
				if (c == '}') {
					state = this.methodCallStackInExpr.pop();
					skip(1);
				} else if (c == '{') {
					skipAhead(Token.ACTION_CURLY, 1);
				}
				break;
			case MESSAGE:
				if (c == '}') {
					return found(Token.PLAIN, 1);
				}
				break;
			}
		}
	}

	/**
	 * @author Bing Ran (bing.ran@gmail.com)
	 * @return
	 */
	private String getCurrentPartialToken() {
		return pageSource.substring(begin, end - 1);
	}

	private String getCurrentLine() {
		return getCurrentLine(pageSource, end - 1);
	}

//	private boolean isStandAloneBackQuote() {
//		String currentLine = getCurrentLine(pageSource, end);
//		if (currentLine.trim().equals(MARKER_STRING)) {
//			return true;
//		} else {
//			return false;
//		}
//	}
//	
	String getRestLine() {
		return getRestLine(pageSource, end - 1);
	}

	String getRestLineIncludingLineBreaks() {
		return getRestLineIncludingLineBreaks(pageSource, end - 1);
	}
	/**
	 * get the rest of the current line 
	 * @return
	 */
	static String getRestLine(String src, int pos) {
		int begin = pos, endp = 0;
		int i = 1;
		int length = src.length();
		while (true) {
			endp = begin + i++;
			if (endp < length) {
				char charAt = src.charAt(endp);
				if (charAt == '\n' || charAt == '\r') {
					// got the end
					break;
				}
			} else {
				break;
			}
			
		}
		return src.substring(++begin, endp);

	}

	static String getRestLineIncludingLineBreaks(String src, int pos) {
		int begin = pos, endp = 0;
		int i = 1;
		int length = src.length();
		boolean breakFound = false;
		while (true) {
			endp = begin + i++;
			if (endp < length) {
				char charAt = src.charAt(endp);
				if (charAt == '\n' || charAt == '\r') {
					breakFound = true;
				}else {
					if (breakFound) {
						// good to go
						break;
					}
				}
			} else {
				break;
			}
			
		}
		return src.substring(++begin, endp);
		
	}
	static String getCurrentLine(String src, final int pos) {
		int begin = 0, endp = 0;
		int i = 1;
		while (true) {
			begin = pos - i++;
			if (begin >= 0) {
				char charAt = src.charAt(begin);
				if (charAt == '\n') {
					// got the beginning
					break;
				}
			} else {
				break;
			}
		}
		i = 1;
		while (true) {
			endp = pos + i++;
			int length = src.length();
			if (endp < length) {
				char charAt = src.charAt(endp);
				if (charAt == '\n') {
					// got the end
					break;
				}
			} else {
				break;
			}

		}
		return src.substring(++begin, endp);
	}

	private boolean nextMatch(String s) {
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			int index = end + i;
			if (index < pageSource.length()) {
				if (c != pageSource.charAt(index)) {
					return false;
				}
			} else {
				return false;
			}
		}

		return true;
	}

//	/**
//	 * @return
//	 */
//	private String getPrevTokenString() {
//		return pageSource.substring(end2, end - 1);
//	}

	/**
	 * @param curToken
	 * @return
	 */
	static boolean allLeadingSpaceInline(String curToken) {
		boolean allLeadingSpace = true;
		int len = curToken.length();
		for (int i = len - 1; i > -1; i--) {
			char ch = curToken.charAt(i);
			if (ch == '\n')
				break;
			else if (ch == ' ' || ch == '\t')
				continue;
			else {
				allLeadingSpace = false;
				break;
			}
		}
		return allLeadingSpace;
	}

	/**
	 * push a nested token to the stack
	 * 
	 * @param token
	 * @param i
	 *            number of chars to skip
	 */
	private void skipAhead(JapidParser.Token token, int i) {
		this.methodCallStackInExpr.push(state);
		state = token;
		skip(i);
	}

	void reset() {
		end = begin = end2 = begin2 = 0;
		state = Token.PLAIN;
	}

	/**
	 * get all the token and content in an ordered list. EOF is not included.
	 * 
	 * @return
	 */
	public List<TokenPair> allTokens() {
		List<TokenPair> result = new ArrayList<TokenPair>();
		loop: for (;;) {
			Token state = nextToken();
			switch (state) {
			case EOF:
				break loop;
			default:
				String tokenstring = getToken();
				result.add(new TokenPair(state, tokenstring));
			}
		}
		return result;
	}
}