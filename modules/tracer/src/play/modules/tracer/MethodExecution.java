/**
 * 
 */
package play.modules.tracer;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unchecked")
public class MethodExecution extends BodyExecution {
	public String name;
	public Class klass;
	public Object that;
	public SerializedObject context;
	
	public Map<Integer, Variable[]> readAccessesByLine, writeAccessesByLine;
	
	public MethodExecution(Execution parent, int line, Class klass, String name, Object that, Variable[] variables, Map<Integer, Variable[]> readAccessesByLine, Map<Integer, Variable[]> writeAccessesByLine) {
		super(parent, line);
		this.name = name;
		this.that = that;
		this.klass = klass;
		context = SerializedObject.serialize(name, that, klass, true);
		for(Variable variable : variables) {
			if(Modifier.isStatic(variable.modifier) && !variable.declaringClass.equals(klass)) { // javassist caveat fix : javassist can not access inherited static fields
				try {
					// TODO: need fix ?
					//System.out.println("MethodExecution.MethodExecution() ::: javassist caveat fix : javassist can not access inherited static fields (declaringClass="+ klass.getName()+ ", field=" + variable.name + ", previous=" + variable.that + ")");
					for(Field field : getFields(klass)) {
						field.setAccessible(true);
					}
					Field field = getField(klass, variable.name);
					field.setAccessible(true);
					variable.that = field.get(null);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			variable.value = SerializedObject.serialize(variable.name, variable.that, variable.klass, true);
			this.variables.add(variable);
		}
		this.readAccessesByLine = readAccessesByLine;
		this.writeAccessesByLine = writeAccessesByLine;
	}
	
	protected String partialToJSON() {
		StringBuffer sb = new StringBuffer();
		sb.append("\"name\":\"").append(name).append("\",");
		sb.append("\"klass\":\"").append(klass.getCanonicalName()).append("\",");
		String superPartialToJSON = super.partialToJSON();
		if(superPartialToJSON != null)
			sb.append(superPartialToJSON).append(",");
		sb.append("\"context\":").append(context.toJSON(true));
		return sb.toString();
	}
	
	private static Field getField(Class<?> klass, String name) {
		for(Field field: getFields(klass))
			if(field.getName().equals(name))
				return field;
		return null;
	}
	
	private static List<Field> getFields(Class<?> klass) {
		return getFields(klass, true);
	}
	
	private static List<Field> getFields(Class<?> klass, boolean getPrivate) {
		ArrayList<Field> fields = new ArrayList<Field>();
		for(Field field : klass.getDeclaredFields()) {
			if(!Modifier.isPrivate(field.getModifiers()) || getPrivate)
				fields.add(field);
		}
		if(klass.getSuperclass() != null)
			fields.addAll(getFields(klass.getSuperclass(), false));
		return fields;
	}
}