package play.classloading.enhancers;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import javassist.CannotCompileException;
import javassist.CtBehavior;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtField;
import javassist.CtMethod;
import javassist.CtNewConstructor;
import javassist.NotFoundException;
import javassist.expr.ExprEditor;
import javassist.expr.FieldAccess;
import play.Logger;
import play.Play;
import play.classloading.ApplicationClasses.ApplicationClass;
import play.exceptions.UnexpectedException;

/**
 * Generate valid JavaBeans.
 */
public class PropertiesEnhancer extends Enhancer {

    private boolean enabled = Boolean.parseBoolean(Play.configuration.getProperty("play.propertiesEnhancer.enabled", "true"));
    private boolean generateAccessors = Boolean.parseBoolean(Play.configuration.getProperty("play.propertiesEnhancer.generateAccessors", "true"));

    @Override
    public void enhanceThisClass(ApplicationClass applicationClass) throws Exception {

        if (!enabled) return;

        CtClass ctClass = makeClass(applicationClass);
        if (ctClass.isInterface() || ctClass.getName().endsWith(".package")) {
            return;
        }

        addDefaultConstructor(ctClass);

        if (generateAccessors && !isScala(applicationClass)) { // Temporary hack for Scala: skip generating getters/setters
            generateAccessors(ctClass);
            addDefaultConstructor2(ctClass);
            interceptAllFieldsAccess(ctClass);
        }

        applicationClass.enhancedByteCode = ctClass.toBytecode();
        ctClass.defrost();
    }

    private void addDefaultConstructor(CtClass ctClass) {
        try {
            boolean hasDefaultConstructor = hasDefaultConstructor(ctClass);
            if (!hasDefaultConstructor && !ctClass.isInterface()) {
                CtConstructor defaultConstructor = CtNewConstructor.make("public " + ctClass.getSimpleName() + "() {}", ctClass);
                ctClass.addConstructor(defaultConstructor);
            }
        } catch (Exception e) {
            Logger.error(e, "Failed to generate default constructor for " + ctClass.getName());
            throw new UnexpectedException("Failed to generate default constructor for " + ctClass.getName(), e);
        }
    }

    private void addDefaultConstructor2(CtClass ctClass) {
        try {
            if (!hasDefaultConstructor(ctClass)) {
                CtConstructor defaultConstructor = CtNewConstructor.defaultConstructor(ctClass);
                ctClass.addConstructor(defaultConstructor);
            }
        } catch (Exception e) {
            Logger.error(e, "Failed to generate default constructor for " + ctClass.getName());
            throw new UnexpectedException("Failed to generate default constructor for " + ctClass.getName(), e);
        }
    }

    private boolean hasDefaultConstructor(CtClass ctClass) throws NotFoundException {
        for (CtConstructor constructor : ctClass.getDeclaredConstructors()) {
            if (constructor.getParameterTypes().length == 0) {
                return true;
            }
        }
        return false;
    }

    private void generateAccessors(CtClass ctClass) {
        for (CtField ctField : ctClass.getDeclaredFields()) {
            try {

                if (isProperty(ctField)) {

                    // Property name
                    String propertyName = ctField.getName().substring(0, 1).toUpperCase() + ctField.getName().substring(1);
                    String getter = "get" + propertyName;
                    String setter = "set" + propertyName;

                    try {
                        CtMethod ctMethod = ctClass.getDeclaredMethod(getter);
                        if (ctMethod.getParameterTypes().length > 0 || Modifier.isStatic(ctMethod.getModifiers())) {
                            throw new NotFoundException("it's not a getter !");
                        }
                    } catch (NotFoundException noGetter) {

                        // Getter creation
                        String code = "public " + ctField.getType().getName() + " " + getter + "() { return this." + ctField.getName() + "; }";
                        CtMethod getMethod = CtMethod.make(code, ctClass);
                        ctClass.addMethod(getMethod);
                        createAnnotation(getAnnotations(getMethod), PlayPropertyAccessor.class);
                    }

                    if (!isFinal(ctField)) {
                        try {
                            CtMethod ctMethod = ctClass.getDeclaredMethod(setter);
                            if (ctMethod.getParameterTypes().length != 1 || !ctMethod.getParameterTypes()[0].equals(ctField.getType()) || Modifier.isStatic(ctMethod.getModifiers())) {
                                throw new NotFoundException("it's not a setter !");
                            }
                        } catch (NotFoundException noSetter) {
                            // Setter creation
                            CtMethod setMethod = CtMethod.make("public void " + setter + "(" + ctField.getType().getName() + " value) { this." + ctField.getName() + " = value; }", ctClass);
                            ctClass.addMethod(setMethod);
                            createAnnotation(getAnnotations(setMethod), PlayPropertyAccessor.class);
                        }
                    }

                }

            } catch (Exception e) {
                String message = "Failed to generate default accessor for " + ctClass.getName() + "." + ctField.getName();
                Logger.error(e, message);
                throw new UnexpectedException(message, e);
            }
        }
    }

    private void interceptAllFieldsAccess(final CtClass ctClass) throws CannotCompileException {
        for (final CtBehavior ctMethod : ctClass.getDeclaredBehaviors()) {
            ctMethod.instrument(new ExprEditor() {

                @Override
                public void edit(FieldAccess fieldAccess) throws CannotCompileException {
                    try {

                        // Check access to property ?
                        if (isProperty(fieldAccess.getField())) {

                            // TODO : Check if it is a application class field (fieldAccess.getClassName())

                            // Getter or setter ?
                            String propertyName = null;

                            if (fieldAccess.getField().getDeclaringClass().equals(ctMethod.getDeclaringClass())
                                    || ctMethod.getDeclaringClass().subclassOf(fieldAccess.getField().getDeclaringClass())) {
                                if ((ctMethod.getName().startsWith("get") || (!isFinal(fieldAccess.getField()) && ctMethod.getName().startsWith("set"))) && ctMethod.getName().length() > 3) {
                                    propertyName = ctMethod.getName().substring(3);
                                    propertyName = propertyName.substring(0, 1).toLowerCase() + propertyName.substring(1);
                                }
                            }

                            // To intercept getter of its own property
                            if (propertyName == null || !propertyName.equals(fieldAccess.getFieldName())) {

                                String invocationPoint = ctClass.getName() + "." + ctMethod.getName() + ", line " + fieldAccess.getLineNumber();

                                if (fieldAccess.isReader()) {

                                    // Rewrite read access to the property
                                    fieldAccess.replace("$_ = ($r)play.classloading.enhancers.PropertiesEnhancer.FieldAccessor.invokeReadProperty($0, \"" + fieldAccess.getFieldName() + "\", \"" + fieldAccess.getClassName() + "\", \"" + invocationPoint + "\");");

                                } else if (!isFinal(fieldAccess.getField()) && fieldAccess.isWriter()) {

                                    // Rewrite write access to the property
                                    fieldAccess.replace("play.classloading.enhancers.PropertiesEnhancer.FieldAccessor.invokeWriteProperty($0, \"" + fieldAccess.getFieldName() + "\", " + fieldAccess.getField().getType().getName() + ".class, $1, \"" + fieldAccess.getClassName() + "\", \"" + invocationPoint + "\");");


                                }
                            }
                        }

                    } catch (Exception e) {
                        String message = "Failed to modify access to " + ctClass.getName() + "." + ctMethod.getName();
                        throw new UnexpectedException(message, e);
                    }
                }
            });
        }
    }

    /**
     * Is this field a valid javabean property ?
     */
    boolean isProperty(CtField ctField) {
        if (ctField.getName().equals(ctField.getName().toUpperCase()) || ctField.getName().substring(0, 1).equals(ctField.getName().substring(0, 1).toUpperCase())) {
            return false;
        }
        final int classModifiers = ctField.getDeclaringClass().getModifiers();
		return Modifier.isPublic(ctField.getModifiers()) 
				&& !Modifier.isStatic(ctField.getModifiers())
                && (Modifier.isPublic(classModifiers) || Modifier.isProtected(classModifiers));
    }

    /**
     * Is this field final ?
     */
    boolean isFinal(CtField ctField) {
        return Modifier.isFinal(ctField.getModifiers());
    }

    /**
     * Runtime part.
     */
    public static class FieldAccessor {

        public static Object invokeReadProperty(Object o, String property, String targetType, String invocationPoint) throws Throwable {
            if (o == null) {
                throw new NullPointerException("Try to read " + property + " on null object " + targetType + " (" + invocationPoint + ")");
            }
            if (o.getClass().getClassLoader() == null || !o.getClass().getClassLoader().equals(Play.classloader)) {
                return o.getClass().getField(property).get(o);
            }
            String getter = "get" + property.substring(0, 1).toUpperCase() + property.substring(1);
            try {
                Method getterMethod = o.getClass().getMethod(getter);
                Object result = getterMethod.invoke(o);
                return result;
            } catch (NoSuchMethodException e) {
                return o.getClass().getField(property).get(o);
            } catch (InvocationTargetException e) {
                throw e.getCause();
            }
        }

        public static void invokeWriteProperty(Object o, String property, Class<?> valueType, boolean value, String targetType, String invocationPoint) throws Throwable {
            invokeWriteProperty(o, property, valueType, Boolean.valueOf(value), targetType, invocationPoint);
        }

        public static void invokeWriteProperty(Object o, String property, Class<?> valueType, byte value, String targetType, String invocationPoint) throws Throwable {
            invokeWriteProperty(o, property, valueType, Byte.valueOf(value), targetType, invocationPoint);
        }

        public static void invokeWriteProperty(Object o, String property, Class<?> valueType, char value, String targetType, String invocationPoint) throws Throwable {
            invokeWriteProperty(o, property, valueType, Character.valueOf(value), targetType, invocationPoint);
        }

        public static void invokeWriteProperty(Object o, String property, Class<?> valueType, double value, String targetType, String invocationPoint) throws Throwable {
            invokeWriteProperty(o, property, valueType, Double.valueOf(value), targetType, invocationPoint);
        }

        public static void invokeWriteProperty(Object o, String property, Class<?> valueType, float value, String targetType, String invocationPoint) throws Throwable {
            invokeWriteProperty(o, property, valueType, Float.valueOf(value), targetType, invocationPoint);
        }

        public static void invokeWriteProperty(Object o, String property, Class<?> valueType, int value, String targetType, String invocationPoint) throws Throwable {
            invokeWriteProperty(o, property, valueType, Integer.valueOf(value), targetType, invocationPoint);
        }

        public static void invokeWriteProperty(Object o, String property, Class<?> valueType, long value, String targetType, String invocationPoint) throws Throwable {
            invokeWriteProperty(o, property, valueType, Long.valueOf(value), targetType, invocationPoint);
        }

        public static void invokeWriteProperty(Object o, String property, Class<?> valueType, short value, String targetType, String invocationPoint) throws Throwable {
            invokeWriteProperty(o, property, valueType, Short.valueOf(value), targetType, invocationPoint);
        }

        public static void invokeWriteProperty(Object o, String property, Class<?> valueType, Object value, String targetType, String invocationPoint) throws Throwable {
            if (o == null) {
                throw new NullPointerException("Attempting to write a property " + property + " on a null object of type " + targetType + " (" + invocationPoint + ")");
            }
            String setter = "set" + property.substring(0, 1).toUpperCase() + property.substring(1);
            try {
                Method setterMethod = o.getClass().getMethod(setter, valueType);
                setterMethod.invoke(o, value);
            } catch (NoSuchMethodException e) {
                o.getClass().getField(property).set(o, value);
            } catch (InvocationTargetException e) {
                throw e.getCause();
            }
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface PlayPropertyAccessor {
    }
}
