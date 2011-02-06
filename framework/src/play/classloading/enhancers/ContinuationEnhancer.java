package play.classloading.enhancers;

import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;
import org.apache.commons.javaflow.bytecode.transformation.asm.AsmClassTransformer;
import play.classloading.ApplicationClasses.ApplicationClass;

public class ContinuationEnhancer extends Enhancer {

    @Override
    public void enhanceThisClass(ApplicationClass applicationClass) throws Exception {

        CtClass ctClass = makeClass(applicationClass);

        if (!ctClass.subtypeOf(classPool.get(ControllersEnhancer.ControllerSupport.class.getName()))) {
            return;
        }

        final boolean[] needsContinuations = new boolean[] {false};

        for(CtMethod m : ctClass.getDeclaredMethods()) {
            m.instrument(new ExprEditor() {

                @Override
                public void edit(MethodCall m) throws CannotCompileException {
                    try {
                        if(m.getMethod().getLongName().startsWith("play.mvc.Controller.waitAndContinue(")) {
                            needsContinuations[0] = true;
                        }
                    } catch(Exception e) {                        
                    }
                }

            });

            if(needsContinuations[0]) {
                break;
            }
        }

        if(!needsContinuations[0]) {
            return;
        }

        // Apply continuations
        applicationClass.enhancedByteCode = new AsmClassTransformer().transform(applicationClass.enhancedByteCode);

        ctClass.defrost();
    }

}
