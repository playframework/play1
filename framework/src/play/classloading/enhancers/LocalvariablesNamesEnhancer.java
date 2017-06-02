package play.classloading.enhancers;

import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;
import javassist.Modifier;
import javassist.NotFoundException;
import javassist.bytecode.Bytecode;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.CodeIterator;
import javassist.bytecode.LocalVariableAttribute;
import javassist.bytecode.Opcode;
import play.Logger;
import play.classloading.ApplicationClasses.ApplicationClass;
import play.libs.F.T2;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;

/**
 * Track names of local variables ...
 */
public class LocalvariablesNamesEnhancer extends Enhancer {

    @Override
    public void enhanceThisClass(ApplicationClass applicationClass) throws Exception {
        if (isAnon(applicationClass)) {
            return;
        }

        CtClass ctClass = makeClass(applicationClass);
        if (!ctClass.subtypeOf(classPool.get(LocalVariablesSupport.class.getName())) && !ctClass.getName().matches("^controllers\\..*\\$class$")) {
            return;
        }

        for (CtMethod method : ctClass.getDeclaredMethods()) {

            if (method.getName().contains("$")) {
                // Generated method, skip
                continue;
            }

            // Signatures names
            CodeAttribute codeAttribute = (CodeAttribute) method.getMethodInfo().getAttribute("Code");
            if (codeAttribute == null || javassist.Modifier.isAbstract(method.getModifiers())) {
                continue;
            }
            LocalVariableAttribute localVariableAttribute = (LocalVariableAttribute) codeAttribute.getAttribute("LocalVariableTable");
            List<T2<Integer,String>> parameterNames = new ArrayList<>();
            
            if (localVariableAttribute == null) {
                if(method.getParameterTypes().length > 0)
                    continue;
            } else {
                if(localVariableAttribute.tableLength() < method.getParameterTypes().length + (Modifier.isStatic(method.getModifiers()) ? 0 : 1)) {
                    Logger.warn("weird: skipping method %s %s as its number of local variables is incorrect (lv=%s || lv.length=%s || params.length=%s || (isStatic? %s)", method.getReturnType().getName(), method.getLongName(), localVariableAttribute, localVariableAttribute != null ? localVariableAttribute.tableLength() : -1, method.getParameterTypes().length, Modifier.isStatic(method.getModifiers()));
                }
                for(int i=0; i<localVariableAttribute.tableLength(); i++) {
                    if (!"__stackRecorder".equals(localVariableAttribute.variableName(i))) {
                        parameterNames.add(new T2<>(localVariableAttribute.startPc(i) + localVariableAttribute.index(i), localVariableAttribute.variableName(i)));
                    }
                }
                Collections.sort(parameterNames, new Comparator<T2<Integer,String>>() {
                    @Override
                    public int compare(T2<Integer, String> o1, T2<Integer, String> o2) {
                        return o1._1.compareTo(o2._1);
                    }

                });
            }
            List<String> names = new ArrayList<>();
            for (int i = 0; i < method.getParameterTypes().length + (Modifier.isStatic(method.getModifiers()) ? 0 : 1); i++) {
                if (localVariableAttribute == null) {
                    continue;
                }
                try {
                    String name = parameterNames.get(i)._2;
                    if (!"this".equals(name)) {
                        names.add(name);
                    }
                } catch (Exception e) {
                    Logger.warn(e, "While applying localvariables to %s.%s, param %s", ctClass.getName(), method.getName(), i);
                }
            }
            StringBuilder iv = new StringBuilder();
            if (names.isEmpty()) {
                iv.append("new String[0];");
            } else {
                iv.append("new String[] {");
                for (Iterator<String> i = names.iterator(); i.hasNext();) {
                    iv.append("\"");
                    String aliasedName = i.next();
                    if (aliasedName.contains("$")) {
                        aliasedName = aliasedName.substring(0, aliasedName.indexOf("$"));
                    }
                    iv.append(aliasedName);
                    iv.append("\"");
                    if (i.hasNext()) {
                        iv.append(",");
                    }
                }
                iv.append("};");
            }
            
            String sigField = "$" + method.getName() + LocalVariablesNamesTracer.computeMethodHash(method.getParameterTypes());
            try { // #1198
                ctClass.getDeclaredField(sigField);
            } catch (NotFoundException nfe) {
                CtField signature = CtField.make("public static String[] " + sigField + " = " + iv, ctClass);
                ctClass.addField(signature);
            }

            if (localVariableAttribute == null || isScala(applicationClass)) {
                continue;
            }

            // OK.
            // Here after each local variable creation instruction,
            // we insert a call to play.utils.LocalVariables.addVariable('var', var)
            // without breaking everything...
            for (int i = 0; i < localVariableAttribute.tableLength(); i++) {

                // name of the local variable
                String name = localVariableAttribute.getConstPool().getUtf8Info(localVariableAttribute.nameIndex(i));

                // Normalize the variable name
                // For several reasons, both variables name and name$1 will be aliased to name
                String aliasedName = name;
                if (aliasedName.contains("$")) {
                    aliasedName = aliasedName.substring(0, aliasedName.indexOf("$"));
                }


                if ("this".equals(name)) {
                    continue;
                }

                /* DEBUG
                IO.write(ctClass.toBytecode(), new File("/tmp/lv_"+applicationClass.name+".class"));
                ctClass.defrost();
                 */

                try {

                    // The instruction at which this local variable has been created
                    Integer pc = localVariableAttribute.startPc(i);

                    // Move to the next instruction (insertionPc)
                    CodeIterator codeIterator = codeAttribute.iterator();
                    codeIterator.move(pc);
                    pc = codeIterator.next();
                    
                    Bytecode b = makeBytecodeForLVStore(method, localVariableAttribute.signature(i), name, localVariableAttribute.index(i));
                    codeIterator.insert(pc, b.get());
                    codeAttribute.setMaxStack(codeAttribute.computeMaxStack());

                    // Bon chaque instruction de cette méthode
                    while (codeIterator.hasNext()) {
                        int index = codeIterator.next();
                        int op = codeIterator.byteAt(index);

                        // DEBUG
                        // printOp(op);

                        int varNumber = -1;
                        // The variable changes
                        if (storeByCode.containsKey(op)) {
                            varNumber = storeByCode.get(op);
                            if (varNumber == -2) {
                                varNumber = codeIterator.byteAt(index + 1);
                            }
                        }

                        // Si c'est un store de la variable en cours d'examination
                        // et que c'est dans la frame d'utilisation de cette variable on trace l'affectation.
                        // (en fait la frame commence à localVariableAttribute.startPc(i)-1 qui est la première affectation
                        //  mais aussi l'initialisation de la variable qui est deja tracé plus haut, donc on commence à localVariableAttribute.startPc(i))
                        if (varNumber == localVariableAttribute.index(i) && index < localVariableAttribute.startPc(i) + localVariableAttribute.codeLength(i)) {
                            b = makeBytecodeForLVStore(method, localVariableAttribute.signature(i), aliasedName, varNumber);
                            codeIterator.insertEx(b.get());
                            codeAttribute.setMaxStack(codeAttribute.computeMaxStack());
                        }
                    }
                } catch (Exception e) {
                    // Well probably a compiled optimizer (I hope so)
                    Logger.error(e, "error in local var enhancement");
                }

            }

            // init variable tracer
            method.insertBefore("play.classloading.enhancers.LocalvariablesNamesEnhancer.LocalVariablesNamesTracer.enter();");
            method.insertAfter("play.classloading.enhancers.LocalvariablesNamesEnhancer.LocalVariablesNamesTracer.exit();", true);

        }

        // Done.
        applicationClass.enhancedByteCode = ctClass.toBytecode();
        ctClass.defrost();

    }
    
    private static Bytecode makeBytecodeForLVStore(CtMethod method, String sig, String name, int slot) {
        Bytecode b = new Bytecode(method.getMethodInfo().getConstPool());
        b.addLdc(name);
        if("I".equals(sig) || "B".equals(sig) || "C".equals(sig) || "S".equals(sig) || "Z".equals(sig))
            b.addIload(slot);
        else if("F".equals(sig))
            b.addFload(slot);
        else if("J".equals(sig))
            b.addLload(slot);
        else if("D".equals(sig))
            b.addDload(slot);
        else
            b.addAload(slot);
        
        String localVarDescriptor = sig;
        if(!"B".equals(sig) && !"C".equals(sig) && !"D".equals(sig) && !"F".equals(sig) &&
           !"I".equals(sig) && !"J".equals(sig) && !"S".equals(sig) && !"Z".equals(sig))
            localVarDescriptor = "Ljava/lang/Object;";

        Logger.trace("for variable '%s' in slot=%s, sig was '%s' and is now '%s'", name, slot, sig, localVarDescriptor);

        b.addInvokestatic("play.classloading.enhancers.LocalvariablesNamesEnhancer$LocalVariablesNamesTracer", "addVariable", "(Ljava/lang/String;"+localVarDescriptor+")V");
        
        return b;
    }

    /**
     * Mark class that need local variables tracking
     */
    public interface LocalVariablesSupport {
    }

    /**
     * Runtime part.
     */
    public static class LocalVariablesNamesTracer {

        public static Integer computeMethodHash(CtClass[] parameters) {
            String[] names = new String[parameters.length];
            for (int i = 0; i < parameters.length; i++) {
                names[i] = parameters[i].getName();
            }
            return computeMethodHash(names);
        }

        public static Integer computeMethodHash(Class<?>[] parameters) {
            String[] names = new String[parameters.length];
            for (int i = 0; i < parameters.length; i++) {
                Class<?> param = parameters[i];
                names[i] = "";
                if (param.isArray()) {
                    int level = 1;
                    param = param.getComponentType();
                    // Array of array
                    while (param.isArray()) {
                        level++;
                        param = param.getComponentType();
                    }
                    names[i] = param.getName();
                    for (int j = 0; j < level; j++) {
                        names[i] += "[]";
                    }
                } else {
                    names[i] = param.getName();
                }
            }
            return computeMethodHash(names);
        }

        public static Integer computeMethodHash(String[] parameters) {
            StringBuilder buffer = new StringBuilder();
            for (String param : parameters) {
                buffer.append(param);
            }
            Integer hash = buffer.toString().hashCode();
            if (hash < 0) {
                return -hash;
            }
            return hash;
        }
        static final ThreadLocal<Stack<Map<String, Object>>> localVariables = new ThreadLocal<>();

        public static void checkEmpty() {
            if (localVariables.get() != null && !localVariables.get().isEmpty()) {
                Logger.error("LocalVariablesNamesTracer.checkEmpty, constraint violated (%s)", localVariables.get().size());
            }
        }

        public static void clear() {
            if (localVariables.get() != null) {
                localVariables.set(null);
            }
        }

        public static void enter() {
            if (localVariables.get() == null) {
                localVariables.set(new Stack<Map<String, Object>>());
            }
            localVariables.get().push(new HashMap<String, Object>());
        }

        public static void exit() {
            if (localVariables.get().isEmpty()) {
                return;
            }
            localVariables.get().pop().clear();
        }

        public static Map<String, Object> locals() {
            if (localVariables.get() != null && !localVariables.get().empty()) {
                return localVariables.get().peek();
            }
            return new HashMap<>();
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
            List<String> allNames = new ArrayList<>();
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

        public static Stack<Map<String, Object>> getLocalVariablesStateBeforeAwait() {
            Stack<Map<String, Object>> state = localVariables.get();
            // must clear the ThreadLocal to prevent destroying the state when exit() is called due to continuations-suspend
            localVariables.set(new Stack<Map<String, Object>>());
            return state;
        }

        public static void setLocalVariablesStateAfterAwait(Stack<Map<String, Object>> state) {
            if (state==null) {
                state = new Stack<>();
            }
            localVariables.set( state );
        }
    }

    private static final Map<Integer, Integer> storeByCode = new HashMap<>();

    /**
     * Useful instructions
     */
    static {
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

    /**
     * Debug utility. Display a byte code op as plain text.
     */
    public static void printOp(int op) {
        try {
            for (Field f : Opcode.class.getDeclaredFields()) {
                if (java.lang.reflect.Modifier.isStatic(f.getModifiers()) && java.lang.reflect.Modifier.isPublic(f.getModifiers()) && f.getInt(null) == op) {
                    System.out.println(op + " " + f.getName());
                }
            }
        } catch (Exception e) {
        }
    }
}
