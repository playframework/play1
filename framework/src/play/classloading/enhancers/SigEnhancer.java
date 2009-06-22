package play.classloading.enhancers;

import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.LocalVariableAttribute;
import javassist.bytecode.annotation.Annotation;
import play.classloading.ApplicationClasses.ApplicationClass;

/**
 * Track names of local variables ...
 */
public class SigEnhancer extends Enhancer {

    @Override
    public void enhanceThisClass(ApplicationClass applicationClass) throws Exception {

        CtClass ctClass = makeClass(applicationClass);
        StringBuilder sigChecksum = new StringBuilder();

        sigChecksum.append("Class->" + ctClass.getName() + ":");
        for (Annotation annotation : getAnnotations(ctClass).getAnnotations()) {
            sigChecksum.append(annotation + ",");
        }

        for (CtField field : ctClass.getDeclaredFields()) {
            sigChecksum.append(" Field->" + ctClass.getName() + " " + field.getSignature() + ":");
            sigChecksum.append(field.getSignature());
            for (Annotation annotation : getAnnotations(ctClass).getAnnotations()) {
                sigChecksum.append(annotation + ",");
            }
        }

        for (CtMethod method : ctClass.getDeclaredMethods()) {
            sigChecksum.append(" Method->" + method.getName() + method.getSignature() + ":");
            for (Annotation annotation : getAnnotations(method).getAnnotations()) {
                sigChecksum.append(annotation + " ");
            }
            // Signatures names
            CodeAttribute codeAttribute = (CodeAttribute) method.getMethodInfo().getAttribute("Code");
            if (codeAttribute == null || javassist.Modifier.isAbstract(method.getModifiers())) {
                continue;
            }
            LocalVariableAttribute localVariableAttribute = (LocalVariableAttribute) codeAttribute.getAttribute("LocalVariableTable");
            if (localVariableAttribute != null) {
                for (int i = 0; i < localVariableAttribute.tableLength(); i++) {
                    sigChecksum.append(localVariableAttribute.variableName(i) + ",");
                }
            }
        }

        applicationClass.sigChecksum = sigChecksum.toString().hashCode();
    }
}
