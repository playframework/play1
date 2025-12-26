package play.classloading.enhancers;

import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;
import org.apache.commons.javaflow.bytecode.transformation.asm.AsmClassTransformer;
import play.Play;
import play.classloading.ApplicationClasses.ApplicationClass;

import java.io.InputStream;
import java.util.List;

public class ContinuationEnhancer extends Enhancer {

    private static final List<String> continuationMethods = List.of(
        "play.mvc.Controller.await(java.lang.String)",
        "play.mvc.Controller.await(int)",
        "play.mvc.Controller.await(java.util.concurrent.Future)",
        "play.mvc.WebSocketController.await(java.lang.String)",
        "play.mvc.WebSocketController.await(int)",
        "play.mvc.WebSocketController.await(java.util.concurrent.Future)"
    );

    public static boolean isEnhanced(String appClassName) {
        ApplicationClass appClass = Play.classes.getApplicationClass( appClassName);

        // All classes enhanced for Continuations are implementing the interface EnhancedForContinuations
        return appClass != null && EnhancedForContinuations.class.isAssignableFrom(appClass.javaClass);
    }

    @Override
    public void enhanceThisClass(ApplicationClass applicationClass) throws Exception {
        if (isScala(applicationClass)) {
            return;
        }

        CtClass ctClass = makeClass(applicationClass);

        if (!ctClass.subtypeOf(classPool.get(ControllersEnhancer.ControllerSupport.class.getName()))) {
            return ;
        }


        boolean needsContinuations = shouldEnhance( ctClass );

        if (!needsContinuations) {
            return;
        }


        // To be able to runtime detect if a class is enhanced for Continuations,
        // we add the interface EnhancedForContinuations to the class
        CtClass enhancedForContinuationsInterface;
        try {
            try (InputStream in = getClass().getClassLoader().getResourceAsStream("play/classloading/enhancers/EnhancedForContinuations.class")) {
                enhancedForContinuationsInterface = classPool.makeClass(in);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        ctClass.addInterface( enhancedForContinuationsInterface );

        // Apply continuations
        applicationClass.enhancedByteCode = new AsmClassTransformer().transform( ctClass.toBytecode());

        ctClass.defrost();
        enhancedForContinuationsInterface.defrost();
    }

    private boolean shouldEnhance(CtClass ctClass) throws Exception {

        if (ctClass == null || ctClass.getPackageName().startsWith("play.")) {
            // If we have not found any await-usage yet, we return false..
            return false;
        }

        var editor = new ExprEditor() {
            boolean needsContinuations;
            @Override
            public void edit(MethodCall methodCall) throws CannotCompileException {
                try {
                    if (continuationMethods.contains(methodCall.getMethod().getLongName())) {
                        this.needsContinuations = true;
                    }
                } catch (Exception e) {
                }
            }
        };
        for (CtMethod m : ctClass.getDeclaredMethods()) {
            m.instrument(editor);

            if (editor.needsContinuations) {
                break;
            }
        }

        return editor.needsContinuations || shouldEnhance(ctClass.getSuperclass());
    }


}
