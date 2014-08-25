package cn.bran.japid.compiler;

import japa.parser.JavaParser;
import japa.parser.ParseException;
import japa.parser.TokenMgrError;
import japa.parser.ast.BlockComment;
import japa.parser.ast.CompilationUnit;
import japa.parser.ast.ImportDeclaration;
import japa.parser.ast.LineComment;
import japa.parser.ast.Node;
import japa.parser.ast.PackageDeclaration;
import japa.parser.ast.TypeParameter;
import japa.parser.ast.body.AnnotationDeclaration;
import japa.parser.ast.body.AnnotationMemberDeclaration;
import japa.parser.ast.body.ClassOrInterfaceDeclaration;
import japa.parser.ast.body.ConstructorDeclaration;
import japa.parser.ast.body.EmptyMemberDeclaration;
import japa.parser.ast.body.EmptyTypeDeclaration;
import japa.parser.ast.body.EnumConstantDeclaration;
import japa.parser.ast.body.EnumDeclaration;
import japa.parser.ast.body.FieldDeclaration;
import japa.parser.ast.body.InitializerDeclaration;
import japa.parser.ast.body.JavadocComment;
import japa.parser.ast.body.MethodDeclaration;
import japa.parser.ast.body.ModifierSet;
import japa.parser.ast.body.Parameter;
import japa.parser.ast.body.VariableDeclarator;
import japa.parser.ast.body.VariableDeclaratorId;
import japa.parser.ast.expr.AnnotationExpr;
import japa.parser.ast.expr.ArrayAccessExpr;
import japa.parser.ast.expr.ArrayCreationExpr;
import japa.parser.ast.expr.ArrayInitializerExpr;
import japa.parser.ast.expr.AssignExpr;
import japa.parser.ast.expr.BinaryExpr;
import japa.parser.ast.expr.BinaryExpr.Operator;
import japa.parser.ast.expr.BooleanLiteralExpr;
import japa.parser.ast.expr.CastExpr;
import japa.parser.ast.expr.CharLiteralExpr;
import japa.parser.ast.expr.ClassExpr;
import japa.parser.ast.expr.ConditionalExpr;
import japa.parser.ast.expr.DoubleLiteralExpr;
import japa.parser.ast.expr.EnclosedExpr;
import japa.parser.ast.expr.Expression;
import japa.parser.ast.expr.FieldAccessExpr;
import japa.parser.ast.expr.InstanceOfExpr;
import japa.parser.ast.expr.IntegerLiteralExpr;
import japa.parser.ast.expr.IntegerLiteralMinValueExpr;
import japa.parser.ast.expr.LongLiteralExpr;
import japa.parser.ast.expr.LongLiteralMinValueExpr;
import japa.parser.ast.expr.MarkerAnnotationExpr;
import japa.parser.ast.expr.MemberValuePair;
import japa.parser.ast.expr.MethodCallExpr;
import japa.parser.ast.expr.NameExpr;
import japa.parser.ast.expr.NormalAnnotationExpr;
import japa.parser.ast.expr.NullLiteralExpr;
import japa.parser.ast.expr.ObjectCreationExpr;
import japa.parser.ast.expr.QualifiedNameExpr;
import japa.parser.ast.expr.SingleMemberAnnotationExpr;
import japa.parser.ast.expr.StringLiteralExpr;
import japa.parser.ast.expr.SuperExpr;
import japa.parser.ast.expr.ThisExpr;
import japa.parser.ast.expr.UnaryExpr;
import japa.parser.ast.expr.VariableDeclarationExpr;
import japa.parser.ast.stmt.AssertStmt;
import japa.parser.ast.stmt.BlockStmt;
import japa.parser.ast.stmt.BreakStmt;
import japa.parser.ast.stmt.CatchClause;
import japa.parser.ast.stmt.ContinueStmt;
import japa.parser.ast.stmt.DoStmt;
import japa.parser.ast.stmt.EmptyStmt;
import japa.parser.ast.stmt.ExplicitConstructorInvocationStmt;
import japa.parser.ast.stmt.ExpressionStmt;
import japa.parser.ast.stmt.ForStmt;
import japa.parser.ast.stmt.ForeachStmt;
import japa.parser.ast.stmt.IfStmt;
import japa.parser.ast.stmt.LabeledStmt;
import japa.parser.ast.stmt.ReturnStmt;
import japa.parser.ast.stmt.SwitchEntryStmt;
import japa.parser.ast.stmt.SwitchStmt;
import japa.parser.ast.stmt.SynchronizedStmt;
import japa.parser.ast.stmt.ThrowStmt;
import japa.parser.ast.stmt.TryStmt;
import japa.parser.ast.stmt.TypeDeclarationStmt;
import japa.parser.ast.stmt.WhileStmt;
import japa.parser.ast.type.ClassOrInterfaceType;
import japa.parser.ast.type.PrimitiveType;
import japa.parser.ast.type.ReferenceType;
import japa.parser.ast.type.Type;
import japa.parser.ast.type.VoidType;
import japa.parser.ast.type.WildcardType;
import japa.parser.ast.visitor.GenericVisitor;
import japa.parser.ast.visitor.VoidVisitor;
import japa.parser.ast.visitor.VoidVisitorAdapter;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JavaSyntaxTool {
	private static final String UTF_8 = "UTF-8";

	public static CompilationUnit parse(String src) throws ParseException {
		ByteArrayInputStream in;
		try {
			in = new ByteArrayInputStream(src.getBytes(UTF_8));
			CompilationUnit cu;
			cu = JavaParser.parse(in, UTF_8);
			return cu;
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TokenMgrError e) {
			throw new ParseException(e.getMessage());
		}
		return null;
	}

	public static boolean isValid(String src) {
		try {
			CompilationUnit cu = parse(src);
			return true;
		} catch (ParseException e) {
			String m = e.getMessage() + "\n";
			System.out.println(m.substring(0, m.indexOf('\n')));
			return false;
		}
	}
	

	/**
	 * 
	 * @author Bing Ran<bing_ran@hotmail.com>
	 * @deprecated use the original Parameter for complete control
	 */
	public static class Param {
		public String type, name;

		public Param(String type, String name) {
			this.type = type;
			this.name = name;
		}
	}

	private static final String classTempForParams = "class T { void t(%s) {} }";

	/**
	 * parse a line of text that is supposed to be parameter list for a method
	 * declaration.
	 * 
	 * TODO: the parameter annotation, modifiers, etc ignored. should do it!
	 * 
	 * @param line
	 * @return
	 */
	public static List<Parameter> parseParams(String line) {
		final List<Parameter> ret = new ArrayList<Parameter>();
		if (line == null || line.trim().length() == 0)
			return ret;

		// make it tolerant of lowercase default
		line = line.replace("@default(", "@Default(");
		String cl = String.format(classTempForParams, line);
		try {
			CompilationUnit cu = parse(cl);
			VoidVisitorAdapter visitor = new VoidVisitorAdapter() {
				@Override
				public void visit(Parameter p, Object arg) {
					ret.add(p);
				}
			};
			cu.accept(visitor, null);
		} catch (ParseException e) {
			throw new RuntimeException(
					"the line does not seem to be a valid param list declaration: "
							+ line);
		}
		return ret;
	}

	/**
	 * tell if a string is a valid method parameter list expression
	 * 
	 * @author Bing Ran (bing.ran@gmail.com)
	 * @param line
	 * @return
	 */
	public static boolean isValidParamList(String line) {
		if (line == null)
			return false;
		if (line.trim().length() == 0) 
			return true;
		
		try {
			parseParams(line);
			return true;
		}
		catch(Exception e) {
			return false;
		}
	}

	private static final String classTempForArgs = "class T {  {  foo(%s); } }";
	private static final String classTempForArgsNoParenthesis = "class T {  {  foo%s; } }";

	// XXX this method does not properly parse thingsl like A<t> a
	// it does not detect the error
	@SuppressWarnings("unchecked")
	public static List<String> parseArgs(String line) {
		final List<String> ret = new ArrayList<String>();
		if (line == null || line.trim().length() == 0)
			return ret;


		@SuppressWarnings("rawtypes")
		VoidVisitorAdapter visitor = new VoidVisitorAdapter() {
			@Override
			public void visit(MethodCallExpr n, Object arg) {
				List<Expression> args = n.getArgs();
				// api issue: args can be null in case of empty arg list
				if (args != null)
					for (Expression e : args) {
						ret.add(e.toString());
					}
			}
		};

		line = line.trim();
		if (line.startsWith("(")){
			// perhaps it's in the form of (...)
			String cl = String.format(classTempForArgsNoParenthesis, line);
			try {
				CompilationUnit cu = parse(cl);
				cu.accept(visitor, null);
				return ret;
			} catch (ParseException e) {
				// perhaps not really (...). fall through
			}
		}
		
		String cl = String.format(classTempForArgs, line);
		try {
			CompilationUnit cu = parse(cl);
			cu.accept(visitor, null);
		} catch (ParseException e) {
			throw new RuntimeException(
					"the line does not seem to be a valid arg list: " + line);
		}
		return ret;
	}

	/**
	 * 
	 * @param line
	 * @return list of named args if all the args are named; empty list if none
	 *         is named; or an exception is thrown if the arg list is not valid
	 *         or named and un-named are mixed
	 * 
	 */
	public static List<NamedArg> parseNamedArgs(String line) {
		final List<NamedArg> ret = new ArrayList<NamedArg>();
		if (line == null || line.trim().length() == 0)
			return ret;

		line = line.trim();
//		if (line.startsWith("(")) {
//			if (line.endsWith(")"))
//				line = line.substring(1, line.length() - 1);
//			else
//				throw new RuntimeException("no closing ')' in arg expression: "
//						+ line);
//		}

		String cl = String.format(classTempForArgs, line);
		final String finalLine = line;
		try {
			CompilationUnit cu = parse(cl);
			VoidVisitorAdapter visitor = new VoidVisitorAdapter() {
				boolean hasNamed = false;
				boolean hasUnNamed = false;

				@Override
				public void visit(MethodCallExpr n, Object arg) {
					List<Expression> args = n.getArgs();
					// api issue: args can be null in case of empty arg list
					if (args != null)
						for (Expression expr : args) {
							if (expr instanceof AssignExpr) {
								if (hasUnNamed)
									throw new RuntimeException(
											"the line has mixed named and un-named arg list. It's not valid in Japid tag invocation. It must be all-or-none.: "
													+ finalLine);
								hasNamed = true;
								AssignExpr ae = (AssignExpr) expr;
								NamedArg na = new NamedArg(ae.getTarget(),
										ae.getValue());
								ret.add(na);
							} else {
								if (hasNamed)
									throw new RuntimeException(
											"the line has mixed named and un-named arg list. It's not valid in Japid tag invocation. It must be all-or-none.: "
													+ finalLine);
								hasUnNamed = true;
							}
						}
				}
			};
			cu.accept(visitor, null);
		} catch (ParseException e) {
			throw new RuntimeException(
					"the line does not seem to be a valid arg list: " + line
							+ ". ");
		}
		return ret;
	}

	public static boolean hasMethod(String javaSource, String string)
			throws ParseException {
		CompilationUnit cu = parse(javaSource);
		return hasMethod(cu, string);
	}

	public static boolean hasMethodInvocatioin(CompilationUnit cu,
			final String string) {
		if (string == null || string.trim().length() == 0)
			return false;

		final StringBuilder re = new StringBuilder();

		VoidVisitorAdapter visitor = new VoidVisitorAdapter() {
			@Override
			public void visit(MethodCallExpr n, Object arg) {
				if (string.equals(n.getName())) {
					re.append(1);
					return;
				} else {
					super.visit(n, arg);
				}
			}
		};
		cu.accept(visitor, null);
		if (re.length() == 0)
			return false;
		else
			return true;
	}

	public static boolean hasMethod(CompilationUnit cu, final String string) {
		final StringBuilder sb = new StringBuilder();

		VoidVisitorAdapter visitor = new VoidVisitorAdapter() {
			@Override
			public void visit(MethodDeclaration n, Object arg) {
				if (n.getName().equals(string)) {
					sb.append(1);
					return;
				}
			}
		};
		cu.accept(visitor, null);
		if (sb.length() == 0)
			return false;
		else
			return true;
	}

	public static boolean hasMethod(CompilationUnit cu, final String name,
			final int modis, final String returnType, String paramList) {
		final StringBuilder sb = new StringBuilder();

		if (paramList == null)
			paramList = "";
		String formalParamList = addParamNamesPlaceHolder(paramList);

		final List<Parameter> params = parseParams(formalParamList);

		VoidVisitorAdapter visitor = new VoidVisitorAdapter() {
			@Override
			public void visit(MethodDeclaration n, Object arg) {
				if (n.getName().equals(name)) {
					int modifiers2 = n.getModifiers();
					if (modifiers2 == modis) {
						Type type = n.getType();
						if (type.toString().equals(returnType)) {
							List<Parameter> ps = n.getParameters();
							if (ps == null)
								ps = new ArrayList<Parameter>();
							if (paramsMatch(params, ps)) {
								sb.append(1);
								return;
							}
						}
					}
				}
			}
		};
		cu.accept(visitor, null);
		if (sb.length() == 0)
			return false;
		else
			return true;
	}

	/**
	 * 
	 * @param cu
	 * @param name
	 * @param modifiers
	 * @param returnType
	 * @param paramList
	 *            , parameter type only: String, final int, etc
	 * @return
	 */
	public static boolean hasMethod(CompilationUnit cu, final String name,
			final String modifiers, final String returnType, String paramList) {
		final int modis = parseModifiers(modifiers);
		return hasMethod(cu, name, modis, returnType, paramList);
	}

	/**
	 * @param paramList
	 * @return
	 */
	static String addParamNamesPlaceHolder(String paramList) {
		List<String> names = getNames(paramList);

		String formalParamList = "";
		for (int i = 0; i < names.size(); i++) {
			formalParamList += names.get(i) + " " + (char) ('a' + i) + ",";
		}

		if (formalParamList.endsWith(","))
			formalParamList = formalParamList.substring(0,
					formalParamList.length() - 1);
		return formalParamList;
	}

	/**
	 * @param paramList
	 * @return
	 */
	private static List<String> getNames(String paramList) {
		paramList = paramList.replace(' ', ',');
		String[] pams = paramList.split(",");
		List<String> names = new ArrayList<String>();
		for (int i = 0; i < pams.length; i++) {
			String p = pams[i].trim();
			if (p.length() > 0)
				names.add(p);
		}
		return names;
	}

	protected static boolean paramsMatch(List<Parameter> params,
			List<Parameter> ps) {
		if (params == ps)
			return true;

		if ((params == null && ps != null) || (params != null && ps == null))
			return false;

		if (params.size() != ps.size()) {
			return false;
		}

		for (int i = 0; i < params.size(); i++) {
			Parameter p1 = params.get(i);
			Parameter p2 = ps.get(i);
			if (!matchParams(p1, p2)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * 
	 * @param p1
	 * @param p2
	 * @return true if the parameters signature matches. Parameter name does not
	 *         matter.
	 */
	private static boolean matchParams(Parameter p1, Parameter p2) {
		if (p1.equals(p2))
			return true;

		if (p1.getModifiers() != p2.getModifiers())
			return false;

		if (!p1.getType().equals(p2.getType()))
			return false;

		// TODO: compare annotations

		return true;
	}

	private static int parseModifiers(String modifiers) {
		int ret = 0;

		List<String> names = getNames(modifiers);

		for (String m : names) {
			if (m.equals("public")) {
				ret |= ModifierSet.PUBLIC;
			} else if (m.equals("private")) {
				ret |= ModifierSet.PRIVATE;
			} else if (m.equals("protected")) {
				ret |= ModifierSet.PROTECTED;
			} else if (m.equals("static")) {
				ret |= ModifierSet.STATIC;
			} else if (m.equals("final")) {
				ret |= ModifierSet.FINAL;
			} else if (m.equals("final")) {
				ret |= ModifierSet.FINAL;
			} else if (m.equals("synchronized")) {
				ret |= ModifierSet.SYNCHRONIZED;
			}
		}

		return ret;
	}

	/**
	 * 
	 * @param params
	 *            Type1 p1, Type2 p2...
	 * @return Final Type1 p1, final Type2 p2...
	 */
	public static String addFinalToAllParams(String paramline) {
		if (paramline == null)
			return null;
		paramline = paramline.trim();
		if (paramline.length() == 0)
			return "";
		List<Parameter> params = parseParams(paramline);
		String s = "";
		for (Parameter p : params) {
			s += "final " + p.getType() + " " + p.getId().getName() + ", ";
		}

		return s.substring(0, s.lastIndexOf(", "));
	}

	/**
	 * box all primitive type declarations in a parameter list
	 * 
	 * @param paramline
	 * @return
	 */
	public static String boxPrimitiveTypesInParams(String paramline) {
		if (paramline == null)
			return null;
		paramline = paramline.trim();
		if (paramline.length() == 0)
			return "";
		List<Parameter> params = parseParams(paramline);
		String s = "";
		for (Parameter p : params) {
			String decl = p.getType() + " " + p.getId().getName();
			decl = cleanDeclPrimitive(decl);
			s += decl + ", ";
		}

		return s.substring(0, s.lastIndexOf(", "));
	}

	private static final String classTempForExpr = "class T {  {  f = %s ; } }";

	/**
	 * starting from the start of a string and find out the longest possible
	 * valid java expression
	 * 
	 * @param src
	 * @return the longest or "" in case of none
	 */
	public static String matchLongestPossibleExpr(String src) {
		if (src == null || src.trim().length() == 0)
			return "";

		src = src.trim();

		String expr = "";
		int i = src.length();
		for (; i > 0; i--) {
			expr = src.substring(0, i);
			if (expr.endsWith(";"))
				continue;
			String ss = String.format(classTempForExpr, expr);
			try {
				parse(ss);
				break;
			} catch (ParseException e) { // TODO perhaps modify Japa to create a
											// light weighted ParseException
				expr = "";
				continue;
			}
		}

		return expr.trim();
	}

	public static String getDefault(Parameter p) {
		String r = "";
		List<AnnotationExpr> annotations = p.getAnnotations();
		if (annotations == null)
			return null;
		for (AnnotationExpr an : annotations) {
			String name = an.getName().getName();
			if ("Default".equals(name)) {
				String string = an.toString();
				r = string.substring("@Default(".length(), string.length() - 1);
				break;
			}
		}
		return r;
	}

	/**
	 * change all primitive data types to the object wrapper type in the
	 * parameter list
	 * 
	 * @param decl
	 *            int i, int[] ia, etc
	 * @return the wrapper form
	 */
	public static String cleanDeclPrimitive(String decl) {
		decl = decl.trim();
		int i = decl.length();
		String var = "";
		String type = "";
		while (--i >= 0) {
			char c = decl.charAt(i);
			if (c == ' ' || c == '\t') {
				var = decl.substring(i + 1);
				type = decl.substring(0, i).trim();
				break;
			}
		}
		if ("int".equals(type)) {
			decl = "Integer " + var;
		} else if ("long".equals(type)) {
			decl = "Long " + var;
		} else if ("short".equals(type)) {
			decl = "Short " + var;
		} else if ("byte".equals(type)) {
			decl = "Byte " + var;
		} else if ("float".equals(type)) {
			decl = "Float " + var;
		} else if ("double".equals(type)) {
			decl = "Double " + var;
		} else if ("char".equals(type)) {
			decl = "Character " + var;
		} else if ("boolean".equals(type)) {
			decl = "Boolean " + var;
		}

		return decl;
	}

	public static final Pattern AS_PATTERN = Pattern
			.compile("(.*)->\\s*(\\w+)");

	 static final String classTempForStmt = "class T {  {   %s ; } }";
	 static final String varDeclTemp = "class T {  {  for (%s : collection){} } }";
	

	 // /**
	// *
	// * @param s
	// * something like: "int a = 1, int b"
	// * @return
	// */
	// public static String getFirstVarDeclare(String src) {
	// if (src == null || src.trim().length() == 0)
	// return "";
	//
	// src = src.trim();
	// final String[] ra = new String[1];
	//
	// VoidVisitorAdapter visitor = new VoidVisitorAdapter() {
	// public void visit(VariableDeclarationExpr n, Object arg) {
	// // Type type = n.getType();
	// List<VariableDeclarator> vars = n.getVars();
	// if (vars.size() > 1) {
	// // not good yet
	// }
	// else {
	// VariableDeclarator var = vars.get(0);
	// VariableDeclaratorId id = var.getId();
	// ra[0] = id.getName();
	// }
	// }
	// };
	//
	// String expr = "";
	// int i = src.length();
	// for (; i > 0; i--) {
	// expr = src.substring(0, i);
	// String ss = String.format(classTempForVarDef, expr);
	// try {
	// CompilationUnit cu = parse(ss);
	// cu.accept(visitor, null);
	// if (ra[0] != null)
	// break;
	// else
	// continue;
	// } catch (ParseException e) {
	// continue;
	// }
	// }
	//
	// return expr.trim();
	// }

	/**
	 * to extract the arg list part and optionally the local var part to take
	 * the result. Used in doBody to set the result to a local variable Can also
	 * be used to catch the tag invocation result in a local variable.
	 * 
	 * 
	 * @param s
	 *            arg list: a, 1 -> var, (1, 2, 3, "as") -> var
	 * @return an array of string, the first one being the arg list, the second
	 *         one, if exists, being the local var name
	 */
	public static String[] breakArgParts(String s) {
		Matcher m = AS_PATTERN.matcher(s);
		if (m.matches()) {
			String[] r = new String[2];
			r[0] = m.group(1);
			r[1] = m.group(2);
			return r;
		} else {
			return new String[] { s };
		}
	}

	public static boolean isValidExpr(String expr) {
		String ss = String.format(classTempForExpr, expr);
		try {
			parse(ss);
			return true;
		} catch (ParseException e) {
			return false;
		}
	}

	/**
	 * verify that line is a valid method declaration part, excluding method body and the {} 
	 * @param line: something like foo(int a, String b)
	 */
	public static void isValidMethDecl(String line) {
		final String classTempForMeth = "class T {  %s{} }";
		String classString = String.format(classTempForMeth, line);
		try {
			CompilationUnit cu = parse(classString);
			VoidVisitorAdapter visitor = new VoidVisitorAdapter() {
			};
			cu.accept(visitor, null);
		} catch (ParseException e) {
			throw new RuntimeException(
					"the line does not seem to be a valid method declaration: "
							+ line + ". Was expecting something like foo(int a, String b).");
		}

	}

	public static void isValidMethodCall(String src) {
		String classString = String.format(classTempForStmt, src);
		try {
			List<CodeNode> nodes = parseCode(classString);
			if (tyepMatch(nodes, 5, MethodCallExpr.class)) {
				// make sure there is only one statement at indentation level 4
				if (getIndentatioCount(nodes, 4) == 1) {
					return;
				}
			}

			throw new RuntimeException(
					"the line does not seem to be a valid method invocation expression: "
							+ src + ". Was expecting something like x.foo(a, b).");
//			
//			CompilationUnit cu = parse(classString);
//			final boolean[] good = new boolean[1];
//			good[0] = false;
//			VoidVisitorAdapter visitor = new VoidVisitorAdapter() {
//				@Override
//				public void visit(MethodCallExpr n, Object arg) {
//					good[0] = true;
//				}
//				
//				// TODO: should detect that the top most expression is a method call
//			};
//			cu.accept(visitor, null);
//			
//			if (!good[0])
//				throw new RuntimeException(
//						"the line does not seem to be a valid method invocation expression: "
//								+ src + ". Was expecting something like x.foo(a, b).");
//			else 
//				if (!methPattern.matcher(src).matches())
//					throw new RuntimeException(
//							"the line does not seem to be a valid method invocation expression: "
//									+ src + ". Was expecting something like x.foo(a, b).");
//				
		} catch (RuntimeException e) {
			throw new RuntimeException(
					"the line does not seem to be a valid method invocation expression: "
							+ src + ". Was expecting something like foo(a, b).");
		}
	}

	private static int getIndentatioCount(List<CodeNode> nodes, int i) {
		int c = 0;
		for (CodeNode n : nodes) {
			if (n.nestLevel == i)
				c++;
		}
		return c;
	}

	private static boolean tyepMatch(List<CodeNode> nodes, int pos,
			Class<? extends Node> targetClass) {
		return nodes.size() > pos && nodes.get(pos).node.getClass() == targetClass;
	}

	public static boolean isValidSingleVarDecl(String src) {
		String classString = String.format(varDeclTemp, src);
		try {
			CompilationUnit cu = parse(classString);
			return true;
		} catch (ParseException e) {
			return false;
		}
	}
	
	public final static Pattern methPattern = Pattern.compile("[\\w\\.\\$\\s]+\\(.*\\)");
	
	public static boolean isValidMethodCallSimple(String src) {
		if (src == null )
			return false;
		src = src.trim();
		if (src.length() < 3)
			return false;
		if (!methPattern.matcher(src).matches())
			return false;
		
		return true;
	}
	
	public static String IF_PREDICATE = "class T {  {  if %s {} } }";
	public static String IF_PREDICATE_BRACE = "class T {  {  if %s } } }";
	public static String IF_PREDICATE_OPEN = "class T {  {  if(%s) {} } }";
	public static String IF_PREDICATE_OPEN_BRACE = "class T {  {  if (%s) } } }";
	

	/**
	 * tell if the input is a regular if predicate with () and optionally a {
	 * @author Bing Ran (bing.ran@hotmail.com)
	 * @param part
	 * @return
	 */
	public static boolean isIf(String part) {
		String classString = String.format(IF_PREDICATE, part);
		try {
			CompilationUnit cu = parse(classString);
			return true;
		} catch (ParseException e) {
			classString = String.format(IF_PREDICATE_BRACE, part);
			try {
				CompilationUnit cu = parse(classString);
				return true;
			} catch (ParseException e1) {
				return false;
			}
		}
		
	}
	
	
	public static boolean isOpenIf(String part) {
		part = part.trim();
		if (part.endsWith("{"))
			part = part.substring(0, part.length() - 1);
		String classString = String.format(IF_PREDICATE_OPEN, part);
		try {
			CompilationUnit cu = parse(classString);
			return true;
		} catch (ParseException e) {
			return false;
		}
	}
	
	
	/**
	 * 
	 * @param part the part in a for(%s) {}
	 * @return true if the input is a valid part in the for java 5 for loop predicative
	 */
	public static boolean isValidForLoopPredicate(String part){
		String classString = String.format(TEMP_FOR_HEADER, part);
		try {
			CompilationUnit cu = parse(classString);
			return true;
		} catch (ParseException e) {
			return false;
		}

	}
	
	public static final String TEMP_FOR_HEADER =  "class T {  {  for (%s){} } }";
	
	public static class CodeNode{
		public int nestLevel;
		public Node node;
		public CodeNode(int nestLevel, Node node) {
			this.nestLevel = nestLevel;
			this.node = node;
		}
		
	}
	
	public static class BinaryOrExpr extends Expression {
		BinaryExpr expr;
		
		
		public BinaryOrExpr(BinaryExpr expr) {
			this.expr = expr;
		}

		@Override
		public <R, A> R accept(GenericVisitor<R, A> arg0, A arg1) {
			return null;
		}

		@Override
		public <A> void accept(VoidVisitor<A> arg0, A arg1) {
		}
		
	}
	
	
	public static List<CodeNode> parseCode(String code) {
		try{
			final List<CodeNode> nodes = new LinkedList();
			CompilationUnit cu = parse(code);
			VoidVisitorAdapter visitor = new VoidVisitorAdapter() {
				int nested = 0;
				@Override
				public void visit(AnnotationDeclaration n, Object arg) {
					
					nodes.add(new CodeNode(nested++, n)); super.visit(n, arg); nested--;
				}

				@Override
				public void visit(AnnotationMemberDeclaration n, Object arg) {

					nodes.add(new CodeNode(nested++, n)); super.visit(n, arg); nested--;
				}

				@Override
				public void visit(ArrayAccessExpr n, Object arg) {

					nodes.add(new CodeNode(nested++, n)); super.visit(n, arg); nested--;
				}

				@Override
				public void visit(ArrayCreationExpr n, Object arg) {

					nodes.add(new CodeNode(nested++, n)); super.visit(n, arg); nested--;
				}

				@Override
				public void visit(ArrayInitializerExpr n, Object arg) {

					nodes.add(new CodeNode(nested++, n)); super.visit(n, arg); nested--;
				}

				@Override
				public void visit(AssertStmt n, Object arg) {

					nodes.add(new CodeNode(nested++, n)); super.visit(n, arg); nested--;
				}

				@Override
				public void visit(AssignExpr n, Object arg) {

					nodes.add(new CodeNode(nested++, n)); super.visit(n, arg); nested--;
				}

				@Override
				public void visit(BinaryExpr n, Object arg) {
					if (n.getOperator() == Operator.binOr) {
						nodes.add(new CodeNode(nested++, new BinaryOrExpr(n)));
					}
					else {
						nodes.add(new CodeNode(nested++, n));
					}
					super.visit(n, arg); 
					nested--;
				}

				@Override
				public void visit(BlockComment n, Object arg) {

					nodes.add(new CodeNode(nested++, n)); super.visit(n, arg); nested--;
				}

				@Override
				public void visit(BlockStmt n, Object arg) {

					nodes.add(new CodeNode(nested++, n)); super.visit(n, arg); nested--;
				}

				@Override
				public void visit(BooleanLiteralExpr n, Object arg) {

					nodes.add(new CodeNode(nested++, n)); super.visit(n, arg); nested--;
				}

				@Override
				public void visit(BreakStmt n, Object arg) {

					nodes.add(new CodeNode(nested++, n)); super.visit(n, arg); nested--;
				}

				@Override
				public void visit(CastExpr n, Object arg) {

					nodes.add(new CodeNode(nested++, n)); super.visit(n, arg); nested--;
				}

				@Override
				public void visit(CatchClause n, Object arg) {

					nodes.add(new CodeNode(nested++, n)); super.visit(n, arg); nested--;
				}

				@Override
				public void visit(CharLiteralExpr n, Object arg) {

					nodes.add(new CodeNode(nested++, n)); super.visit(n, arg); nested--;
				}

				@Override
				public void visit(ClassExpr n, Object arg) {

					nodes.add(new CodeNode(nested++, n)); super.visit(n, arg); nested--;
				}

				@Override
				public void visit(ClassOrInterfaceDeclaration n, Object arg) {

					nodes.add(new CodeNode(nested++, n)); super.visit(n, arg); nested--;
				}

				@Override
				public void visit(ClassOrInterfaceType n, Object arg) {

					nodes.add(new CodeNode(nested++, n)); super.visit(n, arg); nested--;
				}

				@Override
				public void visit(CompilationUnit n, Object arg) {

					nodes.add(new CodeNode(nested++, n)); super.visit(n, arg); nested--;
				}

				@Override
				public void visit(ConditionalExpr n, Object arg) {

					nodes.add(new CodeNode(nested++, n)); super.visit(n, arg); nested--;
				}

				@Override
				public void visit(ConstructorDeclaration n, Object arg) {

					nodes.add(new CodeNode(nested++, n)); super.visit(n, arg); nested--;
				}

				@Override
				public void visit(ContinueStmt n, Object arg) {

					nodes.add(new CodeNode(nested++, n)); super.visit(n, arg); nested--;
				}

				@Override
				public void visit(DoStmt n, Object arg) {

					nodes.add(new CodeNode(nested++, n)); super.visit(n, arg); nested--;
				}

				@Override
				public void visit(DoubleLiteralExpr n, Object arg) {

					nodes.add(new CodeNode(nested++, n)); super.visit(n, arg); nested--;
				}

				@Override
				public void visit(EmptyMemberDeclaration n, Object arg) {

					nodes.add(new CodeNode(nested++, n)); super.visit(n, arg); nested--;
				}

				@Override
				public void visit(EmptyStmt n, Object arg) {

					nodes.add(new CodeNode(nested++, n)); super.visit(n, arg); nested--;
				}

				@Override
				public void visit(EmptyTypeDeclaration n, Object arg) {

					nodes.add(new CodeNode(nested++, n)); super.visit(n, arg); nested--;
				}

				@Override
				public void visit(EnclosedExpr n, Object arg) {

					nodes.add(new CodeNode(nested++, n)); super.visit(n, arg); nested--;
				}

				@Override
				public void visit(EnumConstantDeclaration n, Object arg) {

					nodes.add(new CodeNode(nested++, n)); super.visit(n, arg); nested--;
				}

				@Override
				public void visit(EnumDeclaration n, Object arg) {

					nodes.add(new CodeNode(nested++, n)); super.visit(n, arg); nested--;
				}

				@Override
				public void visit(ExplicitConstructorInvocationStmt n,
						Object arg) {

					nodes.add(new CodeNode(nested++, n)); super.visit(n, arg); nested--;
				}

				@Override
				public void visit(ExpressionStmt n, Object arg) {

					nodes.add(new CodeNode(nested++, n)); super.visit(n, arg); nested--;
				}

				@Override
				public void visit(FieldAccessExpr n, Object arg) {

					nodes.add(new CodeNode(nested++, n)); super.visit(n, arg); nested--;
				}

				@Override
				public void visit(FieldDeclaration n, Object arg) {

					nodes.add(new CodeNode(nested++, n)); super.visit(n, arg); nested--;
				}

				@Override
				public void visit(ForeachStmt n, Object arg) {

					nodes.add(new CodeNode(nested++, n)); super.visit(n, arg); nested--;
				}

				@Override
				public void visit(ForStmt n, Object arg) {

					nodes.add(new CodeNode(nested++, n)); super.visit(n, arg); nested--;
				}

				@Override
				public void visit(IfStmt n, Object arg) {

					nodes.add(new CodeNode(nested++, n)); super.visit(n, arg); nested--;
				}

				@Override
				public void visit(ImportDeclaration n, Object arg) {

					nodes.add(new CodeNode(nested++, n)); super.visit(n, arg); nested--;
				}

				@Override
				public void visit(InitializerDeclaration n, Object arg) {

					nodes.add(new CodeNode(nested++, n)); super.visit(n, arg); nested--;
				}

				@Override
				public void visit(InstanceOfExpr n, Object arg) {

					nodes.add(new CodeNode(nested++, n)); super.visit(n, arg); nested--;
				}

				@Override
				public void visit(IntegerLiteralExpr n, Object arg) {

					nodes.add(new CodeNode(nested++, n)); super.visit(n, arg); nested--;
				}

				@Override
				public void visit(IntegerLiteralMinValueExpr n, Object arg) {

					nodes.add(new CodeNode(nested++, n)); super.visit(n, arg); nested--;
				}

				@Override
				public void visit(JavadocComment n, Object arg) {

					nodes.add(new CodeNode(nested++, n)); super.visit(n, arg); nested--;
				}

				@Override
				public void visit(LabeledStmt n, Object arg) {

					nodes.add(new CodeNode(nested++, n)); super.visit(n, arg); nested--;
				}

				@Override
				public void visit(LineComment n, Object arg) {

					nodes.add(new CodeNode(nested++, n)); super.visit(n, arg); nested--;
				}

				@Override
				public void visit(LongLiteralExpr n, Object arg) {

					nodes.add(new CodeNode(nested++, n)); super.visit(n, arg); nested--;
				}

				@Override
				public void visit(LongLiteralMinValueExpr n, Object arg) {

					nodes.add(new CodeNode(nested++, n)); super.visit(n, arg); nested--;
				}

				@Override
				public void visit(MarkerAnnotationExpr n, Object arg) {

					nodes.add(new CodeNode(nested++, n)); super.visit(n, arg); nested--;
				}

				@Override
				public void visit(MemberValuePair n, Object arg) {

					nodes.add(new CodeNode(nested++, n)); super.visit(n, arg); nested--;
				}

				@Override
				public void visit(MethodCallExpr n, Object arg) {

					nodes.add(new CodeNode(nested++, n)); super.visit(n, arg); nested--;
				}

				@Override
				public void visit(MethodDeclaration n, Object arg) {

					nodes.add(new CodeNode(nested++, n)); super.visit(n, arg); nested--;
				}

				@Override
				public void visit(NameExpr n, Object arg) {

					nodes.add(new CodeNode(nested++, n)); super.visit(n, arg); nested--;
				}

				@Override
				public void visit(NormalAnnotationExpr n, Object arg) {

					nodes.add(new CodeNode(nested++, n)); super.visit(n, arg); nested--;
				}

				@Override
				public void visit(NullLiteralExpr n, Object arg) {

					nodes.add(new CodeNode(nested++, n)); super.visit(n, arg); nested--;
				}

				@Override
				public void visit(ObjectCreationExpr n, Object arg) {

					nodes.add(new CodeNode(nested++, n)); super.visit(n, arg); nested--;
				}

				@Override
				public void visit(PackageDeclaration n, Object arg) {

					nodes.add(new CodeNode(nested++, n)); super.visit(n, arg); nested--;
				}

				@Override
				public void visit(Parameter n, Object arg) {

					nodes.add(new CodeNode(nested++, n)); super.visit(n, arg); nested--;
				}

				@Override
				public void visit(PrimitiveType n, Object arg) {

					nodes.add(new CodeNode(nested++, n)); super.visit(n, arg); nested--;
				}

				@Override
				public void visit(QualifiedNameExpr n, Object arg) {

					nodes.add(new CodeNode(nested++, n)); super.visit(n, arg); nested--;
				}

				@Override
				public void visit(ReferenceType n, Object arg) {

					nodes.add(new CodeNode(nested++, n)); super.visit(n, arg); nested--;
				}

				@Override
				public void visit(ReturnStmt n, Object arg) {

					nodes.add(new CodeNode(nested++, n)); super.visit(n, arg); nested--;
				}

				@Override
				public void visit(SingleMemberAnnotationExpr n, Object arg) {

					nodes.add(new CodeNode(nested++, n)); super.visit(n, arg); nested--;
				}

				@Override
				public void visit(StringLiteralExpr n, Object arg) {

					nodes.add(new CodeNode(nested++, n)); super.visit(n, arg); nested--;
				}

				@Override
				public void visit(SuperExpr n, Object arg) {

					nodes.add(new CodeNode(nested++, n)); super.visit(n, arg); nested--;
				}

				@Override
				public void visit(SwitchEntryStmt n, Object arg) {

					nodes.add(new CodeNode(nested++, n)); super.visit(n, arg); nested--;
				}

				@Override
				public void visit(SwitchStmt n, Object arg) {

					nodes.add(new CodeNode(nested++, n)); super.visit(n, arg); nested--;
				}

				@Override
				public void visit(SynchronizedStmt n, Object arg) {

					nodes.add(new CodeNode(nested++, n)); super.visit(n, arg); nested--;
				}

				@Override
				public void visit(ThisExpr n, Object arg) {

					nodes.add(new CodeNode(nested++, n)); super.visit(n, arg); nested--;
				}

				@Override
				public void visit(ThrowStmt n, Object arg) {

					nodes.add(new CodeNode(nested++, n)); super.visit(n, arg); nested--;
				}

				@Override
				public void visit(TryStmt n, Object arg) {

					nodes.add(new CodeNode(nested++, n)); super.visit(n, arg); nested--;
				}

				@Override
				public void visit(TypeDeclarationStmt n, Object arg) {

					nodes.add(new CodeNode(nested++, n)); super.visit(n, arg); nested--;
				}

				@Override
				public void visit(TypeParameter n, Object arg) {

					nodes.add(new CodeNode(nested++, n)); super.visit(n, arg); nested--;
				}

				@Override
				public void visit(UnaryExpr n, Object arg) {

					nodes.add(new CodeNode(nested++, n)); super.visit(n, arg); nested--;
				}

				@Override
				public void visit(VariableDeclarationExpr n, Object arg) {

					nodes.add(new CodeNode(nested++, n)); super.visit(n, arg); nested--;
				}

				@Override
				public void visit(VariableDeclarator n, Object arg) {

					nodes.add(new CodeNode(nested++, n)); super.visit(n, arg); nested--;
				}

				@Override
				public void visit(VariableDeclaratorId n, Object arg) {

					nodes.add(new CodeNode(nested++, n)); super.visit(n, arg); nested--;
				}

				@Override
				public void visit(VoidType n, Object arg) {

					nodes.add(new CodeNode(nested++, n)); super.visit(n, arg); nested--;
				}

				@Override
				public void visit(WhileStmt n, Object arg) {

					nodes.add(new CodeNode(nested++, n)); super.visit(n, arg); nested--;
				}

				@Override
				public void visit(WildcardType n, Object arg) {

					nodes.add(new CodeNode(nested++, n)); super.visit(n, arg); nested--;
				}
				
			};
			cu.accept(visitor, null);
			return nodes;
		} catch (ParseException e) {
			throw new RuntimeException(
					"invalid Java code: " + code + ". " + e);
		}

	}
}
