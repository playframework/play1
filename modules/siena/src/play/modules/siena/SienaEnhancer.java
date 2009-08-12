package play.modules.siena;

import javassist.CtClass;
import javassist.CtMethod;
import play.classloading.ApplicationClasses.ApplicationClass;
import play.classloading.enhancers.Enhancer;

public class SienaEnhancer extends Enhancer {

    @Override
    public void enhanceThisClass(ApplicationClass applicationClass) throws Exception {
        CtClass ctClass = makeClass(applicationClass);

        if (!ctClass.subtypeOf(classPool.get("siena.Model"))) {
            return;
        }

        try {
            ctClass.getDeclaredMethod("all", new CtClass[0]);
        } catch (Exception e) {
            // Add all() method
            CtMethod all = CtMethod.make("public static siena.Query all() { return siena.Model.all(" + ctClass.getName() + " .class); }", ctClass);
            ctClass.addMethod(all);
        }

        // Done.
        applicationClass.enhancedByteCode = ctClass.toBytecode();
        ctClass.defrost();
    }
}
