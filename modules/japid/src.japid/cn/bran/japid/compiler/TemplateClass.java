package cn.bran.japid.compiler;

import java.util.List;

import japa.parser.ParseException;
import japa.parser.ast.CompilationUnit;
import japa.parser.ast.body.TypeDeclaration;

/**
 * Wrap the compilation unit from the generated class. For use in testing.
 * 
 * @author Bing Ran<bing_ran@hotmail.com>
 *
 */
public class TemplateClass {
	CompilationUnit cu;
	String name;
	
	public TemplateClass(String src) {
		try {
			this.cu = JavaSyntaxTool.parse(src);
			List<TypeDeclaration> types = cu.getTypes();
			name = types.get(0).getName();
		} catch (ParseException e) {
			String m = e.getMessage();
			m.substring(0, m.indexOf('\n'));
			throw new RuntimeException(m.substring(0, m.indexOf('\n')));
		}
	}

	public String getName() {
		return this.name;
	}
}
