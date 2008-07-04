package play.classloading.enhancers;

import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;
import javassist.Modifier;
import javassist.expr.ExprEditor;
import javassist.expr.FieldAccess;
import javassist.expr.Handler;
import play.classloading.ApplicationClasses.ApplicationClass;
import play.exceptions.UnexpectedException;

/**
 * Enhance controllers classes.
 */
public class ControllersEnhancer extends Enhancer {

    @Override
    public void enhanceThisClass(ApplicationClass applicationClass) throws Exception {
        CtClass ctClass = makeClass(applicationClass);
        
        if(!ctClass.subtypeOf(classPool.get("play.mvc.Controller"))) {
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
                                fieldAccess.replace("$_ = ($r)play.libs.Java.invokeStatic($type, \"current\");");
                            }
                        }
                    } catch (Exception e) {
                        throw new UnexpectedException(e);
                    }
                }
            });
            
            // Auto-redirect
            if (Modifier.isPublic(ctMethod.getModifiers()) && Modifier.isStatic(ctMethod.getModifiers())) {
                try {
                    ctMethod.insertBefore(
                               "if(!play.classloading.enhancers.ControllersEnhancer.ControllerInstrumentation.isActionCallAllowed()) {"+
                                    "redirect(\""+ctClass.getName()+"."+ctMethod.getName()+"\", $args);"+
                               "}"+
                               "play.classloading.enhancers.ControllersEnhancer.ControllerInstrumentation.stopActionCall();"
                    );
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            
            // Enchance global catch to avoid potential unwanted catching of play.mvc.results.Result
            ctMethod.instrument(new ExprEditor() {
                @Override
                public void edit(Handler handler) throws CannotCompileException {
                    StringBuffer code = new StringBuffer();
                    code.append("if($1 instanceof play.mvc.results.Result) throw $1;");
                    handler.insertBefore(code.toString());
                }
            });

        }
        
        applicationClass.enhancedByteCode = ctClass.toBytecode();
        ctClass.defrost();

    }
    
    static boolean isThreadedFieldAccess(CtField field) {
        if(field.getDeclaringClass().getName().equals("play.mvc.Controller")) {
            return field.getName().equals("params") 
                || field.getName().equals("request") 
                || field.getName().equals("response") 
                || field.getName().equals("session")
                || field.getName().equals("params")
                || field.getName().equals("renderArgs")
                || field.getName().equals("flash");
	}
	return false;
    }	
    
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
    
}
