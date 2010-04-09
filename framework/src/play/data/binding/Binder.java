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

import org.apache.commons.lang.StringUtils;
import play.Logger;
import play.Play;
import play.PlayPlugin;
import play.data.Upload;
import play.data.validation.Validation;
import play.utils.Utils;

/**
 * The binder try to convert String values to Java objects.
 */
public class Binder {

    static Map<Class<?>, SupportedType<?>> supportedTypes = new HashMap<Class<?>, SupportedType<?>>();

    static {
        supportedTypes.put(Date.class, new DateBinder());
        supportedTypes.put(File.class, new FileBinder());
        supportedTypes.put(Upload.class, new UploadBinder());
        supportedTypes.put(Calendar.class, new CalendarBinder());
        supportedTypes.put(Locale.class, new LocaleBinder());
    }

    static Map<Class<?>, BeanWrapper> beanwrappers = new HashMap<Class<?>, BeanWrapper>();

    static BeanWrapper getBeanWrapper(Class<?> clazz) {
        if (!beanwrappers.containsKey(clazz)) {
            BeanWrapper beanwrapper = new BeanWrapper(clazz);
            beanwrappers.put(clazz, beanwrapper);
        }
        return beanwrappers.get(clazz);
    }

    public static Object MISSING = new Object();

    @SuppressWarnings("unchecked")
    static Object bindInternal(String name, Class clazz, Type type, Map<String, String[]> params, String prefix) {
        Logger.trace("bindInternal: class [" + clazz + "] name [" + name + "] isComposite [" + isComposite(name + prefix, params.keySet()) + "]");

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
                int j = 0;
                for (int i = 0; i < value.length; i++) {
                    try {
                        Object obj = directBind(value[i], clazz.getComponentType());
                        if (obj != null) {
                            Array.set(r, j++, obj);
                        }
                    } catch (Exception e) {
                        // ?? One item was bad
                    }
                }
                return r;
            }
            Logger.trace("bindInternal: value [" + value + "]");


            // Enums
            if (Enum.class.isAssignableFrom(clazz)) {
                if (value == null || value.length == 0 || StringUtils.isEmpty(value[0])) {
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
                Map<Object, Object> r = new HashMap<Object, Object>();
                for (String param : params.keySet()) {
                    Pattern p = Pattern.compile("^" + name + prefix + "\\[([^\\]]+)\\](.*)$");
                    Matcher m = p.matcher(param);
                    if (m.matches()) {
                        String key = m.group(1);
			value = params.get(param);
                        Map<String, String[]> tP = new HashMap<String, String[]>();
                        tP.put("key", new String[]{key});
                        Object oKey = bindInternal("key", keyClass, keyClass, tP, "");

                        if (oKey != MISSING) {
                            if (isComposite(name + prefix + "[" + key + "]", params.keySet())) {
                                BeanWrapper beanWrapper = getBeanWrapper(valueClass);
                                Object oValue = beanWrapper.bind("", type, params, name + prefix + "[" + key + "]");
                                r.put(oKey, oValue);
                            } else {
                                tP = new HashMap<String, String[]>();
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
                Logger.trace("bindInternal: componentClass [" + componentClass + "]");
                if (value == null) {
                    value = params.get(name + prefix + "[]");
                    if (value == null && r instanceof Collection) {
                        for (String param : params.keySet()) {
                            Pattern p = Pattern.compile("^" + name + prefix + "\\[([0-9]+)\\](.*)$");
                            Matcher m = p.matcher(param);
                            if (m.matches()) {
                                int key = Integer.parseInt(m.group(1));
                                while (((Collection<?>) r).size() <= key) {
                                    ((Collection<?>) r).add(null);
                                }
                                if (isComposite(name + prefix + "[" + key + "]", params.keySet())) {
                                    BeanWrapper beanWrapper = getBeanWrapper(componentClass);
                                    Logger.trace("bindInternal: param [" + param + "]");
                                    Object oValue = beanWrapper.bind("", type, params, name + prefix + "[" + key + "]");
                                    Logger.trace("bindInternal: oValue [" + oValue + "]");
                                    if (r instanceof List) {
                                        ((List) r).set(key, oValue);
                                    } else if (r instanceof Set) {
                                        ((Set) r).add(oValue);
                                    }
                                } else {
                                    Map<String, String[]> tP = new HashMap<String, String[]>();
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
                        Object newValue = directBind(v, componentClass);
                        r.add(newValue);
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

    public static Object bind(String name, Class<?> clazz, Type type, Map<String, String[]> params) {
        Object result = null;
        // Let a chance to plugins to bind this object
        for (PlayPlugin plugin : Play.plugins) {
            result = plugin.bind(name, clazz, type, params);
            if (result != null) {
                return result;
            }
        }
        result = bindInternal(name, clazz, type, params, "");
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
        Logger.trace("directBind: value [" + value + "] class [" + clazz + "] ");

        if (clazz.equals(String.class)) {
            return value;
        }
        
        boolean nullOrEmpty = value == null || value.trim().length() == 0;

        if (supportedTypes.containsKey(clazz)) {
            return nullOrEmpty ? null : supportedTypes.get(clazz).bind(value);
        }

        if (Enum.class.isAssignableFrom(clazz)) {
            return nullOrEmpty ? null : Enum.valueOf(clazz, value);
        }

        // int or Integer binding
        if (clazz.getName().equals("int") || clazz.equals(Integer.class)) {
            if (nullOrEmpty)
                return clazz.isPrimitive() ? 0 : null;

            return Integer.parseInt(value.contains(".") ? value.substring(0, value.indexOf(".")) : value);
        }

        // long or Long binding
        if (clazz.getName().equals("long") || clazz.equals(Long.class)) {
            if (nullOrEmpty)
                return clazz.isPrimitive() ? 0l : null;

            return Long.parseLong(value.contains(".") ? value.substring(0, value.indexOf(".")) : value);
        }

        // byte or Byte binding
        if (clazz.getName().equals("byte") || clazz.equals(Byte.class)) {
            if (nullOrEmpty)
                return clazz.isPrimitive() ? (byte) 0 : null;

            return Byte.parseByte(value.contains(".") ? value.substring(0, value.indexOf(".")) : value);
        }

        // short or Short binding
        if (clazz.getName().equals("short") || clazz.equals(Short.class)) {
            if (nullOrEmpty)
                return clazz.isPrimitive() ? (short) 0 : null;

            return Short.parseShort(value.contains(".") ? value.substring(0, value.indexOf(".")) : value);
        }

        // float or Float binding
        if (clazz.getName().equals("float") || clazz.equals(Float.class)) {
            if (nullOrEmpty)
                return clazz.isPrimitive() ? 0f : null;

            return Float.parseFloat(value);
        }

        // double or Double binding
        if (clazz.getName().equals("double") || clazz.equals(Double.class)) {
            if (nullOrEmpty)
                return clazz.isPrimitive() ? 0d : null;

            return Double.parseDouble(value);
        }

        // BigDecimal binding
        if (clazz.equals(BigDecimal.class)) {
            if (nullOrEmpty)
                return null;

            return new BigDecimal(value);
        }

        // boolean or Boolean binding
        if (clazz.getName().equals("boolean") || clazz.equals(Boolean.class)) {
            if (nullOrEmpty)
                return clazz.isPrimitive() ? false : null;

            if (value.equals("1") || value.toLowerCase().equals("on") || value.toLowerCase().equals("yes"))
                return true;

            return Boolean.parseBoolean(value);
        }

        return null;
    }
}
