package play.classloading.enhancers;

import java.util.ArrayList;
import java.util.List;
import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;
import org.apache.commons.javaflow.bytecode.transformation.asm.AsmClassTransformer;
import play.classloading.ApplicationClasses.ApplicationClass;

public class ContinuationEnhancer extends Enhancer {

    static final List<String> continuationMethods = new ArrayList<String>();

    static {
        continuationMethods.add("play.mvc.Controller.await(java.lang.String)");
        continuationMethods.add("play.mvc.Controller.await(int)");
        continuationMethods.add("play.mvc.Controller.await(java.util.concurrent.Future)");
        continuationMethods.add("play.mvc.WebSocketController.await(java.lang.String)");
        continuationMethods.add("play.mvc.WebSocketController.await(int)");
        continuationMethods.add("play.mvc.WebSocketController.await(java.util.concurrent.Future)");
    }

    @Override
    public void enhanceThisClass(ApplicationClass applicationClass) throws Exception {
        if (isScala(applicationClass)) {
            return;
        }

        CtClass ctClass = makeClass(applicationClass);

        if (!ctClass.subtypeOf(classPool.get(ControllersEnhancer.ControllerSupport.class.getName()))) {
            return;
        }

        final boolean[] needsContinuations = new boolean[]{false};

        for (CtMethod m : ctClass.getDeclaredMethods()) {
            m.instrument(new ExprEditor() {

                @Override
                public void edit(MethodCall m) throws CannotCompileException {
                    try {
                        if (continuationMethods.contains(m.getMethod().getLongName())) {
                            needsContinuations[0] = true;
                        }
                    } catch (Exception e) {
                    }
                }
            });

            if (needsContinuations[0]) {
                break;
            }
        }

        if (!needsContinuations[0]) {
            return;
        }

        // Apply continuations
        applicationClass.enhancedByteCode = new AsmClassTransformer().transform(applicationClass.enhancedByteCode);

        ctClass.defrost();
    }
}
