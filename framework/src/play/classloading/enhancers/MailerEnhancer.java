package play.classloading.enhancers;

import javassist.CtClass;
import javassist.CtMethod;
import javassist.Modifier;
import play.Logger;
import play.classloading.ApplicationClasses.ApplicationClass;
import play.mvc.Mailer;

/**
 * Enhance mailers classes.
 */
public class MailerEnhancer extends Enhancer {

    @Override
    public void enhanceThisClass(ApplicationClass applicationClass) throws Exception {
        CtClass ctClass = makeClass(applicationClass);

        if (!ctClass.subtypeOf(classPool.get(Mailer.class.getName()))) {
            return;
        }

        for (final CtMethod ctMethod : ctClass.getDeclaredMethods()) {

            if (Modifier.isPublic(ctMethod.getModifiers()) && Modifier.isStatic(ctMethod.getModifiers())) {
                try {
                    ctMethod.insertBefore("infos.set(new java.util.HashMap());((java.util.Map)infos.get()).put(\"method\", \"" + ctMethod.getLongName() + "\");");
                    ctMethod.insertAfter("infos.set(null);", true);
                } catch (Exception e) {
                    Logger.error(e, "Error in ControllersEnhancer");
                }
            }

        }

        applicationClass.enhancedByteCode = ctClass.toBytecode();
        ctClass.defrost();

    }
}
