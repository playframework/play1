package play.data.binding;

import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import play.Logger;
import play.Play;
import play.data.Upload;
import play.data.binding.types.*;
import play.data.validation.Validation;
import play.db.Model;
import play.exceptions.UnexpectedException;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.*;


/**
 * The binder try to convert String values to Java objects.
 */
public abstract class Binder {
    public final static Object MISSING = new Object();
    private final static Object DIRECTBINDING_NO_RESULT = new Object();
    public final static Object NO_BINDING = new Object();

    static final Map<Class<?>, TypeBinder<?>> supportedTypes = new HashMap<Class<?>, TypeBinder<?>>();

    // TODO: something a bit more dynamic? The As annotation allows you to inject your own binder
    static {
        supportedTypes.put(Date.class, new DateBinder());
        supportedTypes.put(DateTime.class, new DateTimeBinder());
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


    public static <T> void register(Class<T> clazz, TypeBinder<T> typeBinder) {
        supportedTypes.put(clazz, typeBinder);
    }

    static Map<Class<?>, BeanWrapper> beanwrappers = new HashMap<Class<?>, BeanWrapper>();

    static BeanWrapper getBeanWrapper(Class<?> clazz) {
        if (!beanwrappers.containsKey(clazz)) {
            BeanWrapper beanwrapper = new BeanWrapper(clazz);
            beanwrappers.put(clazz, beanwrapper);
        }
        return beanwrappers.get(clazz);
    }

    public static class MethodAndParamInfo {
        public final Object objectInstance;
        public final Method method;
        public int parameterIndex;

        public MethodAndParamInfo(Object objectInstance, Method method, int parameterIndex) {
            this.objectInstance = objectInstance;
            this.method = method;
            this.parameterIndex = parameterIndex;
        }
    }

    /**
     * Deprecated. Use bindBean() instead.
     */
    @Deprecated
    public static Object bind(Object o, String name, Map<String, String[]> params) {
        RootParamNode parentParamNode = RootParamNode.convert(params);
        Binder.bindBean(parentParamNode, name, o);
        return o;
    }

    @Deprecated
    public static Object bind(String name, Class<?> clazz, Type type, Annotation[] annotations, Map<String, String[]> params) {
        RootParamNode parentParamNode = RootParamNode.convert(params);
        return bind(parentParamNode, name, clazz, type, annotations);
    }

    public static Object bind(RootParamNode parentParamNode, String name, Class<?> clazz, Type type, Annotation[] annotations) {
        return bind(parentParamNode, name, clazz, type, annotations, null);
    }

    public static Object bind(RootParamNode parentParamNode, String name, Class<?> clazz, Type type, Annotation[] annotations, MethodAndParamInfo methodAndParamInfo) {
        final ParamNode paramNode = parentParamNode.getChild(name, true);

        Object result = null;
        if (paramNode == null) {
            result = MISSING;
        }

        final BindingAnnotations bindingAnnotations = new BindingAnnotations(annotations);

        if (bindingAnnotations.checkNoBinding()) {
            return NO_BINDING;
        }

        if (paramNode != null) {

            // Let a chance to plugins to bind this object
            result = Play.pluginCollection.bind(parentParamNode, name, clazz, type, annotations);
            if (result != null) {
                return result;
            }

            result = internalBind(paramNode, clazz, type, bindingAnnotations);
        }

        if (result == MISSING) {
            // Try the scala default
            if (methodAndParamInfo != null) {
                try {
                    Method method = methodAndParamInfo.method;
                    Method defaultMethod = method.getDeclaringClass().getDeclaredMethod(method.getName() + "$default$" + methodAndParamInfo.parameterIndex);
                    return defaultMethod.invoke(methodAndParamInfo.objectInstance);
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


    protected static Object internalBind(ParamNode paramNode, Class<?> clazz, Type type, BindingAnnotations bindingAnnotations) {

        if (paramNode == null) {
            return MISSING;
        }

        if (paramNode.getValues() == null && paramNode.getAllChildren().size() == 0) {
            return MISSING;
        }

        if (bindingAnnotations.checkNoBinding()) {
            return NO_BINDING;
        }

        try {

            if (Enum.class.isAssignableFrom(clazz)) {
                return bindEnum(clazz, paramNode);
            }

            if (Map.class.isAssignableFrom(clazz)) {
                return bindMap(clazz, type, paramNode, bindingAnnotations);
            }

            if (Collection.class.isAssignableFrom(clazz)) {
                return bindCollection(clazz, type, paramNode, bindingAnnotations);
            }

            Object directBindResult = internalDirectBind(paramNode.getOriginalKey(), bindingAnnotations.annotations, paramNode.getFirstValue(clazz), clazz, type);
            
            if (directBindResult != DIRECTBINDING_NO_RESULT) {
                // we found a value/result when direct binding
                return directBindResult;
            }

            // Must do the default array-check after direct binding, since some custom-binders checks for specific arrays
            if (clazz.isArray()) {
                return bindArray(clazz, paramNode, bindingAnnotations);
            }
			
			if (!paramNode.getAllChildren().isEmpty()) {
	        	return internalBindBean(clazz, paramNode, bindingAnnotations);
	        }

            return null; // give up
        } catch (Exception e) {
            Validation.addError(paramNode.getOriginalKey(), "validation.invalid");
        }
        return MISSING;
    }

    private static Object bindArray(Class<?> clazz, ParamNode paramNode, BindingAnnotations bindingAnnotations) {

        Class<?> componentType = clazz.getComponentType();

        int invalidItemsCount = 0;
        int size;
        Object array;
        String[] values = paramNode.getValues();
        if (values != null) {

            if (bindingAnnotations.annotations != null) {
                for (Annotation annotation : bindingAnnotations.annotations) {
                    if (annotation.annotationType().equals(As.class)) {
                        As as = ((As) annotation);
                        final String separator = as.value()[0];
                        values = values[0].split(separator);
                    }
                }
            }

            size = values.length;
            array = Array.newInstance(componentType, size);
            for (int i = 0; i < size; i++) {
                String thisValue = values[i];
                try {
                    Array.set(array, i - invalidItemsCount, directBind(paramNode.getOriginalKey(), bindingAnnotations.annotations, thisValue, componentType, componentType));
                } catch (Exception e) {
                    // bad item..
                    invalidItemsCount++;
                }
            }
        } else {
            size = paramNode.getAllChildren().size();
            array = Array.newInstance(componentType, size);
            int i = 0;
            for (ParamNode child : paramNode.getAllChildren()) {
                Object childValue = internalBind(child, componentType, componentType, bindingAnnotations);
                if (childValue != NO_BINDING && childValue != MISSING) {
                    try {
                        Array.set(array, i - invalidItemsCount, childValue);
                    } catch (Exception e) {
                        // bad item..
                        invalidItemsCount++;
                    }
                }
                i++;
            }
        }

        if (invalidItemsCount > 0) {
            // must remove some elements from the end..
            int newSize = size - invalidItemsCount;
            Object newArray = Array.newInstance(componentType, newSize);
            for (int i = 0; i < newSize; i++) {
                Array.set(newArray, i, Array.get(array, i));
            }
            array = newArray;
        }

        return array;
    }

    private static Object internalBindBean(Class<?> clazz, ParamNode paramNode, BindingAnnotations bindingAnnotations) throws Exception {
        Object bean = clazz.newInstance();
        internalBindBean(paramNode, bean, bindingAnnotations);
        return bean;
    }

    /**
     * Invokes the plugins before using the internal bindBean.
     */
    public static void bindBean(RootParamNode rootParamNode, String name, Object bean) {

        // Let a chance to plugins to bind this object
        Object result = Play.pluginCollection.bindBean(rootParamNode, name, bean);
        if (result != null) {
            return;
        }

        ParamNode paramNode = StringUtils.isEmpty(name) ? rootParamNode : rootParamNode.getChild(name);

        try {
            internalBindBean(paramNode, bean, new BindingAnnotations());
        } catch (Exception e) {
            Validation.addError(paramNode.getOriginalKey(), "validation.invalid");
        }

    }

    /**
     * Does NOT invoke plugins
     */
    public static void bindBean(ParamNode paramNode, Object bean, Annotation[] annotations) throws Exception {
        internalBindBean(paramNode, bean, new BindingAnnotations(annotations));
    }

    private static void internalBindBean(ParamNode paramNode, Object bean, BindingAnnotations bindingAnnotations) throws Exception {

        BeanWrapper bw = getBeanWrapper(bean.getClass());
        for (BeanWrapper.Property prop : bw.getWrappers()) {
            ParamNode propParamNode = paramNode.getChild(prop.getName());
            if (propParamNode != null) {
                // Create new ParamsContext for this property
                Annotation[] annotations = null;
                // first we try with annotations resolved from property
                annotations = prop.getAnnotations();
                BindingAnnotations propBindingAnnotations = new BindingAnnotations(annotations, bindingAnnotations.getProfiles());
                Object value = internalBind(propParamNode, prop.getType(), prop.getGenericType(), propBindingAnnotations);
                if (value != MISSING) {
                    if (value != NO_BINDING) {
                        prop.setValue(bean, value);
                    }
                } else {
                    // retry without annotations resolved from property, but use input-annotations instead..
                    // This is actually necessary to parse Fixture (iso) dates
                    value = internalBind(propParamNode, prop.getType(), prop.getGenericType(), bindingAnnotations);
                    if (value != NO_BINDING && value != MISSING) {
                        prop.setValue(bean, value);
                    }
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static Object bindEnum(Class<?> clazz, ParamNode paramNode) throws Exception {
        if (paramNode.getValues() == null) {
            return MISSING;
        }

        String value = paramNode.getFirstValue(null);

        if (StringUtils.isEmpty(value)) {
            return null;
        }
        return Enum.valueOf((Class<? extends Enum>) clazz, value);
    }

    private static Object bindMap(Class<?> clazz, Type type, ParamNode paramNode, BindingAnnotations bindingAnnotations) throws Exception {
        Class keyClass = String.class;
        Class valueClass = String.class;
        if (type instanceof ParameterizedType) {
            keyClass = (Class) ((ParameterizedType) type).getActualTypeArguments()[0];
            valueClass = (Class) ((ParameterizedType) type).getActualTypeArguments()[1];
        }

        Map<Object, Object> r = new HashMap<Object, Object>();

        for (ParamNode child : paramNode.getAllChildren()) {
            try {
                Object keyObject = directBind(paramNode.getOriginalKey(), bindingAnnotations.annotations, child.getName(), keyClass, keyClass);
                Object valueObject = internalBind(child, valueClass, valueClass, bindingAnnotations);
                if (valueObject == NO_BINDING || valueObject == MISSING) {
                    valueObject = null;
                }
                r.put(keyObject, valueObject);
            } catch (Exception e) {
                // Just ignore the exception and continue on the next item
            }
        }

        return r;
    }

    @SuppressWarnings("unchecked")
    private static Object bindCollection(Class<?> clazz, Type type, ParamNode paramNode, BindingAnnotations bindingAnnotations) throws Exception {
        if (clazz.isInterface()) {
            if (clazz.equals(List.class)) {
                clazz = ArrayList.class;
            } else if (clazz.equals(Set.class)) {
                clazz = HashSet.class;
            } else if (clazz.equals(SortedSet.class)) {
                clazz = TreeSet.class;
            } else {
                clazz = ArrayList.class;
            }
        }

        Class componentClass = String.class;
        if (type instanceof ParameterizedType) {
            componentClass = (Class) ((ParameterizedType) type).getActualTypeArguments()[0];
        }

        if (paramNode.getAllChildren().isEmpty()) {
            // should use value-array as collection
            String[] values = paramNode.getValues();

            if (values == null) {
                return MISSING;
            }

            if (bindingAnnotations.annotations != null) {
                for (Annotation annotation : bindingAnnotations.annotations) {
                    if (annotation.annotationType().equals(As.class)) {
                        As as = ((As) annotation);
                        final String separator = as.value()[0];
						if (separator != null && StringUtils.isNotEmpty(separator)){
                        	values = values[0].split(separator);
						}
                    }
                }
            }

            Collection l = (Collection) clazz.newInstance();
            for (int i = 0; i < values.length; i++) {
                try {
                    Object value = directBind(paramNode.getOriginalKey(), bindingAnnotations.annotations, values[i], componentClass, componentClass);
                    l.add(value);
                } catch (Exception e) {
                    // Just ignore the exception and continue on the next item
                }
            }
            return l;
        }

        Collection r = (Collection) clazz.newInstance();

        if (List.class.isAssignableFrom(clazz)) {
            // Must add items at position resolved from each child's key
            List l = (List) r;

            // must get all indexes and sort them so we add items in correct order.
            Set<String> indexes = new TreeSet<String>(new Comparator<String>() {
                public int compare(String arg0, String arg1) {
                    try {
                        return Integer.parseInt(arg0) - Integer.parseInt(arg1);
                    } catch (NumberFormatException e) {
                        return arg0.compareTo(arg1);
                    }
                }
            });
            indexes.addAll(paramNode.getAllChildrenKeys());

            // get each value in correct order with index

            for (String index : indexes) {
                ParamNode child = paramNode.getChild(index);
                Object childValue = internalBind(child, componentClass, componentClass, bindingAnnotations);
                if (childValue != NO_BINDING && childValue != MISSING) {

                    // must make sure we place the value at the correct position
                    int pos = Integer.parseInt(index);
                    // must check if we must add empty elements before adding this item
                    int paddingCount = (l.size() - pos) * -1;
                    if (paddingCount > 0) {
                        for (int p = 0; p < paddingCount; p++) {
                            l.add(null);
                        }
                    }
                    l.add(childValue);
                }
            }

            return l;

        }

        for (ParamNode child : paramNode.getAllChildren()) {
            Object childValue = internalBind(child, componentClass, componentClass, bindingAnnotations);
            if (childValue != NO_BINDING && childValue != MISSING) {
                r.add(childValue);
            }
        }

        return r;
    }

    /**
     * @param value
     * @param clazz
     * @return
     * @throws Exception
     */
    public static Object directBind(String value, Class<?> clazz) throws Exception {
        return directBind(null, value, clazz, null);
    }

    /**
     * @param name
     * @param annotations
     * @param value
     * @param clazz
     * @param type
     * @return
     * @throws Exception
     */
    public static Object directBind(String name, Annotation[] annotations, String value, Class<?> clazz) throws Exception {
        return directBind(name, annotations, value, clazz, null);
    }

    /**
     * @param annotations
     * @param value
     * @param clazz
     * @param type
     * @return
     * @throws Exception
     */
    public static Object directBind(Annotation[] annotations, String value, Class<?> clazz, Type type) throws Exception {
        return directBind(null, annotations, value, clazz, type);
    }

    /**
     * This method calls the user's defined binders prior to bind simple type
     *
     * @param name
     * @param annotations
     * @param value
     * @param clazz
     * @param type
     * @return
     * @throws Exception
     */
    public static Object directBind(String name, Annotation[] annotations, String value, Class<?> clazz, Type type) throws Exception {
        // calls the direct binding and returns null if no value could be resolved..
        Object r = internalDirectBind(name, annotations, value, clazz, type);
        if ( r == DIRECTBINDING_NO_RESULT) {
            return null;
        } else {
            return r;
        }
    }

    // If internalDirectBind was not able to bind it, it returns a special variable instance: DIRECTBIND_MISSING
    // Needs this because sometimes we need to know if no value was returned..
    private static Object internalDirectBind(String name, Annotation[] annotations, String value, Class<?> clazz, Type type) throws Exception {
        boolean nullOrEmpty = value == null || value.trim().length() == 0;

        if (annotations != null) {
            for (Annotation annotation : annotations) {
                if (annotation.annotationType().equals(As.class)) {
                    Class<? extends TypeBinder<?>> toInstanciate = ((As) annotation).binder();
                    if (!(toInstanciate.equals(As.DEFAULT.class))) {
                        // Instantiate the binder
                        TypeBinder<?> myInstance = toInstanciate.newInstance();
                        return myInstance.bind(name, annotations, value, clazz, type);
                    }
                }
            }
        }

        // application custom types have higher priority. If unable to bind proceed with the next one
        for (Class<TypeBinder<?>> c : Play.classloader.getAssignableClasses(TypeBinder.class)) {
            if (c.isAnnotationPresent(Global.class)) {
                Class<?> forType = (Class) ((ParameterizedType) c.getGenericInterfaces()[0]).getActualTypeArguments()[0];
                if (forType.isAssignableFrom(clazz)) {
                    Object result = c.newInstance().bind(name, annotations, value, clazz, type);
                    if (result != null) {
                        return result;
                    }
                }
            }
        }

        // custom types
        for (Class<?> c : supportedTypes.keySet()) {
            if (Logger.isTraceEnabled()) {
                Logger.trace("directBind: value [" + value + "] c [" + c + "] Class [" + clazz + "]");
            }

            if (c.isAssignableFrom(clazz)) {
                if (Logger.isTraceEnabled()) {
                    Logger.trace("directBind: isAssignableFrom is true");
                }
                return supportedTypes.get(c).bind(name, annotations, value, clazz, type);
            }
        }

        // raw String
        if (clazz.equals(String.class)) {
            return value;
        }

        // Handles the case where the model property is a sole character
        if (clazz.equals(Character.class)) {
            return value.charAt(0);
        }

        // Enums
        if (Enum.class.isAssignableFrom(clazz)) {
            return nullOrEmpty ? null : Enum.valueOf((Class<Enum>) clazz, value);
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
            return nullOrEmpty ? null : new BigDecimal(value);
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

        return DIRECTBINDING_NO_RESULT;
    }


}
