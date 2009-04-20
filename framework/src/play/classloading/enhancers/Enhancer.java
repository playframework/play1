package play.classloading.enhancers;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.net.MalformedURLException;
import java.net.URL;
import javassist.ClassPath;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.LoaderClassPath;
import javassist.NotFoundException;
import play.Play;
import play.classloading.ApplicationClasses.ApplicationClass;

/**
 * Enhancer support
 */
public abstract class Enhancer { 

    protected ClassPool classPool = new ClassPool();

    public Enhancer() {
        classPool.appendSystemPath();
        classPool.appendClassPath(new LoaderClassPath(Enhancer.class.getClassLoader()));
        classPool.appendClassPath(new ApplicationClassesClasspath());
    }

    CtClass makeClass(ApplicationClass applicationClass) throws IOException {
        return classPool.makeClass(new ByteArrayInputStream(applicationClass.enhancedByteCode));
    }

    public abstract void enhanceThisClass(ApplicationClass applicationClass) throws Exception;

    public static class ApplicationClassesClasspath implements ClassPath {

        public InputStream openClassfile(String className) throws NotFoundException {
            return new ByteArrayInputStream(Play.classes.getApplicationClass(className).enhancedByteCode);
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
     * test if a class has the provided annotation 
     * @param ctClass the javassist class representation 
     * @param annotation fully qualified name of the annotation class eg."javax.persistence.Entity"
     * @return true if class has the annotation
     * @throws java.lang.ClassNotFoundException
     */
    protected boolean hasAnnotation(CtClass ctClass, String annotation) throws ClassNotFoundException {
        for (Object object : ctClass.getAnnotations()) {
            Annotation ann = (Annotation) object;
            if (ann.annotationType().getName().equals(annotation)) {
                return true;
            }
        }
        return false;
    }
}
