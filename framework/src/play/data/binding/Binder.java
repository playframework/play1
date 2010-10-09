package play.data.binding;

import play.data.binding.types.*;
import play.Logger;
import play.Play;
import play.PlayPlugin;
import play.data.Upload;
import play.data.validation.Validation;
import play.exceptions.UnexpectedException;
import play.utils.Utils;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang.StringUtils;

import play.db.Model;

/**
 * The binder try to convert String values to Java objects.
 */
public class Binder {

    static final Map<Class<?>, TypeBinder<?>> supportedTypes = new HashMap<Class<?>, TypeBinder<?>>();

    // TODO: something a bit more dynamic? The As annotation allows you to inject your own binder
    static {
        supportedTypes.put(Date.class, new DateBinder());
        supportedTypes.put(File.class, new FileBinder());
        supportedTypes.put(File[].class, new FileArrayBinder());
        supportedTypes.put(Model.BinaryField.class, new BinaryBinder());
        supportedTypes.put(Upload.class, new UploadBinder());
        supportedTypes.put(Upload[].class, new UploadArrayBinder());
        supportedTypes.put(Calendar.class, new CalendarBinder());
        supportedTypes.put(Locale.class, new LocaleBinder());
        supportedTypes.put(byte[].class, new ByteArrayBinder());
        supportedTypes.put(byte[][].class, new ByteArrayArrayBinder());
    }
    static Map<Class<?>, BeanWrapper> beanwrappers = new HashMap<Class<?>, BeanWrapper>();

    static BeanWrapper getBeanWrapper(Class<?> clazz) {
        if (!beanwrappers.containsKey(clazz)) {
            BeanWrapper beanwrapper = new BeanWrapper(clazz);
            beanwrappers.put(clazz, beanwrapper);
        }
        return beanwrappers.get(clazz);
    }
    public final static Object MISSING = new Object();
    public final static Object NO_BINDING = new Object();

    @SuppressWarnings("unchecked")
    static Object bindInternal(String name, Class clazz, Type type, Annotation[] annotations, Map<String, String[]> params, String prefix, String[] profiles) {
        try {
            Logger.trace("bindInternal: class [" + clazz + "] name [" + name + "] annotation [" + Utils.join(annotations, " ") + "] isComposite [" + isComposite(name + prefix, params) + "]");

            if (isComposite(name + prefix, params)) {
                BeanWrapper beanWrapper = getBeanWrapper(clazz);
                return beanWrapper.bind(name, type, params, prefix, annotations);
            }
            Logger.trace("bindInternal: name [" + name + "] prefix [" + prefix + "]");

            String[] value = params.get(name + prefix);
            Logger.trace("bindInternal: value [" + value + "]");
            Logger.trace("bindInternal: profile [" + Utils.join(profiles, ",") + "]");
            // Let see if we have a BindAs annotation and a separator. If so, we need to split the values
            // Look up for the BindAs annotation. Extract the profile if there is any.
            // TODO: Move me somewhere else?
            if (annotations != null) {
                for (Annotation annotation : annotations) {
                    if ((clazz.isArray() || Collection.class.isAssignableFrom(clazz)) && value != null && value.length > 0 && annotation.annotationType().equals(As.class)) {
                        As as = ((As) annotation);
                        final String separator = as.value()[0];
                        value = value[0].split(separator);
                    }
                    if (annotation.annotationType().equals(NoBinding.class)) {
                        NoBinding bind = ((NoBinding) annotation);
                        String[] localUnbindProfiles = bind.value();
                        Logger.trace("bindInternal: localUnbindProfiles [" + Utils.join(localUnbindProfiles, ",") + "]");

                        if (localUnbindProfiles != null && contains(profiles, localUnbindProfiles)) {
                            return NO_BINDING;
                        }
                    }
                }
            }

            // Arrays types
            // The array condition is not so nice... We should find another way of doing this....
            if (clazz.isArray() && (clazz != byte[].class && clazz != byte[][].class && clazz != File[].class && clazz != Upload[].class)) {
                if (value == null) {
                    value = params.get(name + prefix + "[]");
                }
                if (value == null) {
                    return MISSING;
                }
                Object r = Array.newInstance(clazz.getComponentType(), value.length);
                for (int i = 0; i <= value.length; i++) {
                    try {
                        Array.set(r, i, directBind(name, annotations, value[i], clazz.getComponentType()));
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
                } else if (StringUtils.isEmpty(value[0])) {
                    return null;
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
                        Object oKey = bindInternal("key", keyClass, keyClass, annotations, tP, "", value);
                        if (oKey != MISSING) {
                            if (isComposite(name + prefix + "[" + key + "]", params)) {
                                BeanWrapper beanWrapper = getBeanWrapper(valueClass);
                                Object oValue = beanWrapper.bind("", type, params, name + prefix + "[" + key + "]", annotations);
                                r.put(oKey, oValue);
                            } else {
                                tP = new HashMap<String, String[]>();
                                tP.put("value", params.get(name + prefix + "[" + key + "]"));
                                Object oValue = bindInternal("value", valueClass, valueClass, annotations, tP, "", value);
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
                            Pattern p = Pattern.compile("^" + escape(name + prefix) + "\\[([0-9]+)\\](.*)$");
                            Matcher m = p.matcher(param);
                            if (m.matches()) {
                                int key = Integer.parseInt(m.group(1));
                                while (((List<?>) r).size() <= key) {
                                    ((List<?>) r).add(null);
                                }
                                if (isComposite(name + prefix + "[" + key + "]", params)) {
                                    BeanWrapper beanWrapper = getBeanWrapper(componentClass);
                                    Object oValue = beanWrapper.bind("", type, params, name + prefix + "[" + key + "]", annotations);
                                    ((List) r).set(key, oValue);
                                } else {
                                    Map<String, String[]> tP = new HashMap<String, String[]>();
                                    tP.put("value", params.get(name + prefix + "[" + key + "]"));
                                    Object oValue = bindInternal("value", componentClass, componentClass, annotations, tP, "", value);
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
                        r.add(directBind(name, annotations, v, componentClass));
                    } catch (Exception e) {
                        // ?? One item was bad
                        Logger.debug(e, "error:");
                    }
                }
                return r;
            }
            // Simple types
            if (value == null || value.length == 0) {
                return MISSING;
            }
            return directBind(name, annotations, value[0], clazz);
        } catch (Exception e) {
            Validation.addError(name + prefix, "validation.invalid");
            return MISSING;
        }
    }

    private static String escape(String s) {
        s = s.replace(".", "\\.");
        s = s.replace("[", "\\[");
        s = s.replace("]", "\\]");
        return s;
    }

    public static boolean contains(String[] profiles, String[] localProfiles) {
        if (localProfiles != null) {
            for (String l : localProfiles) {
                if ("*".equals(l)) {
                    return true;
                }
                if (profiles != null) {
                    for (String p : profiles) {
                        if (l.equals(p) || "*".equals(p)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public static Object bind(Object o, String name, Map<String, String[]> params) {
        for (PlayPlugin plugin : Play.plugins) {
            Object result = plugin.bind(name, o, params);
            if (result != null) {
                return result;
            }
        }
        try {
            return new BeanWrapper(o.getClass()).bind(name, null, params, "", o, null);
        } catch (Exception e) {
            Validation.addError(name, "validation.invalid");
            return null;
        }
    }

    public static Object bind(String name, Class<?> clazz, Type type, Annotation[] annotations, Map<String, String[]> params) {
        return bind(name, clazz, type, annotations, params, null, null, 0);
    }

    public static Object bind(String name, Class<?> clazz, Type type, Annotation[] annotations, Map<String, String[]> params, Object o, Method method, int parameterIndex) {
        Logger.trace("bind: name [" + name + "] annotation [" + Utils.join(annotations, " ") + "] ");

        Object result = null;
        // Let a chance to plugins to bind this object
        for (PlayPlugin plugin : Play.plugins) {
            result = plugin.bind(name, clazz, type, annotations, params);
            if (result != null) {
                return result;
            }
        }
        String[] profiles = null;
        if (annotations != null) {
            for (Annotation annotation : annotations) {
                if (annotation.annotationType().equals(As.class)) {
                    As as = ((As) annotation);
                    profiles = as.value();
                }
                if (annotation.annotationType().equals(NoBinding.class)) {
                    NoBinding bind = ((NoBinding) annotation);
                    profiles = bind.value();
                }
            }
        }
        result = bindInternal(name, clazz, type, annotations, params, "", profiles);

        if (result == MISSING) {
            // Try the scala default
            if (o != null && parameterIndex > 0) {
                try {
                    Method defaultMethod = method.getDeclaringClass().getDeclaredMethod(method.getName() + "$default$" + parameterIndex);
                    return defaultMethod.invoke(o);
                } catch (NoSuchMethodException e) {
                    //
                } catch (Exception e) {
                    throw new UnexpectedException(e);
                }
            }
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

    static boolean isComposite(String name, Map<String, String[]> params) {
        for (String pName : params.keySet()) {
            if (pName.startsWith(name + ".") && params.get(pName) != null && params.get(pName).length > 0) {
                return true;
            }
        }
        return false;
    }

    public static Object directBind(String value, Class<?> clazz) throws Exception {
        return directBind(null, null, value, clazz);
    }

    public static Object directBind(String name, Annotation[] annotations, String value, Class<?> clazz) throws Exception {
        Logger.trace("directBind: value [" + value + "] annotation [" + Utils.join(annotations, " ") + "] Class [" + clazz + "]");

        boolean nullOrEmpty = value == null || value.trim().length() == 0;

        if (annotations != null) {
            for (Annotation annotation : annotations) {
                if (annotation.annotationType().equals(As.class)) {
                    Class<? extends TypeBinder<?>> toInstanciate = ((As) annotation).binder();
                    if (!(toInstanciate.equals(As.DEFAULT.class))) {
                        // Instantiate the binder
                        TypeBinder<?> myInstance = toInstanciate.newInstance();
                        return myInstance.bind(name, annotations, value, clazz);
                    }
                }
            }
        }

        // custom types
        for (Class<?> c : supportedTypes.keySet()) {
            Logger.trace("directBind: value [" + value + "] c [" + c + "] Class [" + clazz + "]");
            if (c.isAssignableFrom(clazz)) {
                Logger.trace("directBind: isAssignableFrom is true");
                return nullOrEmpty ? null : supportedTypes.get(c).bind(name, annotations, value, clazz);
            }
        }

        // application custom types
        for (Class<TypeBinder<?>> c : Play.classloader.getAssignableClasses(TypeBinder.class)) {
            if (c.isAnnotationPresent(Global.class)) {
                Class<?> forType = (Class) ((ParameterizedType) c.getGenericInterfaces()[0]).getActualTypeArguments()[0];
                if (forType.isAssignableFrom(clazz)) {
                    return c.newInstance().bind(name, annotations, value, clazz);
                }
            }
        }

        // raw String
        if (clazz.equals(String.class)) {
            return value;
        }

        // Enums
        if (Enum.class.isAssignableFrom(clazz)) {
            if (nullOrEmpty) {
                return null;
            }
            return Enum.valueOf((Class<Enum>)clazz, value);
        }

        // int or Integer binding
        if (clazz.getName().equals("int") || clazz.equals(Integer.class)) {
            if (nullOrEmpty) {
                return clazz.isPrimitive() ? 0 : null;
            }

            return Integer.parseInt(value.contains(".") ? value.substring(0, value.indexOf(".")) : value);
        }

        // long or Long binding
        if (clazz.getName().equals("long") || clazz.equals(Long.class)) {
            if (nullOrEmpty) {
                return clazz.isPrimitive() ? 0l : null;
            }

            return Long.parseLong(value.contains(".") ? value.substring(0, value.indexOf(".")) : value);
        }

        // byte or Byte binding
        if (clazz.getName().equals("byte") || clazz.equals(Byte.class)) {
            if (nullOrEmpty) {
                return clazz.isPrimitive() ? (byte) 0 : null;
            }

            return Byte.parseByte(value.contains(".") ? value.substring(0, value.indexOf(".")) : value);
        }

        // short or Short binding
        if (clazz.getName().equals("short") || clazz.equals(Short.class)) {
            if (nullOrEmpty) {
                return clazz.isPrimitive() ? (short) 0 : null;
            }

            return Short.parseShort(value.contains(".") ? value.substring(0, value.indexOf(".")) : value);
        }

        // float or Float binding
        if (clazz.getName().equals("float") || clazz.equals(Float.class)) {
            if (nullOrEmpty) {
                return clazz.isPrimitive() ? 0f : null;
            }

            return Float.parseFloat(value);
        }

        // double or Double binding
        if (clazz.getName().equals("double") || clazz.equals(Double.class)) {
            if (nullOrEmpty) {
                return clazz.isPrimitive() ? 0d : null;
            }

            return Double.parseDouble(value);
        }

        // BigDecimal binding
        if (clazz.equals(BigDecimal.class)) {
            if (nullOrEmpty) {
                return null;
            }

            return new BigDecimal(value);
        }

        // boolean or Boolean binding
        if (clazz.getName().equals("boolean") || clazz.equals(Boolean.class)) {
            if (nullOrEmpty) {
                return clazz.isPrimitive() ? false : null;
            }

            if (value.equals("1") || value.toLowerCase().equals("on") || value.toLowerCase().equals("yes")) {
                return true;
            }

            return Boolean.parseBoolean(value);
        }

        return null;
    }
}
