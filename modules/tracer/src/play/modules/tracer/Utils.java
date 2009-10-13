package play.modules.tracer;

import java.lang.reflect.Modifier;

public class Utils {
	public static Variable findVariable(Variable[] variables, String name, boolean isLocal, boolean isStatic) {
		for(Variable variable : variables) {
			if(variable.isLocal == isLocal && Modifier.isStatic(variable.modifier) == isStatic && variable.name.equals(name)) {
				return variable;
			}
		}
		return null;
	}

	public static String escapeJSON(String s) {
		return "\"" + (s == null ? "null" : s.replace("\\", "\\\\").replace("\"", "\\\"")).replace("\n", "\\n") + "\"";
	}
}
