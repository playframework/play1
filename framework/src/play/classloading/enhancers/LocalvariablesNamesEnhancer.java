package play.classloading.enhancers;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.Modifier;
import javassist.bytecode.Bytecode;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.CodeIterator;
import javassist.bytecode.LocalVariableAttribute;
import javassist.compiler.Javac;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;
import play.Play;
import play.classloading.ApplicationClasses.ApplicationClass;
import play.exceptions.UnexpectedException;
import play.libs.Java;

/**
 * Track names of local variables ...
 */
public class LocalvariablesNamesEnhancer extends Enhancer {

    @Override
    public void enhanceThisClass(ApplicationClass applicationClass) throws Exception {
        CtClass ctClass = makeClass(applicationClass);
        if (!ctClass.subtypeOf(classPool.get("play.mvc.Controller"))) {
            return;
        }

        for (CtMethod method : ctClass.getDeclaredMethods()) {

            // Signatures names
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

            // enterMethod/endMethod
            // Supprimé car la tracage n'est plus fait en dehors des controllers ...
            // On va voir ...
            
            /*method.instrument(new ExprEditor() {

                @Override
                public void edit(MethodCall m) throws CannotCompileException {
                    try {
                        if (Play.classes.getApplicationClass(m.getMethod().getDeclaringClass().getName()) != null) {
                            m.replace(
                                    "play.classloading.enhancers.LocalvariablesNamesEnhancer.LocalVariablesNamesTracer.enterMethod();" +
                                    "try{" +
                                    "$_ = $proceed($$);" +
                                    "play.classloading.enhancers.LocalvariablesNamesEnhancer.LocalVariablesNamesTracer.exitMethod();" +
                                    "} catch(Throwable t) {" +
                                    "play.classloading.enhancers.LocalvariablesNamesEnhancer.LocalVariablesNamesTracer.exitMethod(); " +
                                    "throw t;" +
                                    "}");
                        }
                    } catch (Exception e) {
                        throw new UnexpectedException("Unexpected error in LocalvariablesEnhancer while compiling the enter/exit block", e);
                    }
                }
            });*/

            // Bon.
            // Alors là il s'agit aprés chaque instruction de creation d'une variable locale
            // d'insérer un appel à play.utils.LocalVariables.addVariable('var', var)
            // et sans tout péter ...
            for (int i = 0; i < localVariableAttribute.tableLength(); i++) {

                // le nom de la variable locale
                String name = localVariableAttribute.getConstPool().getUtf8Info(localVariableAttribute.nameIndex(i));
                if (name.equals("this")) {
                    continue;
                }
                // l'instruction a laquelle cette variable locale est créée
                Integer pc = localVariableAttribute.startPc(i);

                // on bouge a l'instrcution suivante (insertionPc)
                CodeIterator iterator = codeAttribute.iterator();
                iterator.move(pc);
                Integer insertionPc = iterator.next();

                // compile le bout de code
                Javac jv = new Javac(ctClass);
                jv.recordLocalVariables(codeAttribute, insertionPc);
                jv.recordParams(method.getParameterTypes(), Modifier.isStatic(method.getModifiers()));
                jv.setMaxLocals(codeAttribute.getMaxLocals());
                jv.compileStmnt("play.classloading.enhancers.LocalvariablesNamesEnhancer.LocalVariablesNamesTracer.addVariable(\"" + name + "\", " + name + ");");

                Bytecode b = jv.getBytecode();
                int locals = b.getMaxLocals();
                int stack = b.getMaxStack();
                codeAttribute.setMaxLocals(locals);

                if (stack > codeAttribute.getMaxStack()) {
                    codeAttribute.setMaxStack(stack);
                }
                iterator.insert(insertionPc, b.get());
                iterator.insert(b.getExceptionTable(), insertionPc);

                // Ensuite il faut tracer également chaque reaffectation de la variable.
                CodeIterator codeIterator = codeAttribute.iterator();
                // Bon chaque instruction de cette méthode
                while (codeIterator.hasNext()) {
                    int index = codeIterator.next();
                    int op = codeIterator.byteAt(index);
                    int varNumber = -1;
                    // La variable change
                    if(storeByCode.containsKey(op)) {
                    	varNumber = storeByCode.get(op);
                    	if(varNumber == -2) {
                    		varNumber = codeIterator.byteAt(index+1);
                    	}
                    }
                    
                    // Si c'est un store de la variable en cours d'examination
                    // et que c'est dans la frame d'utilisation de cette variable on trace l'affectation.
                    // (en fait la frame commence à localVariableAttribute.startPc(i)-1 qui est la première affectation
                    //  mais aussi l'initialisation de la variable qui est deja tracé plus haut, donc on commence à localVariableAttribute.startPc(i))
                    if (varNumber == localVariableAttribute.index(i) && index >= localVariableAttribute.startPc(i) && index <= localVariableAttribute.startPc(i) + localVariableAttribute.codeLength(i)) {

                        jv.compileStmnt("play.classloading.enhancers.LocalvariablesNamesEnhancer.LocalVariablesNamesTracer.addVariable(\"" + name + "\", " + name + ");");

                        b = jv.getBytecode();
                        locals = b.getMaxLocals();
                        stack = b.getMaxStack();
                        codeAttribute.setMaxLocals(locals);

                        if (stack > codeAttribute.getMaxStack()) {
                            codeAttribute.setMaxStack(stack);
                        }
                        codeIterator.insert(b.get());

                    }
                }

            }

        }

        applicationClass.enhancedByteCode = ctClass.toBytecode();
        ctClass.defrost();

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

    public static class LocalVariablesNamesTracer {

        static ThreadLocal<Stack<Map<String, Object>>> localVariables = new ThreadLocal<Stack<Map<String, Object>>>();

        public static void clear() {
            if (localVariables.get() != null) {                
                localVariables.set(null);
            }
        }
        
        public static void enterMethod() {
            if (localVariables.get() == null) {
                localVariables.set(new Stack<Map<String, Object>>());
            }
            localVariables.get().push(new HashMap<String, Object>());            
        }

        public static void exitMethod() {
            if(localVariables.get().isEmpty()) {
                return;
            }
            localVariables.get().pop().clear();            
        }

        public static Map<String, Object> locals() {
            if (localVariables.get() != null && !localVariables.get().empty()) {
                return localVariables.get().peek();
            }
            return new HashMap<String, Object>();
        }

        public static void addVariable(String name, Object o) {
            locals().put(name, o);
        }

        public static void addVariable(String name, boolean b) {
            locals().put(name, b);
        }

        public static void addVariable(String name, char c) {
            locals().put(name, c);
        }

        public static void addVariable(String name, byte b) {
            locals().put(name, b);
        }

        public static void addVariable(String name, double d) {
            locals().put(name, d);
        }

        public static void addVariable(String name, float f) {
            locals().put(name, f);
        }

        public static void addVariable(String name, int i) {
            locals().put(name, i);
        }

        public static void addVariable(String name, long l) {
            locals().put(name, l);
        }

        public static void addVariable(String name, short s) {
            locals().put(name, s);
        }

        public static Map<String, Object> getLocalVariables() {
            return locals();
        }

        public static List<String> getAllLocalVariableNames(Object o) {
            List<String> allNames = new ArrayList<String>();
            for (String variable : getLocalVariables().keySet()) {
                if (getLocalVariables().get(variable) == o) {
                    allNames.add(variable);
                }
                if (o != null && o instanceof Number && o.equals(getLocalVariables().get(variable))) {
                    allNames.add(variable);
                }
            }
            return allNames;
        }

        public static Object getLocalVariable(String variable) {
            return getLocalVariables().get(variable);
        }
    }
    
    private static Map<Integer, Integer> storeByCode = new HashMap<Integer, Integer> ();
    
    static  {
    	storeByCode.put(CodeIterator.ASTORE_0, 0);
    	storeByCode.put(CodeIterator.ASTORE_1, 1);
    	storeByCode.put(CodeIterator.ASTORE_2, 2);
    	storeByCode.put(CodeIterator.ASTORE_3, 3);
    	storeByCode.put(CodeIterator.ASTORE, -2);
    	
    	storeByCode.put(CodeIterator.ISTORE_0, 0);
    	storeByCode.put(CodeIterator.ISTORE_1, 1);
    	storeByCode.put(CodeIterator.ISTORE_2, 2);
    	storeByCode.put(CodeIterator.ISTORE_3, 3);
    	storeByCode.put(CodeIterator.ISTORE, -2);
    	storeByCode.put(CodeIterator.IINC, -2);
    	
    	storeByCode.put(CodeIterator.LSTORE_0, 0);
    	storeByCode.put(CodeIterator.LSTORE_1, 1);
    	storeByCode.put(CodeIterator.LSTORE_2, 2);
    	storeByCode.put(CodeIterator.LSTORE_3, 3);
    	storeByCode.put(CodeIterator.LSTORE, -2);
    	
    	storeByCode.put(CodeIterator.FSTORE_0, 0);
    	storeByCode.put(CodeIterator.FSTORE_1, 1);
    	storeByCode.put(CodeIterator.FSTORE_2, 2);
    	storeByCode.put(CodeIterator.FSTORE_3, 3);
    	storeByCode.put(CodeIterator.FSTORE, -2);
    	
    	storeByCode.put(CodeIterator.DSTORE_0, 0);
    	storeByCode.put(CodeIterator.DSTORE_1, 1);
    	storeByCode.put(CodeIterator.DSTORE_2, 2);
    	storeByCode.put(CodeIterator.DSTORE_3, 3);
    	storeByCode.put(CodeIterator.DSTORE, -2);
    }
}
