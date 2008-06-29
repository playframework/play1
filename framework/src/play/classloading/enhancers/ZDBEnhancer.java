package play.classloading.enhancers;

import javassist.CtClass;
import javassist.CtMethod;
import play.classloading.ApplicationClasses.ApplicationClass;

public class ZDBEnhancer extends Enhancer {

    public void enhanceThisClass(ApplicationClass applicationClass) throws Exception {
        CtClass ctClass = makeClass(applicationClass);

        if (!ctClass.subtypeOf(classPool.get("play.db.zdb.ZDBModel"))) {
            return;
        }

        // Implémenter les méthodes statiques

        // in
        CtMethod in = CtMethod.make("public static play.db.zdb.ZDBModel$ZDBModelBucket in(String bucket) { return new play.db.zdb.ZDBModel$ZDBModelBucket(" + ctClass.getName() + ".class, bucket); }", ctClass);
        ctClass.addMethod(in);

        applicationClass.enhancedByteCode = ctClass.toBytecode();
        ctClass.defrost();
    }
}
