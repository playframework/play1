package play.data.binding;

import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import play.data.validation.Validation;

/**
 * The binder try to convert String values to Java objects.
 */
public class Binder {

    static Map<Class, SupportedType> supportedTypes = new HashMap<Class, SupportedType>();
    
    static {
        supportedTypes.put(Date.class, new DateBinder());
        supportedTypes.put(File.class, new FileBinder());
        supportedTypes.put(Calendar.class, new CalendarBinder());
        supportedTypes.put(Locale.class, new LocaleBinder());

    }

    static Map<Class, BeanWrapper> beanwrappers = new HashMap<Class, BeanWrapper>();

    static BeanWrapper getBeanWrapper(Class clazz) {
        if (!beanwrappers.containsKey(clazz)) {
            BeanWrapper beanwrapper = new BeanWrapper(clazz);
            beanwrappers.put(clazz, beanwrapper);
        }
        return beanwrappers.get(clazz);
    }

    static Object bindInternal(String name, Class clazz, Type type, Map<String, String[]> params, String prefix) {
        try {
            if (isComposite(name + prefix, params.keySet())) {
                BeanWrapper beanWrapper = getBeanWrapper(clazz);
                return beanWrapper.bind(name, type, params, prefix);
            }
            String[] value = params.get(name + prefix);
            // Arrays types 
            if (clazz.isArray()) {
                if (value == null) {
                    return null;
                }
                Object r = Array.newInstance(clazz.getComponentType(), value.length);
                for (int i = 0; i < value.length; i++) {
                    Array.set(r, i, directBind(value[i], clazz.getComponentType()));
                }
                return r;
            }
            // Enums
            if(Enum.class.isAssignableFrom(clazz)) {
            	if(value == null || value.length == 0)
            		return null;
            	return Enum.valueOf(clazz, value[0]);
            }
            // Map 
            if(Map.class.isAssignableFrom(clazz)) {
                Class keyClass = String.class;
                Class valueClass = String.class;
                if (type instanceof ParameterizedType) {
                    keyClass = (Class) ((ParameterizedType) type).getActualTypeArguments()[0];
                    valueClass = (Class) ((ParameterizedType) type).getActualTypeArguments()[1];
                }
                // Search for all params
                Map r = new HashMap();
                for(String param : params.keySet()) {
                    Pattern p = Pattern.compile("^"+name + prefix+"\\[([^\\]]+)\\](.*)$");
                    Matcher m = p.matcher(param);
                    if(m.matches()) {
                        String key = m.group(1);
                        Map<String,String[]> tP = new HashMap();
                        tP.put("key", new String[] {key});
                        Object oKey = bindInternal("key", keyClass, keyClass, tP, "");
                        if (isComposite(name + prefix  +"[" + key + "]", params.keySet())) {
                            BeanWrapper beanWrapper = getBeanWrapper(valueClass);
                            Object oValue = beanWrapper.bind("", type, params, name + prefix  +"[" + key + "]");
                            r.put(oKey, oValue);
                        } else {
                            tP = new HashMap();
                            tP.put("value", params.get(name + prefix  +"[" + key + "]"));
                            Object oValue = bindInternal("value", valueClass, valueClass, tP, "");
                            r.put(oKey, oValue);
                        }
                    }
                }
                return r;
            }
            // Collections types
            if (Collection.class.isAssignableFrom(clazz)) {
                if (value == null) {
                    return null;
                }
                if (clazz.isInterface()) {
                    if (clazz.equals(List.class)) {
                        clazz = ArrayList.class;
                    }
                    if (clazz.equals(Set.class)) {
                        clazz = HashSet.class;
                    }
                    if (clazz.equals(SortedSet.class)) {
                        clazz = TreeSet.class;
                    }
                }
                Collection r = (Collection) clazz.newInstance();
                Class componentClass = String.class;
                if (type instanceof ParameterizedType) {
                    componentClass = (Class) ((ParameterizedType) type).getActualTypeArguments()[0];
                }
                for (String v : value) {
                    r.add(directBind(v, componentClass));
                }
                return r;
            }
            if (value == null) {
                value = new String[0];
            }
            // Simple types
            if (value.length > 0) {
                return directBind(value[0], clazz);
            } else {
                if (clazz.equals(boolean.class)) {
                    return false;
                }
                if (clazz.equals(int.class)) {
                    return 0;
                }
                if (clazz.equals(long.class)) {
                    return 0;
                }
                if (clazz.equals(double.class)) {
                    return 0;
                }
                if (clazz.equals(short.class)) {
                    return 0;
                }
                if (clazz.equals(byte.class)) {
                    return 0;
                }
                if (clazz.equals(char.class)) {
                    return ' ';
                }
            }
            return null;
        } catch (Exception e) {
            Validation.addError(name+prefix, "validation.invalid");
                if (clazz.equals(boolean.class)) {
                    return false;
                }
                if (clazz.equals(int.class)) {
                    return 0;
                }
                if (clazz.equals(long.class)) {
                    return 0;
                }
                if (clazz.equals(double.class)) {
                    return 0;
                }
                if (clazz.equals(short.class)) {
                    return 0;
                }
                if (clazz.equals(byte.class)) {
                    return 0;
                }
                if (clazz.equals(char.class)) {
                    return ' ';
                }
            return null;
        }
    }

    public static Object bind(String name, Class clazz, Type type, Map<String, String[]> params) {
        return bindInternal(name, clazz, type, params, "");
    }

    public static boolean isComposite(String name, Set<String> pNames) {
        for (String pName : pNames) {
            if (pName.startsWith(name + ".")) {
                return true;
            }
        }
        return false;
    }

    public static Object directBind(String value, Class clazz) throws Exception {
        if (clazz.equals(String.class)) {
            return value;
        }
        if (supportedTypes.containsKey(clazz)) {
            if (value == null || value.trim().length() == 0) {
                return null;
            }
            return supportedTypes.get(clazz).bind(value);
        }
        if (clazz.getName().equals("int") || clazz.equals(Integer.class)) {
            if (value == null || value.trim().length() == 0) {
                return null;
            }
            if(value.contains(".")) {
                value = value.substring(0, value.indexOf("."));
            }
            return Integer.parseInt(value);
        }
        if (clazz.getName().equals("double") || clazz.equals(Double.class)) {
            if (value == null || value.trim().length() == 0) {
                return null;
            }
            return Double.parseDouble(value);
        }
        if (clazz.getName().equals("short") || clazz.equals(Short.class)) {
            if (value == null || value.trim().length() == 0) {
                return null;
            }
            if(value.contains(".")) {
                value = value.substring(0, value.indexOf("."));
            }
            return Short.parseShort(value);
        }
        if (clazz.getName().equals("long") || clazz.equals(Long.class)) {
            if (value == null || value.trim().length() == 0) {
                return null;
            }
            if(value.contains(".")) {
                value = value.substring(0, value.indexOf("."));
            }
            return Long.parseLong(value);
        }
        if (clazz.getName().equals("float") || clazz.equals(Float.class)) {
            if (value == null || value.trim().length() == 0) {
                return null;
            }
            return Float.parseFloat(value);
        }
        if ( clazz.equals(BigDecimal.class)) {
            if (value == null || value.trim().length() == 0) {
                return null;
            }
            return new BigDecimal(value);
        }
        if (clazz.getName().equals("boolean") || clazz.equals(Boolean.class)) {
            if (value == null || value.trim().length() == 0) {
                return null;
            }
            return Boolean.parseBoolean(value);
        }
        return null;
    }
}
