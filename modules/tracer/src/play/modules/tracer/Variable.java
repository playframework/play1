package play.modules.tracer;

import javassist.bytecode.LocalVariableAttribute;

public class Variable {
	public LocalVariableAttribute localVariableAttribute;
	
	public int startLine;
	public int endLine;
	public String name;
	public int modifier;
	public String classFQDN;
	public Class<?> klass;
	public SerializedObject value;
	public boolean isParam;
	public boolean isLocal;
	
	public Object that;
	
	public int index;
	public int localVariableTableIndex;

	public String declaringClassFQDN;
	public Class<?> declaringClass;
	
	public Variable() {
		this(null, -1);
	}
	
	public Variable(LocalVariableAttribute lattr, int index) {
		this.localVariableAttribute = lattr;
		this.localVariableTableIndex = index;
	}
	
	public void mutate(Object value) {
		this.that = value;
	}
	
	public void mutate(boolean b) {
		mutate(new Boolean(b));
	}
	
	public void mutate(byte b) {
		mutate(new Byte(b));
	}
	
	public void mutate(short s) {
		mutate(new Short(s));
	}
	
	public void mutate(int i) {
		mutate(new Integer(i));
	}
	
	public void mutate(long l) {
		mutate(new Long(l));
	}
	
	public void mutate(float f) {
		mutate(new Float(f));
	}
	
	public void mutate(double d) {
		mutate(new Double(d));
	}
	
	public int getStartPC() {
		if(localVariableAttribute == null)
			return -1;
		return localVariableAttribute.startPc(localVariableTableIndex);
	}
	
	public int getEndPC() {
		if(localVariableAttribute == null)
			return Integer.MAX_VALUE;
		return localVariableAttribute.startPc(localVariableTableIndex) + localVariableAttribute.codeLength(localVariableTableIndex);
	}
	
	public String toJSON() {
		StringBuffer sb = new StringBuffer("{");
		sb.append("\"name\":\"").append(name).append("\",");
		sb.append("\"startLine\":").append(startLine).append(",");
		sb.append("\"endLine\":").append(endLine).append(",");
		sb.append("\"isParam\":").append(isParam).append(",");
		sb.append("\"isLocal\":").append(isLocal).append(",");
		sb.append("\"value\":").append(value.toJSON()).append(",");
		sb.append("\"modifier\":").append(modifier);
		return sb.append("}").toString();
	}
}