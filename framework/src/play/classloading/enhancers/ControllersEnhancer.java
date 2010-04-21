package play.classloading.enhancers;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;
import javassist.Modifier;
import javassist.NotFoundException;
import javassist.bytecode.annotation.Annotation;
import javassist.expr.ExprEditor;
import javassist.expr.FieldAccess;
import javassist.expr.Handler;
import play.Logger;
import play.classloading.ApplicationClasses.ApplicationClass;
import play.exceptions.UnexpectedException;

/**
 * Enhance controllers classes. 
 */
public class ControllersEnhancer extends Enhancer {

    @Override
    public void enhanceThisClass(final ApplicationClass applicationClass) throws Exception {
        CtClass ctClass = makeClass(applicationClass);

        if (!ctClass.subtypeOf(classPool.get(ControllerSupport.class.getName()))) {
            return;
        }

        for (final CtMethod ctMethod : ctClass.getDeclaredMethods()) {

            // Threaded access		
            ctMethod.instrument(new ExprEditor() {

                @Override
                public void edit(FieldAccess fieldAccess) throws CannotCompileException {
                    try {
                        if (isThreadedFieldAccess(fieldAccess.getField())) {
                            if (fieldAccess.isReader()) {
                                fieldAccess.replace("$_ = ($r)play.utils.Java.invokeStatic($type, \"current\");");
                            }
                        }
                    } catch (Exception e) {
                        Logger.error(e, "Error in ControllersEnhancer. %s.%s has not been properly enhanced (fieldAccess %s).", applicationClass.name, ctMethod.getName(), fieldAccess);
                        throw new UnexpectedException(e);
                    }
                }
            });

            // Auto-redirect
            boolean isHandler = false;
            for (Annotation a : getAnnotations(ctMethod).getAnnotations()) {
                if (a.getTypeName().startsWith("play.mvc.")) {
                    isHandler = true;
                    break;
                }
                if (a.getTypeName().endsWith("$ByPass")) {
                    isHandler = true;
                    break;
                }
            }

            // Perhaps it is a scala-generated method ?
            if (ctMethod.getName().contains("$")) {
                isHandler = true;
            } else {
                if (ctClass.getName().endsWith("$") && ctMethod.getParameterTypes().length == 0) {
                    try {
                        ctClass.getField(ctMethod.getName());
                        isHandler = true;
                    } catch (NotFoundException e) {
                        // ok
                    }
                }
            }

            if (Modifier.isPublic(ctMethod.getModifiers()) && ((ctClass.getName().endsWith("$") && !ctMethod.getName().contains("$default$")) || (Modifier.isStatic(ctMethod.getModifiers()) && ctMethod.getReturnType().equals(CtClass.voidType))) && !isHandler) {
                try {
                    ctMethod.insertBefore(
                            "if(!play.classloading.enhancers.ControllersEnhancer.ControllerInstrumentation.isActionCallAllowed()) {"
                            + "play.mvc.Controller.redirect(\"" + ctClass.getName().replace("$", "") + "." + ctMethod.getName() + "\", $args);"
                            + (ctMethod.getReturnType().equals(CtClass.voidType) ? "return;}" : "return null;}")
                            + "play.classloading.enhancers.ControllersEnhancer.ControllerInstrumentation.stopActionCall();");
                } catch (Exception e) {
                    Logger.error(e, "Error in ControllersEnhancer. %s.%s has not been properly enhanced (autoredirect).", applicationClass.name, ctMethod.getName());
                    throw new UnexpectedException(e);
                }
            }

            // Enhance global catch to avoid potential unwanted catching of play.mvc.results.Result
            ctMethod.instrument(new ExprEditor() {

                @Override
                public void edit(Handler handler) throws CannotCompileException {
                    StringBuffer code = new StringBuffer();
                    try {
                        code.append("if($1 instanceof play.mvc.results.Result) throw $1;");
                        handler.insertBefore(code.toString());
                    } catch (NullPointerException e) {
                        // TODO: finally clause ?
                        // There are no $1 in finally statements in javassist
                    }
                }
            });

        }

        // Done.
        applicationClass.enhancedByteCode = ctClass.toBytecode();
        ctClass.defrost();

    }

    /**
     * Mark class that need controller enhancement
     */
    public static interface ControllerSupport {
    }

    /**
     * Check if a field must be translated to a 'thread safe field'
     */
    static boolean isThreadedFieldAccess(CtField field) {
        if (field.getDeclaringClass().getName().equals("play.mvc.Controller")) {
            return field.getName().equals("params")
                    || field.getName().equals("request")
                    || field.getName().equals("response")
                    || field.getName().equals("session")
                    || field.getName().equals("params")
                    || field.getName().equals("renderArgs")
                    || field.getName().equals("validation")
                    || field.getName().equals("flash");
        }
        return false;
    }

    /**
     * Runtime part needed by the instrumentation
     */
    public static class ControllerInstrumentation {

        public static boolean isActionCallAllowed() {
            return allow.get();
        }

        public static void initActionCall() {
            allow.set(true);
        }

        public static void stopActionCall() {
            allow.set(false);
        }
        static ThreadLocal<Boolean> allow = new ThreadLocal<Boolean>();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface ByPass {
    }
}
