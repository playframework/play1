package cn.bran.japid.compiler;

import cn.bran.japid.compiler.JapidParser.Token;

class TokenPair {
	Token token;
	String source;
	public TokenPair(Token token, String source) {
		super();
		this.token = token;
		this.source = source;
	}
}