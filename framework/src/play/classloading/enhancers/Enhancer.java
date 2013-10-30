package play.classloading.enhancers;

import java.io.File;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileInputStream;
import java.lang.annotation.Annotation;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import javassist.ClassPath;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;
import javassist.LoaderClassPath;
import javassist.NotFoundException;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.annotation.MemberValue;
import play.Play;
import play.Logger;
import play.classloading.ApplicationClasses.ApplicationClass;

/**
 * Enhancer support
 */
public abstract class Enhancer {

    protected ClassPool classPool;

    public Enhancer() {
        this.classPool = newClassPool();
    }
    
    public static ClassPool newClassPool() {
        ClassPool classPool = new ClassPool();
        classPool.appendSystemPath();
        classPool.appendClassPath(new LoaderClassPath(Enhancer.class.getClassLoader()));
        classPool.appendClassPath(new ApplicationClassesClasspath());
        return classPool;
    }

    /**
     * Construct a javassist CtClass from an application class.
     */
    public CtClass makeClass(ApplicationClass applicationClass) throws IOException {
        return classPool.makeClass(new ByteArrayInputStream(applicationClass.enhancedByteCode));
    }

    /**
     * The magic happen here...
     */
    public abstract void enhanceThisClass(ApplicationClass applicationClass) throws Exception;

    /**
     * Dumb classpath implementation for javassist hacking
     */
    public static class ApplicationClassesClasspath implements ClassPath {

        public InputStream openClassfile(String className) throws NotFoundException {

            if(Play.usePrecompiled) {
                try {
                    File file = Play.getFile("precompiled/java/" + className.replace(".", "/") + ".class");
                    return new FileInputStream(file);
                } catch(Exception e) {
                    Logger.error("Missing class %s", className);
                }
            }
            ApplicationClass appClass = Play.classes.getApplicationClass(className);

            if ( appClass.enhancedByteCode == null) {
                throw new RuntimeException("Trying to visit uncompiled class while enhancing. Uncompiled class: " + className);
            }

            return new ByteArrayInputStream(appClass.enhancedByteCode);
        }

        public URL find(String className) {
            if (Play.classes.getApplicationClass(className) != null) {
                String cname = className.replace('.', '/') + ".class";
                try {
                    // return new File(cname).toURL();
                    return new URL("file:/ApplicationClassesClasspath/" + cname);
                } catch (MalformedURLException e) {
                }
            }
            return null;
        }

        public void close() {
        }
    }

    /**
     * Test if a class has the provided annotation 
     * @param ctClass the javassist class representation 
     * @param annotation fully qualified name of the annotation class eg."javax.persistence.Entity"
     * @return true if class has the annotation
     * @throws java.lang.ClassNotFoundException
     */
    protected boolean hasAnnotation(CtClass ctClass, String annotation) throws ClassNotFoundException {
        for (Object object : ctClass.getAvailableAnnotations()) {
            Annotation ann = (Annotation) object;
            if (ann.annotationType().getName().equals(annotation)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Test if a field has the provided annotation 
     * @param ctField the javassist field representation 
     * @param annotation fully qualified name of the annotation class eg."javax.persistence.Entity"
     * @return true if field has the annotation
     * @throws java.lang.ClassNotFoundException
     */    
    protected boolean hasAnnotation(CtField ctField, String annotation) throws ClassNotFoundException {
        for (Object object : ctField.getAvailableAnnotations()) {
            Annotation ann = (Annotation) object;
            if (ann.annotationType().getName().equals(annotation)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Test if a method has the provided annotation
	 * @param ctMethod the javassist method representation
	 * @param annotation fully qualified name of the annotation class eg."javax.persistence.Entity"
	 * @return true if field has the annotation
	 * @throws java.lang.ClassNotFoundException
	 */
    protected boolean hasAnnotation(CtMethod ctMethod, String annotation) throws ClassNotFoundException {
        for (Object object : ctMethod.getAvailableAnnotations()) {
            Annotation ann = (Annotation) object;
            if (ann.annotationType().getName().equals(annotation)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Create a new annotation to be dynamically inserted in the byte code.
     */
    protected static void createAnnotation(AnnotationsAttribute attribute, Class<? extends Annotation> annotationType, Map<String, MemberValue> members) {
        javassist.bytecode.annotation.Annotation annotation = new javassist.bytecode.annotation.Annotation(annotationType.getName(), attribute.getConstPool());
        for (Map.Entry<String, MemberValue> member : members.entrySet()) {
            annotation.addMemberValue(member.getKey(), member.getValue());
        }
        attribute.addAnnotation(annotation);
    }

    /**
     * Create a new annotation to be dynamically inserted in the byte code.
     */    
    protected static void createAnnotation(AnnotationsAttribute attribute, Class<? extends Annotation> annotationType) {
        createAnnotation(attribute, annotationType, new HashMap<String, MemberValue>());
    }

    /**
     * Retrieve all class annotations.
     */
    protected static AnnotationsAttribute getAnnotations(CtClass ctClass) {
        AnnotationsAttribute annotationsAttribute = (AnnotationsAttribute) ctClass.getClassFile().getAttribute(AnnotationsAttribute.visibleTag);
        if (annotationsAttribute == null) {
            annotationsAttribute = new AnnotationsAttribute(ctClass.getClassFile().getConstPool(), AnnotationsAttribute.visibleTag);
            ctClass.getClassFile().addAttribute(annotationsAttribute);
        }
        return annotationsAttribute;
    }

    /**
     * Retrieve all field annotations.
     */    
    protected static AnnotationsAttribute getAnnotations(CtField ctField) {
        AnnotationsAttribute annotationsAttribute = (AnnotationsAttribute) ctField.getFieldInfo().getAttribute(AnnotationsAttribute.visibleTag);
        if (annotationsAttribute == null) {
            annotationsAttribute = new AnnotationsAttribute(ctField.getFieldInfo().getConstPool(), AnnotationsAttribute.visibleTag);
            ctField.getFieldInfo().addAttribute(annotationsAttribute);
        }
        return annotationsAttribute;
    }

    /**
     * Retrieve all method annotations.
     */    
    protected static AnnotationsAttribute getAnnotations(CtMethod ctMethod) {
        AnnotationsAttribute annotationsAttribute = (AnnotationsAttribute) ctMethod.getMethodInfo().getAttribute(AnnotationsAttribute.visibleTag);
        if (annotationsAttribute == null) {
            annotationsAttribute = new AnnotationsAttribute(ctMethod.getMethodInfo().getConstPool(), AnnotationsAttribute.visibleTag);
            ctMethod.getMethodInfo().addAttribute(annotationsAttribute);
        }
        return annotationsAttribute;
    }

    boolean isScalaObject(CtClass ctClass) throws Exception {
        for(CtClass i : ctClass.getInterfaces()) {
            if(i.getName().equals("scala.ScalaObject")) {
                return true;
            }
        }
        return false;
    }

    boolean isScala(ApplicationClass app) {
        return app.javaFile.getName().endsWith(".scala");
    }

    boolean isAnon(ApplicationClass app) {
        return app.name.contains("$anonfun$") || app.name.contains("$anon$");
    }
    
}
