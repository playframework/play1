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
import play.data.Upload;
import play.data.validation.Validation;

/**
 * The binder try to convert String values to Java objects.
 */
public class Binder {

    static Map<Class, SupportedType> supportedTypes = new HashMap<Class, SupportedType>();    

    static {
        supportedTypes.put(Date.class, new DateBinder());
        supportedTypes.put(File.class, new FileBinder());
        supportedTypes.put(Upload.class, new UploadBinder());
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
    public static Object MISSING = new Object();

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
                    value = params.get(name + prefix + "[]");
                }
                if (value == null) {
                    return MISSING;
                }
                Object r = Array.newInstance(clazz.getComponentType(), value.length);
                for (int i = 0; i < value.length; i++) {
                    try {
                        Array.set(r, i, directBind(value[i], clazz.getComponentType()));
                    } catch (Exception e) {
                        // ?? One item was bad
                    }
                }
                return r;
            }
            // Enums
            if (Enum.class.isAssignableFrom(clazz)) {
                if (value == null || value.length == 0) {
                    return MISSING;
                }
                return Enum.valueOf(clazz, value[0]);
            }
            // Map 
            if (Map.class.isAssignableFrom(clazz)) {
                Class keyClass = String.class;
                Class valueClass = String.class;
                if (type instanceof ParameterizedType) {
                    keyClass = (Class) ((ParameterizedType) type).getActualTypeArguments()[0];
                    valueClass = (Class) ((ParameterizedType) type).getActualTypeArguments()[1];
                }
                // Search for all params
                Map r = new HashMap();
                for (String param : params.keySet()) {
                    Pattern p = Pattern.compile("^" + name + prefix + "\\[([^\\]]+)\\](.*)$");
                    Matcher m = p.matcher(param);
                    if (m.matches()) {
                        String key = m.group(1);
                        Map<String, String[]> tP = new HashMap();
                        tP.put("key", new String[]{key});
                        Object oKey = bindInternal("key", keyClass, keyClass, tP, "");
                        if (oKey != MISSING) {
                            if (isComposite(name + prefix + "[" + key + "]", params.keySet())) {
                                BeanWrapper beanWrapper = getBeanWrapper(valueClass);
                                Object oValue = beanWrapper.bind("", type, params, name + prefix + "[" + key + "]");
                                r.put(oKey, oValue);
                            } else {
                                tP = new HashMap();
                                tP.put("value", params.get(name + prefix + "[" + key + "]"));
                                Object oValue = bindInternal("value", valueClass, valueClass, tP, "");
                                if (oValue != MISSING) {
                                    r.put(oKey, oValue);
                                } else {
                                    r.put(oKey, null);
                                }
                            }
                        }
                    }
                }
                return r;
            }
            // Collections types
            if (Collection.class.isAssignableFrom(clazz)) {
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
                if (value == null) {
                    value = params.get(name + prefix + "[]");
                    if (value == null && r instanceof List) {
                        for (String param : params.keySet()) {
                            Pattern p = Pattern.compile("^" + name + prefix + "\\[([0-9]+)\\](.*)$");
                            Matcher m = p.matcher(param);
                            if (m.matches()) {
                                int key = Integer.parseInt(m.group(1));
                                while (((List) r).size() <= key) {
                                    ((List) r).add(null);
                                }
                                if (isComposite(name + prefix + "[" + key + "]", params.keySet())) {
                                    BeanWrapper beanWrapper = getBeanWrapper(componentClass);
                                    Object oValue = beanWrapper.bind("", type, params, name + prefix + "[" + key + "]");
                                    ((List) r).set(key, oValue);
                                } else {
                                    Map tP = new HashMap();
                                    tP.put("value", params.get(name + prefix + "[" + key + "]"));
                                    Object oValue = bindInternal("value", componentClass, componentClass, tP, "");
                                    if (oValue != MISSING) {
                                        ((List) r).set(key, oValue);
                                    }
                                }
                            }
                        }
                        return r.size() == 0 ? MISSING : r;
                    }
                }
                if (value == null) {
                    return MISSING;
                }
                for (String v : value) {
                    try {
                        r.add(directBind(v, componentClass));
                    } catch (Exception e) {
                        // ?? One item was bad
                    }
                }
                return r;
            }
            // Simple types
            if (value == null || value.length == 0) {
                return MISSING;
            }
            return directBind(value[0], clazz);
        } catch (Exception e) {
            Validation.addError(name + prefix, "validation.invalid");
            return MISSING;
        }
    }

    public static Object bind(String name, Class clazz, Type type, Map<String, String[]> params) {
        Object result = bindInternal(name, clazz, type, params, "");
        if (result == MISSING) {
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
        return result;
    }

    static boolean isComposite(String name, Set<String> pNames) {
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
        if (clazz.getName().equals("int")) {
            if (value == null || value.trim().length() == 0) {
                return 0;
            }
            if (value.contains(".")) {
                value = value.substring(0, value.indexOf("."));
            }
            return Integer.parseInt(value);
        }
        if (clazz.equals(Integer.class)) {
            if (value == null || value.trim().length() == 0) {
                return null;
            }
            if (value.contains(".")) {
                value = value.substring(0, value.indexOf("."));
            }
            return Integer.parseInt(value);
        }
        if (clazz.getName().equals("double")) {
            if (value == null || value.trim().length() == 0) {
                return 0D;
            }
            return Double.parseDouble(value);
        }
        if (clazz.equals(Double.class)) {
            if (value == null || value.trim().length() == 0) {
                return null;
            }
            return Double.parseDouble(value);
        }
        if (clazz.getName().equals("short")) {
            if (value == null || value.trim().length() == 0) {
                return 0;
            }
            if (value.contains(".")) {
                value = value.substring(0, value.indexOf("."));
            }
            return Short.parseShort(value);
        }
        if (clazz.equals(Short.class)) {
            if (value == null || value.trim().length() == 0) {
                return null;
            }
            if (value.contains(".")) {
                value = value.substring(0, value.indexOf("."));
            }
            return Short.parseShort(value);
        }
        if (clazz.getName().equals("long")) {
            if (value == null || value.trim().length() == 0) {
                return 0L;
            }
            if (value.contains(".")) {
                value = value.substring(0, value.indexOf("."));
            }
            return Long.parseLong(value);
        }
        if (clazz.equals(Long.class)) {
            if (value == null || value.trim().length() == 0) {
                return null;
            }
            if (value.contains(".")) {
                value = value.substring(0, value.indexOf("."));
            }
            return Long.parseLong(value);
        }
        if (clazz.getName().equals("float")) {
            if (value == null || value.trim().length() == 0) {
                return 0;
            }
            return Float.parseFloat(value);
        }
        if (clazz.equals(Float.class)) {
            if (value == null || value.trim().length() == 0) {
                return null;
            }
            return Float.parseFloat(value);
        }
        if (clazz.equals(BigDecimal.class)) {
            if (value == null || value.trim().length() == 0) {
                return null;
            }
            return new BigDecimal(value);
        }
        if (clazz.getName().equals("boolean")) {
            if (value == null || value.trim().length() == 0) {
                return false;
            }
            return Boolean.parseBoolean(value);
        }
        if (clazz.equals(Boolean.class)) {
            if (value == null || value.trim().length() == 0) {
                return null;
            }
            return Boolean.parseBoolean(value);
        }
        return null;
    }
}
