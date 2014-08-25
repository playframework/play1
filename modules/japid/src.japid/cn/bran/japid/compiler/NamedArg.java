package cn.bran.japid.compiler;

import japa.parser.ast.expr.Expression;

/**
 * a = "asd", b = 12, etc
 * 
 * @author Bing Ran<bing_ran@hotmail.com>
 *
 */
public class NamedArg {
	public NamedArg(Expression target, Expression value) {
		name = target.toString(); 
		valExpr = value.toString();
	}
	public String name;
	public String valExpr;
	@Override
	public String toString() {
		return name + " = " + valExpr;
	}
	public String toNamed() {
		return "named(\"" + name + "\", " + valExpr + ")";
	}
	
	
}
