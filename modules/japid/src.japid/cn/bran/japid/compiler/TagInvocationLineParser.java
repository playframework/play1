package cn.bran.japid.compiler;

import japa.parser.ast.body.Parameter;

import java.util.List;

import cn.bran.japid.util.StringUtils;

/**
 * parse `tag (t, v) | U u
 * 
 * @author Bing Ran<bing_ran@hotmail.com>
 * 
 */
public class TagInvocationLineParser {

	/**
	 * TODO: use stricter syntax checker. The callback border symbol is not
	 * strictly picked up based on Java grammar, etc.
	 * 
	 * @param line
	 * @return
	 */
	public Tag parse(String line) {
		// String original = line;
		Tag tag = new Tag();

		// get tag name
		for (int i = 0; i < line.length(); i++) {
			char c = line.charAt(i);
			if (Character.isJavaIdentifierPart(c) || c == '.' || c == '/') {
				continue;
			} else {
				if (Character.isWhitespace(c) || c == '(' || c == '|') {
					tag.tagName = line.substring(0, i).replace('/', '.');
					if (tag.tagName.startsWith(".."))
						tag.tagName = tag.tagName.substring(1);
					line = line.substring(i).trim();
					break;
				} else {
					throw new RuntimeException("invalid character in the tag invocation line " + line + ": [" + c + "]");
				}
			}
		}
		if (tag.tagName == null) {
			// there was no end char in the line. so the whole line is the tag
			// name
			tag.tagName = line.replace('/', '.');
			return tag;
		}

		if (tag.tagName.equals("def")) {
			tag = new Tag.TagDef();
			if (!line.endsWith(")"))
				line += "()";
			JavaSyntaxTool.isValidMethDecl(line);
			tag.args = line;
			return tag;
		} else if (tag.tagName.equals("set")) {
			tag = new Tag.TagSet();
			tag.args = line;
			return tag;
		} else if (tag.tagName.equals("get")) {
			tag.args = line;
			return tag;
		}

		// let's parse the closure params
		int vertline = line.lastIndexOf('|');
		if (vertline < 0) {
			List<String> args = JavaSyntaxTool.parseArgs(line);
			tag.args = StringUtils.join(args, ",");
			parseNamedArgs(tag);
		} else {
			// need to find out the real meaning of the vertical bar
			String left = line.substring(0, vertline);
			String right = line.substring(vertline);
			if (right.length() > 1)
				right = right.substring(1);
			else
				right = "";

			if (JavaSyntaxTool.isValidParamList(right))
				parseTagCallWithCallback(line, tag);
			else // the bar may be part of a argument
			{
				List<String> args = JavaSyntaxTool.parseArgs(line);
				tag.args = StringUtils.join(args, ",");
				parseNamedArgs(tag);
			}
		}

		if ("Each".equals(tag.tagName) || "each".equals(tag.tagName)) {
			if (!tag.hasBody) {
				throw new RuntimeException("The Each tag must have a body. Invalid statement: " + line);
			}
		}
		return tag;
	}

	private void parseTagCallWithCallback(String line, Tag tag) {
		int vertline = line.lastIndexOf('|');
		if (vertline >= 0) {
			String closureArgs = line.substring(vertline + 1).trim();
			// test syntax
			JavaSyntaxTool.parseParams(closureArgs);
			tag.callbackArgs = closureArgs;
			tag.hasBody = true;
			line = line.substring(0, vertline).trim();

			if (line.length() == 0)
				return;
			else {
				// parse args.
				char firstC = line.charAt(0);
				char lastC = line.charAt(line.length() - 1);
				if ('(' == firstC) {
					if (')' != lastC) {
						throw new RuntimeException("The tag argument part is not valid: parenthesis is not paired.");
					} else {
						tag.args = line.substring(1, line.length() - 1);
					}
				} else {
					tag.args = line;
				}

				parseNamedArgs(tag);
			}
		} else
			throw new RuntimeException("tag args not valid: " + line);
	}

	private void parseNamedArgs(Tag tag) {
		// check for grammar and named arguments
		List<NamedArg> args = JavaSyntaxTool.parseNamedArgs(tag.args);
		if (args.size() > 0) {
			tag.namedArgs = args;
			// let reformat the args to named("a1", a), named("a2", 12), etc
			String ar = "";
			for (NamedArg na : args) {
				ar += na.toNamed() + ", ";
			}
			if (ar.endsWith(", ")) {
				ar = ar.substring(0, ar.length() - 2);
			}
			tag.args = ar;
		}
	}
}
