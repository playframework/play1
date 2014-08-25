package cn.bran.japid.compiler;

import japa.parser.ast.expr.Expression;

/**
 * a = "asd", b = 12, etc
 * 
 * @author Bing Ran<bing_ran@hotmail.com>
 *
 */
public class NamedArgRuntime {
	public NamedArgRuntime(String name, Object val) {
		this.name = name;
		this.val = val;
	}
	public String name;
	public Object val;
	@Override
	public String toString() {
		return name + " = " + val;
	}
	
}
