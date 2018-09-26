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
import java.lang.reflect.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.util.*;

/**
 * The binder try to convert String values to Java objects.
 */
public abstract class Binder {
    public static final Object MISSING = new Object();
    private static final Object DIRECTBINDING_NO_RESULT = new Object();
    public static final Object NO_BINDING = new Object();

    static final Map<Class<?>, TypeBinder<?>> supportedTypes = new HashMap<>();

    // TODO: something a bit more dynamic? The As annotation allows you to inject your own binder
    static {
        supportedTypes.put(Date.class, new DateBinder());
        supportedTypes.put(DateTime.class, new DateTimeBinder());
        supportedTypes.put(File.class, new FileBinder());
        supportedTypes.put(File[].class, new FileArrayBinder());
        supportedTypes.put(LocalDateTime.class, new LocalDateTimeBinder());
        supportedTypes.put(Model.BinaryField.class, new BinaryBinder());
        supportedTypes.put(Upload.class, new UploadBinder());
        supportedTypes.put(Upload[].class, new UploadArrayBinder());
        supportedTypes.put(Calendar.class, new CalendarBinder());
        supportedTypes.put(Locale.class, new LocaleBinder());
        supportedTypes.put(byte[].class, new ByteArrayBinder());
        supportedTypes.put(byte[][].class, new ByteArrayArrayBinder());
    }

    /**
     * Add custom binder for any given class
     * 
     * E.g. @{code Binder.register(BigDecimal.class, new MyBigDecimalBinder());}
     * 
     * NB! Do not forget to UNREGISTER your custom binder when applications is reloaded (most probably in method
     * onApplicationStop()). Otherwise you will have a memory leak.
     * 
     * @param clazz
     *            The class to register
     * @param typeBinder
     *            The custom binder
     * @param <T>
     *            The Class type to register
     * @see #unregister(java.lang.Class)
     */
    public static <T> void register(Class<T> clazz, TypeBinder<T> typeBinder) {
        supportedTypes.put(clazz, typeBinder);
    }

    /**
     * Remove custom binder that was add with method #register(java.lang.Class, play.data.binding.TypeBinder)
     * 
     * @param clazz
     *            The class to remove the custom binder
     * @param <T>
     *            The Class type to register
     */
    public static <T> void unregister(Class<T> clazz) {
        supportedTypes.remove(clazz);
    }

    static Map<Class<?>, BeanWrapper> beanwrappers = new HashMap<>();

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
     * 
     * @param o
     *            Object to bind
     * @param name
     *            Name of the object
     * @param params
     *            List of the parameters
     * @return : The binding object
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

    public static Object bind(RootParamNode parentParamNode, String name, Class<?> clazz, Type type, Annotation[] annotations,
            MethodAndParamInfo methodAndParamInfo) {
        ParamNode paramNode = parentParamNode.getChild(name, true);

        Object result = null;
        if (paramNode == null) {
            result = MISSING;
        }

        BindingAnnotations bindingAnnotations = new BindingAnnotations(annotations);

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
                    Method defaultMethod = method.getDeclaringClass()
                            .getDeclaredMethod(method.getName() + "$default$" + methodAndParamInfo.parameterIndex);
                    return defaultMethod.invoke(methodAndParamInfo.objectInstance);
                } catch (NoSuchMethodException ignore) {
                } catch (Exception e) {
                    logBindingNormalFailure(paramNode, e);
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
                return bindMap(type, paramNode, bindingAnnotations);
            }

            if (Collection.class.isAssignableFrom(clazz)) {
                return bindCollection(clazz, type, paramNode, bindingAnnotations);
            }

            Object directBindResult = internalDirectBind(paramNode.getOriginalKey(), bindingAnnotations.annotations,
                    paramNode.getFirstValue(clazz), clazz, type);

            if (directBindResult != DIRECTBINDING_NO_RESULT) {
                // we found a value/result when direct binding
                return directBindResult;
            }

            // Must do the default array-check after direct binding, since some custom-binders checks for specific
            // arrays
            if (clazz.isArray()) {
                return bindArray(clazz, paramNode, bindingAnnotations);
            }

            if (!paramNode.getAllChildren().isEmpty()) {
                return internalBindBean(clazz, paramNode, bindingAnnotations);
            }

            return null; // give up
        } catch (NumberFormatException | ParseException e) {
            logBindingNormalFailure(paramNode, e);
            addValidationError(paramNode);
        } catch (Exception e) {
            // TODO This is bad catch. I would like to remove it in next version.
            logBindingUnexpectedFailure(paramNode, e);
            addValidationError(paramNode);
        }
        return MISSING;
    }

    private static void addValidationError(ParamNode paramNode) {
        Validation.addError(paramNode.getOriginalKey(), "validation.invalid");
    }

    private static void logBindingUnexpectedFailure(ParamNode paramNode, Exception e) {
        Logger.error(e, "Failed to bind %s=%s", paramNode.getOriginalKey(), Arrays.toString(paramNode.getValues()));
    }

    private static void logBindingNormalFailure(ParamNode paramNode, Exception e) {
        Logger.debug("Failed to bind %s=%s: %s", paramNode.getOriginalKey(), Arrays.toString(paramNode.getValues()), e);
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
                        String separator = as.value()[0];
                        values = values[0].split(separator);
                    }
                }
            }

            size = values.length;
            array = Array.newInstance(componentType, size);
            for (int i = 0; i < size; i++) {
                String thisValue = values[i];
                try {
                    Array.set(array, i - invalidItemsCount, directBind(paramNode.getOriginalKey(), bindingAnnotations.annotations,
                            thisValue, componentType, componentType));
                } catch (Exception e) {
                    Logger.debug("Bad item #%s: %s", i, e);
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
                        Logger.debug("Bad item #%s: %s", i, e);
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

    private static Object internalBindBean(Class<?> clazz, ParamNode paramNode, BindingAnnotations bindingAnnotations) {
        Object bean = createNewInstance(clazz);
        internalBindBean(paramNode, bean, bindingAnnotations);
        return bean;
    }

    private static <T> T createNewInstance(Class<T> clazz) {
        try {
            Constructor<T> constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            Logger.warn("Failed to create instance of %s: %s", clazz.getName(), e);
            throw new UnexpectedException(e);
        } catch (NoSuchMethodException | InvocationTargetException e) {
            Logger.error("Failed to create instance of %s: %s", clazz.getName(), e);
            throw new UnexpectedException(e);
        }
    }

    /**
     * Invokes the plugins before using the internal bindBean.
     * 
     * @param rootParamNode
     *            List of parameters
     * @param name
     *            The object name
     * @param bean
     *            the bean object
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
        } catch (NumberFormatException e) {
            logBindingNormalFailure(paramNode, e);
            addValidationError(paramNode);
        }

    }

    /**
     * Does NOT invoke plugins
     * 
     * @param paramNode
     *            List of parameters
     * @param bean
     *            the bean object
     * @param annotations
     *            annotations associated with the object
     */
    public static void bindBean(ParamNode paramNode, Object bean, Annotation[] annotations) {
        internalBindBean(paramNode, bean, new BindingAnnotations(annotations));
    }

    private static void internalBindBean(ParamNode paramNode, Object bean, BindingAnnotations bindingAnnotations) {

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
    private static Object bindEnum(Class<?> clazz, ParamNode paramNode) {
        if (paramNode.getValues() == null) {
            return MISSING;
        }

        String value = paramNode.getFirstValue(null);

        if (StringUtils.isEmpty(value)) {
            return null;
        }
        return Enum.valueOf((Class<? extends Enum>) clazz, value);
    }

    private static Object bindMap(Type type, ParamNode paramNode, BindingAnnotations bindingAnnotations) {
        Class keyClass = String.class;
        Class valueClass = String.class;
        if (type instanceof ParameterizedType) {
            keyClass = (Class) ((ParameterizedType) type).getActualTypeArguments()[0];
            valueClass = (Class) ((ParameterizedType) type).getActualTypeArguments()[1];
        }

        Map<Object, Object> r = new HashMap<>();

        for (ParamNode child : paramNode.getAllChildren()) {
            try {
                Object keyObject = directBind(paramNode.getOriginalKey(), bindingAnnotations.annotations, child.getName(), keyClass,
                        keyClass);
                Object valueObject = internalBind(child, valueClass, valueClass, bindingAnnotations);
                if (valueObject == NO_BINDING || valueObject == MISSING) {
                    valueObject = null;
                }
                r.put(keyObject, valueObject);
            } catch (ParseException | NumberFormatException e) {
                // Just ignore the exception and continue on the next item
                logBindingNormalFailure(paramNode, e);
            } catch (Exception e) {
                // TODO This is bad catch. I would like to remove it in next version.
                logBindingUnexpectedFailure(paramNode, e);
            }
        }

        return r;
    }

    @SuppressWarnings("unchecked")
    private static Object bindCollection(Class<?> clazz, Type type, ParamNode paramNode, BindingAnnotations bindingAnnotations) {
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
        Type componentType = String.class;
        if (type instanceof ParameterizedType) {
            componentType = ((ParameterizedType) type).getActualTypeArguments()[0];
            if (componentType instanceof ParameterizedType) {
                componentClass = (Class) ((ParameterizedType) componentType).getRawType();
            } else {
                componentClass = (Class) componentType;
            }
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
                        String separator = as.value()[0];
                        if (separator != null && !separator.isEmpty()) {
                            values = values[0].split(separator);
                        }
                    }
                }
            }

            Collection l;
            if (clazz.equals(EnumSet.class)) {
                l = EnumSet.noneOf(componentClass);
            } else {
                l = (Collection) createNewInstance(clazz);
            }
            boolean hasMissing = false;
            for (int i = 0; i < values.length; i++) {
                try {
                    Object value = internalDirectBind(paramNode.getOriginalKey(), bindingAnnotations.annotations, values[i], componentClass,
                            componentType);
                    if (value == DIRECTBINDING_NO_RESULT) {
                        hasMissing = true;
                    } else {
                        l.add(value);
                    }
                } catch (Exception e) {
                    // Just ignore the exception and continue on the next item
                    logBindingNormalFailure(paramNode, e); // TODO debug or error?
                }
            }
            if (hasMissing && l.size() == 0) {
                return MISSING;
            }
            return l;
        }

        Collection r = (Collection) createNewInstance(clazz);

        if (List.class.isAssignableFrom(clazz)) {
            // Must add items at position resolved from each child's key
            List l = (List) r;

            // must get all indexes and sort them so we add items in correct order.
            Set<String> indexes = new TreeSet<>(new Comparator<String>() {
                @Override
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
                Object childValue = internalBind(child, componentClass, componentType, bindingAnnotations);
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
            Object childValue = internalBind(child, componentClass, componentType, bindingAnnotations);
            if (childValue != NO_BINDING && childValue != MISSING) {
                r.add(childValue);
            }
        }

        return r;
    }

    /**
     * Bind a object
     * 
     * @param value
     *            value to bind
     * @param clazz
     *            class of the object
     * @return The binding object
     * @throws Exception
     *             if problem occurred during binding
     */
    public static Object directBind(String value, Class<?> clazz) throws Exception {
        return directBind(null, value, clazz, null);
    }

    /**
     * Bind a object
     * 
     * @param name
     *            name of the object
     * @param annotations
     *            annotation on the object
     * @param value
     *            Value to bind
     * @param clazz
     *            The class of the object
     * 
     * @return The binding object
     * @throws Exception
     *             if problem occurred during binding
     */
    public static Object directBind(String name, Annotation[] annotations, String value, Class<?> clazz) throws Exception {
        return directBind(name, annotations, value, clazz, null);
    }

    /**
     * Bind a object
     * 
     * @param annotations
     *            annotation on the object
     * @param value
     *            value to bind
     * @param clazz
     *            class of the object
     * @param type
     *            type to bind
     * @return The binding object
     * @throws Exception
     *             if problem occurred during binding
     */
    public static Object directBind(Annotation[] annotations, String value, Class<?> clazz, Type type) throws Exception {
        return directBind(null, annotations, value, clazz, type);
    }

    /**
     * This method calls the user's defined binders prior to bind simple type
     * 
     * @param name
     *            name of the object
     * @param annotations
     *            annotation on the object
     * @param value
     *            value to bind
     * @param clazz
     *            class of the object
     * @param type
     *            type to bind
     * @return The binding object
     * @throws Exception
     *             if problem occurred during binding
     */
    public static Object directBind(String name, Annotation[] annotations, String value, Class<?> clazz, Type type) throws Exception {
        // calls the direct binding and returns null if no value could be resolved..
        Object r = internalDirectBind(name, annotations, value, clazz, type);
        if (r == DIRECTBINDING_NO_RESULT) {
            return null;
        } else {
            return r;
        }
    }

    // If internalDirectBind was not able to bind it, it returns a special variable instance: DIRECTBIND_MISSING
    // Needs this because sometimes we need to know if no value was returned..
    private static Object internalDirectBind(String name, Annotation[] annotations, String value, Class<?> clazz, Type type)
            throws Exception {
        boolean nullOrEmpty = value == null || value.trim().length() == 0;

        if (annotations != null) {
            for (Annotation annotation : annotations) {
                if (annotation.annotationType().equals(As.class)) {
                    Class<? extends TypeBinder<?>> toInstantiate = ((As) annotation).binder();
                    if (!(toInstantiate.equals(As.DEFAULT.class))) {
                        // Instantiate the binder
                        TypeBinder<?> myInstance = createNewInstance(toInstantiate);
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
                    Object result = createNewInstance(c).bind(name, annotations, value, clazz, type);
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

        // BigInteger binding
        if (clazz.equals(BigInteger.class)) {
            return nullOrEmpty ? null : new BigInteger(value);
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
