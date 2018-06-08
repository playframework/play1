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
import java.util.ArrayList;
import java.util.List;

public class ContinuationEnhancer extends Enhancer {

    static final List<String> continuationMethods = new ArrayList<>();

    static {
        continuationMethods.add("play.mvc.Controller.await(java.lang.String)");
        continuationMethods.add("play.mvc.Controller.await(int)");
        continuationMethods.add("play.mvc.Controller.await(java.util.concurrent.Future)");
        continuationMethods.add("play.mvc.WebSocketController.await(java.lang.String)");
        continuationMethods.add("play.mvc.WebSocketController.await(int)");
        continuationMethods.add("play.mvc.WebSocketController.await(java.util.concurrent.Future)");
    }

    public static boolean isEnhanced(String appClassName) {
        ApplicationClass appClass = Play.classes.getApplicationClass( appClassName);
        if ( appClass == null) {
            return false;
        }

        // All classes enhanced for Continuations are implementing the interface EnhancedForContinuations
        return EnhancedForContinuations.class.isAssignableFrom( appClass.javaClass );
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

        final boolean[] _needsContinuations = new boolean[]{false};

        for (CtMethod m : ctClass.getDeclaredMethods()) {
            m.instrument(new ExprEditor() {

                @Override
                public void edit(MethodCall m) throws CannotCompileException {
                    try {
                        if (continuationMethods.contains(m.getMethod().getLongName())) {
                            _needsContinuations[0] = true;
                        }
                    } catch (Exception e) {
                    }
                }
            });

            if (_needsContinuations[0]) {
                break;
            }
        }

        if (!_needsContinuations[0]) {
            // Check parent class
            _needsContinuations[0] = shouldEnhance( ctClass.getSuperclass());
        }

        return _needsContinuations[0];

    }


}
