package play.modules.tracer.plugin;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;
import javassist.Modifier;
import javassist.NotFoundException;
import javassist.bytecode.Bytecode;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.CodeIterator;
import javassist.bytecode.LineNumberAttribute;
import javassist.bytecode.LocalVariableAttribute;
import javassist.bytecode.Opcode;
import javassist.compiler.Javac;
import javassist.expr.ExprEditor;
import javassist.expr.FieldAccess;
import javassist.expr.MethodCall;

import play.Logger;
import play.classloading.ApplicationClasses.ApplicationClass;
import play.classloading.enhancers.Enhancer;
import play.classloading.enhancers.LocalvariablesNamesEnhancer.LocalVariablesSupport;
import play.classloading.enhancers.PropertiesEnhancer.PlayPropertyAccessor;
import play.modules.tracer.Variable;

/**
 * Track names of local variables ...
 */
public class TracerEnhancer extends Enhancer {
	public static @interface TracerEnhanced { }
	
	private static class Lines {
		Map<Integer, Set<Integer>> readVariablesByLine = new HashMap<Integer, Set<Integer>>();
		Map<Integer, Set<Integer>> writtenVariablesByLine = new HashMap<Integer, Set<Integer>>();
		
		void registerWrite(int line, Variable v) {
			Set<Integer> writtenVariables = writtenVariablesByLine.get(line);
			if(writtenVariables == null) {
				writtenVariables = new HashSet<Integer>();
				writtenVariablesByLine.put(line, writtenVariables);
			}
			writtenVariables.add(v.index);
		}
		
		void registerRead(int line, Variable v) {
			Set<Integer> readVariables = readVariablesByLine.get(line);
			if(readVariables == null) {
				readVariables = new HashSet<Integer>();
				readVariablesByLine.put(line, readVariables);
			}
			readVariables.add(v.index);
		}
	}
	
    @Override
    public void enhanceThisClass(ApplicationClass applicationClass) throws Exception {
        final CtClass ctClass = makeClass(applicationClass);
        if (!ctClass.subtypeOf(classPool.get(LocalVariablesSupport.class.getName())) && !ctClass.getName().startsWith("models.")) {
        	Logger.debug("TRACERENHANCER refused " + applicationClass.name);
        	return;
        }
        Logger.debug("*********TRACER enhance '" + ctClass.getName() + "'************");
        
        createAnnotation(getAnnotations(ctClass), TracerEnhanced.class);

        for (final CtMethod method : ctClass.getDeclaredMethods()) {
        	Logger.debug("\nTracerEnhancer.enhanceThisClass() --- enhancing " + method.getLongName() + " ---");
        	if(isPlayPropertyAccessor(method)) {
        		continue;
        	}
        	
            // Signatures names
            CodeAttribute codeAttribute = (CodeAttribute) method.getMethodInfo().getAttribute("Code");
            if (codeAttribute == null || javassist.Modifier.isAbstract(method.getModifiers())) {
                continue;
            }
            LocalVariableAttribute localVariableAttribute = (LocalVariableAttribute) codeAttribute.getAttribute("LocalVariableTable");
            if (localVariableAttribute == null || localVariableAttribute.tableLength() < method.getParameterTypes().length) {
                if(method.getParameterTypes().length > 0) {
                    continue;
                }
            }
            
            LineNumberAttribute lineNumberAttribute = (LineNumberAttribute) codeAttribute.getAttribute("LineNumberTable");
            if(lineNumberAttribute == null || lineNumberAttribute.tableLength() < 1) {
            	Logger.debug("TRACER ENHANCER : method '" + method.getLongName() + "' has no lines. Skipping.");
            	continue;
            }
            
            // No variables name, skip ...
            if(localVariableAttribute == null) {
                continue;
            }
            
            
            final List<Variable> accessibleVariables = new ArrayList<Variable>();
            final Lines lines = new Lines();
            
            method.addLocalVariable("$$variables", ctClass.getClassPool().get("play.modules.tracer.Variable[]"));
            method.addLocalVariable("$$lineReadsMap", method.getDeclaringClass().getClassPool().get("java.util.Map"));
            method.addLocalVariable("$$lineWritesMap", method.getDeclaringClass().getClassPool().get("java.util.Map"));
            
            for(CtField field : getFields(ctClass, true)) {
            	if(field.getName().startsWith("$")) continue;
            	Variable v = new Variable();
            	v.startLine = -1;
            	v.endLine = Integer.MAX_VALUE;
            	v.name = field.getName();
            	v.isParam = false;
            	v.isLocal = false;
            	v.modifier = field.getModifiers();
            	v.classFQDN = field.getType().getName();
            	v.declaringClassFQDN = field.getDeclaringClass().getName();
            	accessibleVariables.add(v);
            	v.index = accessibleVariables.size() - 1;
            }
            
            for(int i = 0; i < localVariableAttribute.tableLength(); i++) {
            	Variable v = new Variable(localVariableAttribute, i);
            	v.name = localVariableAttribute.getConstPool().getUtf8Info(localVariableAttribute.nameIndex(i));
            	v.isParam = i < method.getParameterTypes().length + (Modifier.isStatic(method.getModifiers()) ? 0 : 1);
            	if("this".equals(v.name) || v.name.startsWith("$"))
            		continue;
            	v.localVariableAttribute = localVariableAttribute;
            	v.endLine = lineNumberAttribute.toLineNumber(v.getEndPC());
            	v.isLocal = true;
            	v.modifier = 0;
            	v.classFQDN = parseVariableSignature(localVariableAttribute.signature(i));
            	v.declaringClassFQDN = ctClass.getName();
            	accessibleVariables.add(v);
            	v.index = accessibleVariables.size() - 1;
            	
            	Logger.debug("TracerEnhancer.enhanceThisClass() added local var '"+v.name+"' from "+v.startLine+" to "+v.endLine);

                int line = toLine(lineNumberAttribute, v.getStartPC());
                v.startLine = line;
                lines.registerWrite(line, v);

                CodeIterator codeIterator = codeAttribute.iterator();
                codeIterator.move(v.getStartPC());
                if(codeIterator.hasNext())
                	codeIterator.next();

                while (codeIterator.hasNext()) {
                    int index = codeIterator.next();
                    int op = codeIterator.byteAt(index);
                    
                    int varNumber = -1;
                    boolean loadOp = true;

                    if (storeByCode.containsKey(op)) {
                        varNumber = storeByCode.get(op);
                        loadOp = false; // write op
                        if (varNumber == -2) {
                            varNumber = codeIterator.byteAt(index + 1);
                        }
                    }
                    
                    if(loadByCode.containsKey(op)) {
                    	varNumber = loadByCode.get(op);
                    	if (varNumber == -2) {
                            varNumber = codeIterator.byteAt(index + 1);
                        }
                    }

                    if (varNumber == localVariableAttribute.index(v.localVariableTableIndex) && index >= v.getStartPC() && index <= v.getEndPC()) {
                    	line = toLine(lineNumberAttribute, index);
                    	if(loadOp) {
                    		lines.registerRead(line, v);
                    	} else {
                    		lines.registerWrite(line, v);
                    	}

                    }
                }
            }
            
            method.instrument(new ExprEditor() {
            	@Override
            	public void edit(FieldAccess f) throws CannotCompileException {
            		String name = f.getFieldName();
            		try {
	            		if(name.startsWith("$") || !f.getField().getDeclaringClass().equals(ctClass))
	            			return;
	            		int line = f.getLineNumber();
	            		
	            		Variable v = null;
	            		for(Variable var : accessibleVariables) {
	            			if(!var.isLocal && var.modifier == f.getField().getModifiers() && var.name.equals(name))
	            				v = var;
	            		}
	            		
	            		if(f.isWriter()) {
	            			lines.registerWrite(line, v);
	            		}
	            		if(f.isReader()) {
	            			lines.registerRead(line, v);
	            		}
            		} catch (Exception e) {
            			StringBuffer sb = new StringBuffer();
            			for(Variable v : accessibleVariables)
            				sb.append(v.name).append(" ");
            			throw new RuntimeException(name + " was not found in\n" + sb, e);
            		}
            	}
            });
            
            
            StringBuffer insertBefore = new StringBuffer();
            insertBefore.append("$$variables = new play.modules.tracer.Variable[").append(accessibleVariables.size()).append("];");
            insertBefore.append("$$lineReadsMap = new java.util.HashMap();");
            insertBefore.append("$$lineWritesMap = new java.util.HashMap();");
            for(int i = 0; i < accessibleVariables.size(); i++) {
            	Variable v = accessibleVariables.get(i);
            	
            	insertBefore.append("$$variables[").append(i).append("] = new play.modules.tracer.Variable();");
            	insertBefore.append("$$variables[").append(i).append("].name = \"").append(v.name).append("\";");
            	insertBefore.append("$$variables[").append(i).append("].startLine = ").append(v.startLine).append(";");
            	insertBefore.append("$$variables[").append(i).append("].endLine = ").append(v.endLine).append(";");
            	insertBefore.append("$$variables[").append(i).append("].isLocal = ").append(v.isLocal).append(";");
            	insertBefore.append("$$variables[").append(i).append("].isParam = ").append(v.isParam).append(";");
            	insertBefore.append("$$variables[").append(i).append("].modifier = ").append(v.modifier).append(";");
            	insertBefore.append("$$variables[").append(i).append("].klass = ").append(v.classFQDN).append(".class").append(";");
            	insertBefore.append("$$variables[").append(i).append("].declaringClass = ").append(v.declaringClassFQDN).append(".class").append(";");
            	
            	if(!v.isLocal && (!Modifier.isStatic(method.getModifiers()) || Modifier.isStatic(v.modifier))) {
            		insertBefore.append("$$variables[").append(i).append("].mutate(");
            		if(Modifier.isStatic(v.modifier)) {
            			Logger.debug("TracerEnhancer.enhanceThisClass(): " + ctClass.getName() + "." + v.name);
            			insertBefore.append(v.declaringClassFQDN).append("#").append(v.name);
            		}
            		else {
            			insertBefore.append(v.name);
            		}
            		insertBefore.append(");");
            	}
            }
            
            
            
            
            StringBuffer insertAfter = new StringBuffer();
            
            /*
             * Line enhancing. Before a line's code, insert a call to enterLine.
             */
            if(lineNumberAttribute.tableLength() > 0) {
            	int previousLine = -1;
            	for(int i = 0; i <= lineNumberAttribute.tableLength(); i++) {
            		StringBuffer insert = new StringBuffer("{");
            		if(previousLine > -1) {
            			Set<Integer> writtenVars = lines.writtenVariablesByLine.get(previousLine);
            			Set<Integer> readVars = lines.readVariablesByLine.get(previousLine);
            			if(readVars != null) {
            				readVars = new HashSet<Integer>(lines.readVariablesByLine.get(previousLine));
            				if(writtenVars != null)
            					readVars.removeAll(writtenVars);
            			}
            			
            			insertBefore.append("$$lineReadsMap.put(new java.lang.Integer(").append(previousLine).append("), new play.modules.tracer.Variable[").append(readVars != null ? readVars.size() : 0).append("]);");
            			if(readVars != null) {
            				int j = 0;
            				for(Integer index : readVars) {
            					insert.append("((play.modules.tracer.Variable[])$$lineReadsMap.get(new java.lang.Integer(").append(previousLine).append(")))[").append(j).append("] = $$variables[").append(index).append("];");
            					j++;
            				}
            			}
            			insertBefore.append("$$lineWritesMap.put(new java.lang.Integer(").append(previousLine).append("), new play.modules.tracer.Variable[").append(writtenVars != null ? writtenVars.size() : 0).append("]);");
            			if(writtenVars != null) {
            				int j = 0;
            				for(Integer index : writtenVars) {
            					insert.append("((play.modules.tracer.Variable[])$$lineWritesMap.get(new java.lang.Integer(").append(previousLine).append(")))[").append(j).append("] = $$variables[").append(index).append("];");
            					j++;
            				}
            			}
            			insert.append("play.modules.tracer.plugin.Tracer.endLine((play.modules.tracer.Variable[])$$lineReadsMap.get(new java.lang.Integer(" + previousLine +")), (play.modules.tracer.Variable[])$$lineWritesMap.get(new java.lang.Integer(" + previousLine +")));");
            		}
            		
            		if(i < lineNumberAttribute.tableLength()) {
            			int line = lineNumberAttribute.lineNumber(i);
	            		insert.append("play.modules.tracer.plugin.Tracer.startLine(").append(line).append(");}");
	            		previousLine = line;
	            		
	            		CodeIterator iterator = codeAttribute.iterator();
	                	int pc = lineNumberAttribute.startPc(i);
	                    iterator.move(pc);
	                    Javac jv = new Javac(ctClass);
	                    jv.recordLocalVariables(codeAttribute, pc);
	    				jv.recordParams(method.getParameterTypes(), Modifier
	    						.isStatic(method.getModifiers()));
	    				jv.setMaxLocals(codeAttribute.getMaxLocals());
	    				jv.compileStmnt(insert.toString());
	    				Bytecode b = jv.getBytecode();
	    				int locals = b.getMaxLocals();
	    				int stack = b.getMaxStack();
	    				codeAttribute.setMaxLocals(locals);

	    				if (stack > codeAttribute.getMaxStack()) {
	    					codeAttribute.setMaxStack(stack);
	    				}
	    				iterator.insert(pc, b.get());
	    				iterator.insert(b.getExceptionTable(), pc);
            		} else {
            			insertAfter.append(insert.append("}"));
            		}
            	}
            }
            
            insertBefore.append("play.modules.tracer.plugin.Tracer.enterMethod(" + ctClass.getName() + ".class, \"" + method.getName() + "\", " + (Modifier.isStatic(method.getModifiers()) ? "null" : "this") + ", $$variables, $$lineReadsMap, $$lineWritesMap);");
            
            
            method.instrument(new ExprEditor() {
            	
            	@Override
            	public void edit(FieldAccess f) throws CannotCompileException {
            		String name = f.getFieldName();
            		try {
	            		if(name.startsWith("$") || !f.getField().getDeclaringClass().equals(ctClass))
	            			return;
	            		
	            		Variable variable = null;
	            		for(Variable v : accessibleVariables) {
	            			if(v.modifier == f.getField().getModifiers() && v.name.equals(name))
	            				variable = v;
	            		}
	            		
	            		if(f.isWriter()) {
	            			String callName = variable.name;
	            			if(!Modifier.isStatic(variable.modifier))
	            				callName = "this." + variable.name;
	            			String statement = "{ "+callName+" = $1; $$variables["+variable.index+"].mutate($1); }";
	            			f.replace(statement);
	            		}
            		} catch (Exception e) {
            			StringBuffer sb = new StringBuffer();
            			for(Variable v : accessibleVariables)
            				sb.append(v.name).append(" ");
            			throw new RuntimeException(name + " was not found in\n" + sb, e);
            		}
            	}
            	
            	@Override
            	public void edit(MethodCall m) throws CannotCompileException {
            		try {
	            		Logger.trace("method call %s", m.getClassName() + "."+m.getMethodName());
	            		if(m.getMethod().getDeclaringClass().equals(ctClass.getClassPool().getCtClass("play.mvc.Controller")) && m.getMethodName().startsWith("render")) {
	            			Logger.trace("method call %s.%s in %s is a controller render (at "+m.getLineNumber()+")", m.getClassName(), m.getMethodName(), method.getLongName());
	            			m.replace("play.modules.tracer.plugin.Tracer.endLine(null, null);play.modules.tracer.plugin.Tracer.exitMethod();$proceed($$);");
	            		}
            		} catch (Exception e) {
            			Logger.error("[TracerEnhancer] methodcall replacement error : ", e);
            		}
            	}
            });
            
            
            /*
             * Local variables enhancing.
             */
            for(Variable v : accessibleVariables) {
            	if(!v.isLocal)
            		continue;
                Integer pc = v.getStartPC();
                CodeIterator iterator = codeAttribute.iterator();
                iterator.move(pc);
                Integer insertionPc = iterator.next();
                
                Javac jv = new Javac(ctClass);
                int line = toLine(lineNumberAttribute, pc);
                Logger.debug("register write (decl) for " + v.name + " at line " + line);
                
                jv.recordLocalVariables(codeAttribute, insertionPc);
                jv.recordParams(method.getParameterTypes(), Modifier.isStatic(method.getModifiers()));
                jv.setMaxLocals(codeAttribute.getMaxLocals());
                jv.compileStmnt("$$variables[" + v.index + "].mutate(" + v.name + ");");
                
				Bytecode b = jv.getBytecode();
				int locals = b.getMaxLocals();
				int stack = b.getMaxStack();
				codeAttribute.setMaxLocals(locals);

				if (stack > codeAttribute.getMaxStack()) {
					codeAttribute.setMaxStack(stack);
				}
				iterator.insert(insertionPc, b.get());
				iterator.insert(b.getExceptionTable(), insertionPc);                

                CodeIterator codeIterator = codeAttribute.iterator();
                codeIterator.move(iterator.lookAhead());
                
                VariableAccess lastAccess = VariableAccess.STORE;
                
                while (codeIterator.hasNext()) {
                    int index = codeIterator.next();
                    int op = codeIterator.byteAt(index);
                    
                    int varNumber = -1;
                    boolean loadOp = true;
                    
                    /*
                     * 
                     * Prevent tracking read accesses made by localvariableenhancer
                     * - when write access, skip next load
                     */
                    
                    // La variable change
                    if (storeByCode.containsKey(op)) {
                        varNumber = storeByCode.get(op);
                        loadOp = false; // write op
                        if (varNumber == -2) {
                            varNumber = codeIterator.byteAt(index + 1);
                        }
                    }
                    
                    if(loadByCode.containsKey(op)) {
                    	varNumber = loadByCode.get(op);
                    	if (varNumber == -2) {
                            varNumber = codeIterator.byteAt(index + 1);
                        }
                    }

                    if (varNumber == localVariableAttribute.index(v.localVariableTableIndex) && index >= v.getStartPC() && index <= v.getEndPC()) {
                    	line = toLine(lineNumberAttribute, index);
                    	if(loadOp) {
                    		if(lastAccess.equals(VariableAccess.STORE)) { // it is a call of localVarEnhancer
                    			lastAccess = VariableAccess.LOAD;
                    			continue;
                    		}
                    		Logger.debug("register read for " + v.name + " at line " + line);
                    		lastAccess = VariableAccess.LOAD;
                    	} else {
                    		Logger.debug("register write for " + v.name + " at line " + line);
                    		lastAccess = VariableAccess.STORE;
                    		jv.compileStmnt("$$variables[" + v.index + "].mutate(" + v.name + ");");
                        	
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

            method.insertBefore(insertBefore.toString());
            
            insertAfter.append("play.modules.tracer.plugin.Tracer.exitMethod();");
            
            method.insertAfter(insertAfter.toString());
            codeAttribute.computeMaxStack();
        }

        
        applicationClass.enhancedByteCode = ctClass.toBytecode();
        // debug purpose
        /*File f = new File("/tmp");
        for(String s : applicationClass.name.replaceAll("\\.(?!class)", "/").split("/")) {
        	if(!s.endsWith(".class")) {
        		f = new File(f, s);
        		if(!f.exists())
        			f.mkdir();
        	}
        }
        IO.write(ctClass.toBytecode(), new File("/tmp/"+applicationClass.name.replaceAll("\\.(?!class)", "/")+".class"));*/
        ctClass.defrost();
        
    }

    private static Map<Integer, Integer> storeByCode = new HashMap<Integer, Integer>();
    private static Map<Integer, Integer> loadByCode = new HashMap<Integer, Integer>();

    public static enum VariableAccess {
    	LOAD,
    	STORE
    }
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
        
        loadByCode.put(CodeIterator.ALOAD_0, 0);
        loadByCode.put(CodeIterator.ALOAD_1, 1);
        loadByCode.put(CodeIterator.ALOAD_2, 2);
        loadByCode.put(CodeIterator.ALOAD_3, 3);
        loadByCode.put(CodeIterator.ALOAD, -2);

        loadByCode.put(CodeIterator.ILOAD_0, 0);
        loadByCode.put(CodeIterator.ILOAD_1, 1);
        loadByCode.put(CodeIterator.ILOAD_2, 2);
        loadByCode.put(CodeIterator.ILOAD_3, 3);
        loadByCode.put(CodeIterator.ILOAD, -2);

        loadByCode.put(CodeIterator.LLOAD_0, 0);
        loadByCode.put(CodeIterator.LLOAD_1, 1);
        loadByCode.put(CodeIterator.LLOAD_2, 2);
        loadByCode.put(CodeIterator.LLOAD_3, 3);
        loadByCode.put(CodeIterator.LLOAD, -2);

        loadByCode.put(CodeIterator.FLOAD_0, 0);
        loadByCode.put(CodeIterator.FLOAD_1, 1);
        loadByCode.put(CodeIterator.FLOAD_2, 2);
        loadByCode.put(CodeIterator.FLOAD_3, 3);
        loadByCode.put(CodeIterator.FLOAD, -2);

        loadByCode.put(CodeIterator.DLOAD_0, 0);
        loadByCode.put(CodeIterator.DLOAD_1, 1);
        loadByCode.put(CodeIterator.DLOAD_2, 2);
        loadByCode.put(CodeIterator.DLOAD_3, 3);
        loadByCode.put(CodeIterator.DLOAD, -2);
    }
    
    static String getOp(int op) {
        try {
            for(Field f : Opcode.class.getDeclaredFields()) {
                if(java.lang.reflect.Modifier.isStatic(f.getModifiers()) && java.lang.reflect.Modifier.isPublic(f.getModifiers()) && f.getInt(null) == op) {
                    return (op + " " + f.getName());
                }
            }
        } catch(Exception e) {
        }
        return null;
    }
    
    static String parseVariableSignature(String sig) {
    	if(sig.startsWith("[")) { // table
    		return parseVariableSignature(sig.substring(1))+"[]";
    	}
    	char type = sig.charAt(0);
    	switch(type) {
    	case 'Z': return "java.lang.Boolean";
    	case 'B': return "java.lang.Byte";
    	case 'C': return "java.lang.Character";
    	case 'S': return "java.lang.Short";
    	case 'I': return "java.lang.Integer";
    	case 'J': return "java.lang.Long";
    	case 'F': return "java.lang.Float";
    	case 'D': return "java.lang.Double";
    	case 'L': return sig.substring(1).replace("/", ".").replace(";", "");
    	}
    	throw new RuntimeException("TracerEnhancer: parseLocalVariableSignature '"+sig+"' cannot be parsed");
    }
    
    static boolean isPlayPropertyAccessor(CtMethod method) {
    	Object[] annotations = method.getAvailableAnnotations();
    	for(Object annotation : annotations) {
    		if(PlayPropertyAccessor.class.isInstance(annotation) || PlayPropertyAccessor.class.equals(annotation.getClass()))
    			return true;
    	}
    	return false;
    }
    
    static int toLine(LineNumberAttribute lattr, int pc) {
    	int i = 0;
    	while(i < lattr.tableLength()) {
    		if(lattr.startPc(i) >= pc)
    			return lattr.lineNumber(i > 0 ? (i - 1) : 0);
    		i++;
    	}
    	return -1;
    }
    
    static List<CtField> getFields(CtClass ctClass, boolean includePrivate) {
    	ArrayList<CtField> fields = new ArrayList<CtField>();
    	if(includePrivate) {
    		for(CtField field : ctClass.getDeclaredFields()) {
    			fields.add(field);
    		}
    		try {
	    		CtClass superclass = ctClass.getSuperclass();
	    		if(superclass != null)
	    			fields.addAll(getFields(ctClass.getSuperclass(), false));
    		} catch (NotFoundException e) {
    			// nothing to do...
    		}
    	} else {
    		for(CtField field : ctClass.getFields()) {
    			fields.add(field);
    		}
    	}
    	return fields;
    }
}
