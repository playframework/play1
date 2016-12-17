package play.classloading.enhancers;

import javassist.*;
import javassist.bytecode.Bytecode;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.CodeIterator;
import javassist.bytecode.LocalVariableAttribute;
import play.Logger;
import play.classloading.ApplicationClasses.ApplicationClass;
import play.exceptions.UnexpectedException;
import play.libs.F.T2;

import java.lang.reflect.Method;
import java.util.*;

/**
 * Track names of local variables + generate signature fields for Java 7 support
 */
public class LocalvariablesNamesEnhancerJava7 extends LocalvariablesNamesEnhancer {

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
            if (codeAttribute == null || Modifier.isAbstract(method.getModifiers())) {
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
            
            String sigField = "$" + method.getName() + computeMethodHash(method.getParameterTypes());
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

    public static String[] parameterNames(Method method) {
        try {
            return (String[]) method.getDeclaringClass().getDeclaredField("$" + method.getName() + computeMethodHash(method.getParameterTypes())).get(null);
        }
        catch (Exception e) {
            throw new UnexpectedException("Cannot read parameter names for " + method, e);
        }
    }
}
