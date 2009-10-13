package play.modules.tracer;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Very basic serialization
 * @author sgo
 *
 */
@SuppressWarnings("unchecked")
public class SerializedObject {
	public static int MAX_DEPTH = 3;
	
	public String name;
	public Class klass;
	public Object object;
	
	public SerializedType serializedType;
	
	// cache
	private String json;
	
	public String value;
	
	public Map<String, SerializedObject> staticFields = new HashMap<String, SerializedObject>();
	public Map<String, SerializedObject> fields = new HashMap<String, SerializedObject>();
	
	public String toJSON() {
		return toJSON(false);
	}
	
	public boolean equals(SerializedObject so) {
		return toJSON().equals(so.toJSON());
	}
	
	public String toJSON(boolean includeStaticIfNull) {
		if(serializedType.equals(SerializedType.NULL) && !includeStaticIfNull) return "\"null\"";
		if(json != null) return json;
		if(serializedType.equals(SerializedType.SIMPLE)) return Utils.escapeJSON(value);
		boolean array = serializedType.equals(SerializedType.COLLECTION) || serializedType.equals(SerializedType.ARRAY);
		
		StringBuffer sb = new StringBuffer(array ? "[" : "{");
		boolean first = true;
		
		if(serializedType.equals(SerializedType.OBJECT) || serializedType.equals(SerializedType.NULL) && includeStaticIfNull) {
			for(Entry<String, SerializedObject> entry : staticFields.entrySet()) {
				if(!first)
					sb.append(",");
				else first = false;
				sb.append(Utils.escapeJSON(entry.getKey())).append(":").append(entry.getValue().toJSON());
			}
		}

		for(Entry<String, SerializedObject> entry : fields.entrySet()) {
			if(!first)
				sb.append(",");
			else first = false;
			if(array)
				sb.append(entry.getValue().toJSON());
			else {
				sb.append(Utils.escapeJSON(entry.getKey())).append(":").append(entry.getValue().toJSON());
			}
		}
		
		sb.append(array ? "]" : "}");

		json = sb.toString();
		return json;
	}
	
	public static SerializedObject serialize(String name, Object object, Class klass, boolean forceStaticSerialization) {
		return serialize(name, object, klass, MAX_DEPTH, forceStaticSerialization);
	}
	
	public static SerializedObject serialize(String name, Object object, Class klass, int maxDepth, boolean forceStaticSerialization) {
		return serialize(name, object, klass, 0, maxDepth, forceStaticSerialization);
	}
	
	private static SerializedObject serialize(String name, Object object, Class klass, int depth, int maxDepth, boolean forceStaticSerialization) {
		SerializedObject result = new SerializedObject();
		result.name = name;
		result.object = object;
		result.klass = klass;
		result.serializedType = depth < maxDepth ? SerializedType.from(object, klass) : SerializedType.SIMPLE;
				
		if(result.serializedType.equals(SerializedType.NULL) && !forceStaticSerialization)
			return result;
		
		if(result.serializedType.equals(SerializedType.SIMPLE)) {
			try {
				result.value = result.object == null ? null : result.object.toString();
			} catch (Throwable t) {
				result.value = "error while getting value [" + t + "]";
			}
			return result;
		}
		
		Field[] fields = klass.getFields();
		
		if(result.serializedType.equals(SerializedType.OBJECT) || result.serializedType.equals(SerializedType.NULL) && forceStaticSerialization) {
			for(Field field: fields) {
				field.setAccessible(true);
				String fieldName = field.getName();
				try {
					if(Modifier.isStatic(field.getModifiers())) {
						if(!fieldName.startsWith("$")) { // prevent inspecting static fields due to play enhancement TODO: better check
							result.staticFields.put(fieldName, serialize(fieldName, field.get(object), field.getType(), depth + 1, maxDepth, forceStaticSerialization));
						}
					} else if(object != null) {
						result.fields.put(fieldName, serialize(fieldName, field.get(object), field.getType(), depth + 1, maxDepth, forceStaticSerialization));
					}
				} catch (IllegalAccessException e) {
					throw new RuntimeException(e);
				}
			}
		}
		
		if(result.serializedType.equals(SerializedType.MAP)) {
			Map<Object, Object> map = (Map)object;
			for(Entry<Object, Object> entry : map.entrySet()) {
				String fieldName = entry.getKey() != null ? entry.getKey().toString() : "null";
				result.fields.put(fieldName, serialize(fieldName, entry.getValue(), entry.getValue() != null ? entry.getValue().getClass() : Object.class, depth + 1, maxDepth, forceStaticSerialization));
			}
		}
		
		if(result.serializedType.equals(SerializedType.COLLECTION)) {
			Iterable<Object> collection = (Iterable)object;
			int i = 0;
			for(Object o : collection) {
				String is = Integer.toString(i);
				result.fields.put(is, serialize(is, o, o != null ? o.getClass() : Object.class, depth + 1, maxDepth, forceStaticSerialization));
				i++;
			}
		}
		
		if(result.serializedType.equals(SerializedType.ARRAY)) {
			int length = Array.getLength(object);
			Class componentType = klass.getComponentType(); 
			for(int i = 0; i < length; i++) {
				String is = Integer.toString(i);
				result.fields.put(is, serialize(is, Array.get(object, i), componentType, depth + 1, maxDepth, forceStaticSerialization));
			}
		}
		
		return result;
	}
	
	public static enum SerializedType {
		OBJECT,
		MAP,
		COLLECTION,
		ARRAY,
		SIMPLE,
		NULL;
		
		public static SerializedType from(Object object, Class klass) {
			if(object == null)
				return NULL;
			if(Map.class.isAssignableFrom(klass))
				return MAP;
			if(Collection.class.isAssignableFrom(klass))
				return COLLECTION;
			if(klass.isArray())
				return ARRAY;
			if(CharSequence.class.isAssignableFrom(klass) || klass.equals(Boolean.class) || klass.equals(Character.class) ||
					Number.class.isAssignableFrom(klass) || InputStream.class.isAssignableFrom(klass) ||
					OutputStream.class.isAssignableFrom(klass))
				return SIMPLE;
			return OBJECT;
		}
	}
}