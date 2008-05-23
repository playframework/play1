package play.classloading.enhancers;

import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;
import javassist.expr.ExprEditor;
import javassist.expr.FieldAccess;
import play.classloading.ApplicationClasses.ApplicationClass;

public class ControllersEnhancer extends Enhancer {

    @Override
    public void enhanceThisClass(ApplicationClass applicationClass) throws Exception {
        CtClass ctClass = makeClass(applicationClass);

        for (final CtMethod ctMethod : ctClass.getDeclaredMethods()) {

            // Threaded access		
            ctMethod.instrument(new ExprEditor() {
                @Override
                public void edit(FieldAccess fieldAccess) throws CannotCompileException {
                    try {
                        if (isThreadedFieldAccess(fieldAccess.getField())) {
                            if (fieldAccess.isReader()) {
                                fieldAccess.replace("$_ = ($r)play.libs.Java.invokeStatic($type, \"get\");");
                            }
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            });

        }
        
        applicationClass.javaByteCode = ctClass.toBytecode();
        ctClass.defrost();

    }
    
    static boolean isThreadedFieldAccess(CtField field) {
        if(field.getDeclaringClass().getName().equals("play.mvc.Controller")) {
            return field.getName().equals("params") 
                || field.getName().equals("response") 
                || field.getName().equals("session")
                || field.getName().equals("params")
                || field.getName().equals("flash");
	}
	return false;
    }	
    
}
