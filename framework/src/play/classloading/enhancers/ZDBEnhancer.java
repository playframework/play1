package play.classloading.enhancers;

import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtMethod;
import javassist.CtNewConstructor;
import javassist.Modifier;
import play.classloading.ApplicationClasses.ApplicationClass;

/**
 * Enhance ZDBModel classes.
 */
public class ZDBEnhancer extends Enhancer {

    public void enhanceThisClass(ApplicationClass applicationClass) throws Exception {
        CtClass ctClass = makeClass(applicationClass);
        
        if(Modifier.isAbstract(ctClass.getModifiers())) {
            return;
        }

        if (!ctClass.subtypeOf(classPool.get("play.db.zdb.ZDBModel"))) {
            return;
        }

        // Ajoute le constructeur par défaut (obligatoire pour la peristence)
        try {
            boolean hasDefaultConstructor = false;
            for (CtConstructor constructor : ctClass.getConstructors()) {
                if (constructor.getParameterTypes().length == 0) {
                    hasDefaultConstructor = true;
                    break;
                }
            }
            if (!hasDefaultConstructor) {
                CtConstructor defaultConstructor = CtNewConstructor.make("public " + ctClass.getSimpleName() + "() {}", ctClass);
                ctClass.addConstructor(defaultConstructor);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Implémenter les méthodes statiques

        // in
        CtMethod in = CtMethod.make("public static play.db.zdb.ZDBModel$ZDBModelBucket in(String bucket) { return new play.db.zdb.ZDBModel$ZDBModelBucket(" + ctClass.getName() + ".class, bucket); }", ctClass);
        ctClass.addMethod(in);

        applicationClass.enhancedByteCode = ctClass.toBytecode();
        ctClass.defrost();
    }
}
