/**
 * 
 */
package play.modules.tracer;

import groovy.lang.Binding;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import play.Logger;
import play.modules.tracer.plugin.TracerEnhancer.TracerEnhanced;

@SuppressWarnings("unchecked")
public class LineExecution extends Execution {
	boolean afterLoop = false;
	
	public static class Assignment {
		public String name;
		public int modifier;
		public int line;
		public boolean isLocal;
		public SerializedObject value;
		
		public Assignment(String variable, Object value, Class klass, int line, boolean isLocal) {
			this.name = variable;
			this.line = line;
			this.value = SerializedObject.serialize(name, value, klass, true);
			this.isLocal = isLocal;
		}
		
		public String toJSON() {
			StringBuffer sb = new StringBuffer("{");
			sb.append("\"name\": \"").append(name).append("\",");
			sb.append("\"modifier\":").append(modifier).append(",");
			sb.append("\"isLocal\":").append(isLocal).append(",");
			sb.append("\"value\":").append(value.toJSON());
			sb.append("}");
			return sb.toString();
		}
	}

	public List<Assignment> assignments = new ArrayList<Assignment>();
	
	public LineExecution(Execution parent, int line) {
		super(parent, line);
	}
	
	public void assign(Variable variable) {
		Assignment temp = new LineExecution.Assignment(variable.name, variable.that, variable.klass, line, variable.isLocal);
		if(variable.value == null || !variable.value.equals(temp.value)) {
			assignments.add(temp);
			variable.value = temp.value;
		}
	}
	
	public void assignIfModified(String variable, Object value, Class klass, int line, boolean isLocal) {
		if(klass.isAnnotationPresent(TracerEnhanced.class)) {
			//TODO does not work for the moment
			return;
		}
		BodyExecution bexec = (BodyExecution) parent;
		Variable var = bexec.findVariable(variable, isLocal, line);
		if(var == null) {
			// we are probably in a template
			Logger.debug("LineExecution.assignIfModified(): adding variable '%s' as we are in a template", variable);
			var = new Variable();
			var.name = variable;
			var.that = value;
			var.startLine = -1;
			var.endLine = Integer.MAX_VALUE;
			var.klass = klass;
			bexec.variables.add(var);
			assign(var);
		} else {
			Assignment temp = new Assignment(variable, value, klass, line, isLocal);
			if(!temp.value.equals(var.value)) {
				assignments.add(temp);
				var.value = temp.value;
				Logger.debug("LineExecution.assignIfModified(): '%s' has changed", variable);
			}
		}
	}
	
	public void registerLocalVariable(String name, int startLine, int endLine,
			boolean isParam) {
		throw new RuntimeException("cannot register local variable in a lineexecution");
	}
	
	
	
	protected String partialToJSON() {
		StringBuffer sb = new StringBuffer("\"assignments\": [");
		if(assignments.size() > 0) {
			sb.append(assignments.get(0).toJSON());
			for(int i = 1; i < assignments.size(); i++) {
				sb.append(",").append(assignments.get(i).toJSON());
			}
		}
		sb.append("]");
		return sb.toString();
	}

	public void end(Binding binding) {
		if(parent instanceof TemplateExecution) {
			Map<String, Object> bindingVariables = binding.getVariables();
			for(Entry<String, Object> entry : bindingVariables.entrySet()) {
				Object object = entry.getValue();
				Class<?> klass = object != null ? object.getClass() : Object.class;
				String name = entry.getKey();
				assignIfModified(name, object, klass, line, false);
			}
		}
		end();
	}
	
	public void end(Variable[] readVars, Variable[] writtenVars) {
		Logger.debug("LineExecution.end() --- line="+line);
		if(parent instanceof MethodExecution) {
			MethodExecution mexec = (MethodExecution) parent;
			Logger.debug("ending line %s in mexec = %s", line, parent);
			for(Variable variable : mexec.readAccessesByLine.get(line)) {
				Logger.debug("LineExecution.end(): read '%s' at line "+line, variable.name);
				assignIfModified(variable.name, variable.that, variable.klass, line, variable.isLocal);
			}
			for(Variable variable : mexec.writeAccessesByLine.get(line)) {
				Logger.debug("LineExecution.end(): write '%s' at line "+line, variable.name);
				assign(variable);
			}
		}
		end();
	}
}