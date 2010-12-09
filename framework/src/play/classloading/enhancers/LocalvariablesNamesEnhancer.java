package play.classloading.enhancers;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtField;
import javassist.CtMethod;
import javassist.Modifier;
import javassist.bytecode.Bytecode;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.CodeIterator;
import javassist.bytecode.LocalVariableAttribute;
import javassist.bytecode.Opcode;
import javassist.compiler.Javac;
import javassist.compiler.NoFieldException;
import play.Logger;
import play.classloading.ApplicationClasses.ApplicationClass;
import play.exceptions.UnexpectedException;

/**
 * Track names of local variables ...
 */
public class LocalvariablesNamesEnhancer extends Enhancer {

    public static List<String> lookupParameterNames(Constructor constructor) {
        try {
            List<String> parameters = new ArrayList<String>();

            ClassPool classPool = newClassPool();
            CtClass ctClass = classPool.get(constructor.getDeclaringClass().getName());
            CtClass[] cc = new CtClass[constructor.getParameterTypes().length];
            for (int i = 0; i < constructor.getParameterTypes().length; i++) {
                cc[i] = classPool.get(constructor.getParameterTypes()[i].getName());
            }
            CtConstructor ctConstructor = ctClass.getDeclaredConstructor(cc);

            // Signatures names
            CodeAttribute codeAttribute = (CodeAttribute) ctConstructor.getMethodInfo().getAttribute("Code");
            if (codeAttribute != null) {
                LocalVariableAttribute localVariableAttribute = (LocalVariableAttribute) codeAttribute.getAttribute("LocalVariableTable");
                if (localVariableAttribute != null && localVariableAttribute.tableLength() >= ctConstructor.getParameterTypes().length) {
                    for (int i = 0; i < ctConstructor.getParameterTypes().length + 1; i++) {
                        String name = localVariableAttribute.getConstPool().getUtf8Info(localVariableAttribute.nameIndex(i));
                        if (!name.equals("this")) {
                            parameters.add(name);
                        }
                    }
                }
            }

            return parameters;
        } catch(Exception e) {
            throw new UnexpectedException("Cannot extract parameter names", e);
        }
    }

    //

    @Override
    public void enhanceThisClass(ApplicationClass applicationClass) throws Exception {

        CtClass ctClass = makeClass(applicationClass);
        if (!ctClass.subtypeOf(classPool.get(LocalVariablesSupport.class.getName())) && !ctClass.getName().matches("^controllers\\..*\\$class$")) {
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
                if (method.getParameterTypes().length > 0) {
                    continue;
                }
            }
            List<String> names = new ArrayList<String>();
            for (int i = 0; i < method.getParameterTypes().length + (Modifier.isStatic(method.getModifiers()) ? 0 : 1); i++) {
                if (localVariableAttribute == null) {
                    continue;
                }
                String name = localVariableAttribute.getConstPool().getUtf8Info(localVariableAttribute.nameIndex(i));
                if (!name.equals("this")) {
                    names.add(name);
                }
            }
            StringBuilder iv = new StringBuilder();
            if (names.size() == 0) {
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

            CtField signature = CtField.make("public static String[] $" + method.getName() + LocalVariablesNamesTracer.computeMethodHash(method.getParameterTypes()) + " = " + iv.toString(), ctClass);
            ctClass.addField(signature);

            // No variable name, skip...
            if (localVariableAttribute == null) {
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


                if (name.equals("this")) {
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
                    CodeIterator iterator = codeAttribute.iterator();
                    iterator.move(pc);
                    Integer insertionPc = iterator.next();

                    Javac jv = new Javac(ctClass);

                    // Compile the code snippet
                    jv.recordLocalVariables(codeAttribute, insertionPc);
                    jv.recordParams(method.getParameterTypes(), Modifier.isStatic(method.getModifiers()));
                    jv.setMaxLocals(codeAttribute.getMaxLocals());
                    jv.compileStmnt("play.classloading.enhancers.LocalvariablesNamesEnhancer.LocalVariablesNamesTracer.addVariable(\"" + aliasedName + "\", " + name + ");");

                    Bytecode b = jv.getBytecode();
                    int locals = b.getMaxLocals();
                    int stack = b.getMaxStack();
                    codeAttribute.setMaxLocals(locals);
                    if (stack > codeAttribute.getMaxStack()) {
                        codeAttribute.setMaxStack(stack);
                    }
                    iterator.insert(insertionPc, b.get());
                    iterator.insert(b.getExceptionTable(), insertionPc);


                    // Then we need to trace each affectation to the variable
                    CodeIterator codeIterator = codeAttribute.iterator();

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
                        if (varNumber == localVariableAttribute.index(i) && index >= localVariableAttribute.startPc(i) && index < localVariableAttribute.startPc(i) + localVariableAttribute.codeLength(i)) {

                            jv.compileStmnt("play.classloading.enhancers.LocalvariablesNamesEnhancer.LocalVariablesNamesTracer.addVariable(\"" + aliasedName + "\", " + name + ");");

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


                } catch (NoFieldException e) {
                    // Well probably a compiled optimizer (I hope so)
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

    /**
     * Mark class that need local variables tracking
     */
    public static interface LocalVariablesSupport {
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
            StringBuffer buffer = new StringBuffer();
            for (String param : parameters) {
                buffer.append(param);
            }
            Integer hash = buffer.toString().hashCode();
            if (hash < 0) {
                return -hash;
            }
            return hash;
        }
        static ThreadLocal<Stack<Map<String, Object>>> localVariables = new ThreadLocal<Stack<Map<String, Object>>>();

        public static void checkEmpty() {
            if (localVariables.get() != null && localVariables.get().size() != 0) {
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
    private static Map<Integer, Integer> storeByCode = new HashMap<Integer, Integer>();

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
