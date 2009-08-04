package play.modules.spring;

import javassist.CtClass;
import javassist.CtField;
import play.classloading.ApplicationClasses.ApplicationClass;
import play.classloading.enhancers.Enhancer;

public class SpringEnhancer extends Enhancer {

    @Override
    public void enhanceThisClass(ApplicationClass applicationClass) throws Exception {
        final CtClass ctClass = makeClass(applicationClass);
        if (ctClass.isInterface()) {
            return;
        }
        for (CtField ctField : ctClass.getDeclaredFields()) {
            if(hasAnnotation(ctField, "javax.inject.Inject")) {
                //ctClass.removeField(ctField);
                //CtField newCtField = new CtField(ctField.getType(), ctField.getName(), ctClass);
                //newCtField.setModifiers(ctField.getModifiers());
                //ctClass.addField(newCtField, "play.modules.spring.Spring.getBeanOfType(\""+newCtField.getType().getName()+"\");");
            }
        }
        applicationClass.enhancedByteCode = ctClass.toBytecode();
        ctClass.defrost();
    }

}
