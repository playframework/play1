package cn.bran.japid.compiler;

import java.util.ArrayList;
import java.util.List;

public class Tag {

	static final String ROOT_TAGNAME = "_root";
	public String tagName;
	public int startLine;
	public boolean hasBody; // tga invoke with a body part
	// bran: put everything in the args tag in it
	public String callbackArgs = null;
	// public StringBuffer bodyBuffer = new StringBuffer(2000);
	// each line contains a line of text in the body of a tag scope.
	List<String> bodyTextList = new ArrayList<String>();
	{
		bodyTextList.add("");
	}
	public String innerClassName;
	public String args = "";
	public int tagIndex;

	// null == not named args
	List<NamedArg> namedArgs = null;
	
	public String getBodyText() {
		if (!hasBody)
			return null;
		StringBuffer sb = new StringBuffer(2000);
		for (String s : bodyTextList) {
			sb.append(s).append('\n');
		}
		String string = sb.toString();
		return string.substring(0, string.length() - 1);
	}

	public String getTagVarName() {
		return "_" + tagName.replace('.', '_').replace('/', '_') + tagIndex;
	}

	public String getBodyVar() {
		return getTagVarName() + "DoBody";
	}

	@Override
	public String toString() {
		return tagName;
	}
	public boolean isRoot() {
		return tagName.equals(ROOT_TAGNAME);
	}
	
	public static class TagIf extends Tag {
		public TagIf(String expr, int line) {
			super();
			args = expr;
			tagName = "if";
			super.startLine = line;
			hasBody = true;
		}

	}
	/**
	 *  the `def, `set tag that end up as methods thus a new scope for tags inside
	 * @author Bing Ran<bing_ran@hotmail.com>
	 *
	 */
	public static class TagInTag extends Tag {
		public List<Tag> tags = new ArrayList<Tag>();
	}

	/**
	 * the def tag
	 * @author Bing Ran<bing_ran@hotmail.com>
	 *
	 */
	public static class TagDef extends TagInTag {
		public TagDef() {
			super();
			tagName = "def";
			hasBody = true;
		}
		
	}
	/**
	 * the set tag
	 * @author Bing Ran<bing_ran@hotmail.com>
	 *
	 */
	public static class TagSet extends TagInTag {
		public TagSet() {
			super();
			tagName = "set";
		}
	}
	public boolean argsNamed() {
		return (namedArgs != null && namedArgs.size() > 0);
	}
}