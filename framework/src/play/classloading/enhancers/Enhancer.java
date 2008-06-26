package play.classloading.enhancers;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import javassist.ClassPath;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;
import play.Play;
import play.classloading.ApplicationClasses.ApplicationClass;

public abstract class Enhancer {
    
    protected ClassPool classPool = new ClassPool();
    
    public Enhancer() {
        classPool.appendSystemPath();
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
            if(Play.classes.getApplicationClass(className) != null) {
                String cname = className.replace('.', '/') + ".class";
                try {
                    // return new File(cname).toURL();
                    return new URL("file:/ApplicationClassesClasspath/" + cname);
                }
                catch (MalformedURLException e) {}
            }
            return null;
        }

        public void close() {            
        }
        
    }

}
