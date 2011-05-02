package play.classloading.enhancers;

import javassist.CtClass;
import javassist.CtMethod;
import javassist.Modifier;
import play.Logger;
import play.classloading.ApplicationClasses.ApplicationClass;
import play.exceptions.UnexpectedException;
import play.mvc.Mailer;

/**
 * Enhance mailers classes. 
 */
public class MailerEnhancer extends Enhancer {

    @Override
    public void enhanceThisClass(ApplicationClass applicationClass) throws Exception {
        if (isScala(applicationClass)) {
            return;
        }

        CtClass ctClass = makeClass(applicationClass);

        if (!ctClass.subtypeOf(classPool.get(Mailer.class.getName()))) {
            return;
        }

        for (final CtMethod ctMethod : ctClass.getDeclaredMethods()) {

            if (Modifier.isPublic(ctMethod.getModifiers()) && Modifier.isStatic(ctMethod.getModifiers()) && ctMethod.getReturnType().isPrimitive()) {
                try {
                    ctMethod.insertBefore("if(infos.get() != null) {play.Logger.warn(\"You call " + ctMethod.getLongName() + " from \" + ((java.util.Map)infos.get()).get(\"method\") + \". It's forbidden in a Mailer. It will propably fail...\", new Object[0]);}; infos.set(new java.util.HashMap());((java.util.Map)infos.get()).put(\"method\", \"" + ctMethod.getLongName() + "\");");
                    ctMethod.insertAfter("infos.set(null);", true);
                } catch (Exception e) {
                    Logger.error(e, "Error in MailerEnhancer");
                    throw new UnexpectedException("Error in MailerEnhancer", e);
                }
            }

        }

        // Done.
        applicationClass.enhancedByteCode = ctClass.toBytecode();
        ctClass.defrost();

    }
}
