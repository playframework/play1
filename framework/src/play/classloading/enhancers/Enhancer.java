package play.classloading.enhancers;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import javassist.ClassPool;
import javassist.CtClass;
import play.classloading.ApplicationClasses.ApplicationClass;

public abstract class Enhancer {
    
    static ClassPool classPool = ClassPool.getDefault();
    
    CtClass makeClass(ApplicationClass applicationClass) throws IOException {
        return classPool.makeClass(new ByteArrayInputStream(applicationClass.javaByteCode));
    }
    
    public abstract void enhanceThisClass(ApplicationClass applicationClass) throws Exception;

}
