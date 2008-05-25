package play.classloading.enhancers;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.LocalVariableAttribute;
import play.classloading.ApplicationClasses.ApplicationClass;
import play.libs.Java;

public class LocalvariablesNamesEnhancer extends Enhancer {

    @Override
    public void enhanceThisClass(ApplicationClass applicationClass) throws Exception {
        CtClass ctClass = makeClass(applicationClass);

        for (CtMethod method : ctClass.getDeclaredMethods()) {
            CodeAttribute codeAttribute = (CodeAttribute) method.getMethodInfo().getAttribute("Code");
            if (codeAttribute == null || javassist.Modifier.isAbstract(method.getModifiers())) {
                continue;
            }
            LocalVariableAttribute localVariableAttribute = (LocalVariableAttribute) codeAttribute.getAttribute("LocalVariableTable");
            if (localVariableAttribute == null || localVariableAttribute.tableLength() < method.getParameterTypes().length) {
                continue;
            }
            List<String> names = new ArrayList<String>();
            for (int i = 0; i < method.getParameterTypes().length; i++) {
                String name = localVariableAttribute.getConstPool().getUtf8Info(localVariableAttribute.nameIndex(i));
                if (!name.equals("this")) {
                    names.add(name);
                }
            }
            SignaturesNamesRepository.signatures.put(SignaturesNamesRepository.signature(method), names.toArray(new String[names.size()]));
        }
    
    }

    public static class SignaturesNamesRepository {

        static Map<String, String[]> signatures = new HashMap<String, String[]>();

        public static String[] get(Method method) {           
            return signatures.get(Java.rawMethodSignature(method));
        }        
        
        static String signature(CtMethod ctMethod) {
            StringBuilder sig = new StringBuilder();
            sig.append(ctMethod.getDeclaringClass().getName());
            sig.append(".");
            sig.append(ctMethod.getName());
            sig.append(ctMethod.getSignature());
            return sig.toString();
        }
    }
}
