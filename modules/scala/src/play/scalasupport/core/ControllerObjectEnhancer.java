package play.scalasupport.core;

import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;
import javassist.Modifier;
import javassist.NotFoundException;
import javassist.bytecode.annotation.Annotation;
import play.Logger;
import play.Play;
import play.classloading.ApplicationClasses.ApplicationClass;
import play.classloading.enhancers.ControllersEnhancer.ControllerSupport;
import play.classloading.enhancers.Enhancer;
import play.classloading.enhancers.LocalvariablesNamesEnhancer.LocalVariablesNamesTracer;
import play.exceptions.UnexpectedException;

public class ControllerObjectEnhancer extends Enhancer {

    @Override
    public void enhanceThisClass(ApplicationClass applicationClass) throws Exception {
        CtClass controllerCtClass = makeClass(applicationClass); 
        
        if(!controllerCtClass.subtypeOf(classPool.makeClass("play.mvc.ControllerObject"))) {
            return;
        }
        
        assert applicationClass.name.endsWith("$"); 
        
        String controllerProxyName = applicationClass.name.substring(0, applicationClass.name.length()-1);
        ApplicationClass controllerProxyApplicationClass = Play.classes.getApplicationClass(controllerProxyName);
        CtClass controllerProxyCtClass = makeClass(controllerProxyApplicationClass);
   
        // Add markers
        controllerProxyCtClass.addInterface(classPool.get(ControllerSupport.class.getName()));
        
        // Copy actions signatures
        for (CtMethod method : controllerProxyCtClass.getDeclaredMethods()) {

            if (!Modifier.isStatic(method.getModifiers())) {
                continue;
            }
            
            String key = "$" + method.getName() + LocalVariablesNamesTracer.computeMethodHash(method.getParameterTypes());
            
            try {
                controllerCtClass.getField(key); // Just a try
                CtField signature = CtField.make("public static String[] " + key + " = " + applicationClass.name+"." +key + ";", controllerProxyCtClass);
                controllerProxyCtClass.addField(signature);  
            } catch(NotFoundException e) {
                //
            }

        }
        
        // Auto-redirect ??
        for (CtMethod method : controllerCtClass.getDeclaredMethods()) {
            boolean isHandler = false;
            for(Annotation a : getAnnotations(method).getAnnotations()) {
                if(a.getTypeName().startsWith("play.mvc.")) {
                    isHandler = true;
                    break;
                }
            }
            if (Modifier.isPublic(method.getModifiers()) && !isHandler) {
                try {
                    method.insertBefore(
                               "if(!play.classloading.enhancers.ControllersEnhancer.ControllerInstrumentation.isActionCallAllowed()) {"+
                                    "play.scalasupport.wrappers.ControllerWrapper.redirect(\""+controllerProxyName+"."+method.getName()+"\", $args);"+
                               "}"+
                               "play.classloading.enhancers.ControllersEnhancer.ControllerInstrumentation.stopActionCall();"
                    );
                } catch (Exception e) {
                    Logger.error(e, "Error in ControllersEnhancer. %s.%s has not been properly enhanced (autoredirect).", applicationClass.name, method.getName());
                    throw new UnexpectedException(e);
                }
            }
        }
        
        // Hop
        controllerProxyApplicationClass.javaByteCode = controllerProxyApplicationClass.enhancedByteCode = controllerProxyCtClass.toBytecode();
        controllerProxyCtClass.defrost();
        
        // Et re hop
        applicationClass.enhancedByteCode = controllerCtClass.toBytecode();
        controllerCtClass.defrost();
        
    }

}
