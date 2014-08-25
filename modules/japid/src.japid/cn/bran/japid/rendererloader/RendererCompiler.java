package cn.bran.japid.rendererloader;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.eclipse.jdt.internal.compiler.CompilationResult;
import org.eclipse.jdt.internal.compiler.Compiler;
import org.eclipse.jdt.internal.compiler.DefaultErrorHandlingPolicies;
import org.eclipse.jdt.internal.compiler.ICompilerRequestor;
import org.eclipse.jdt.internal.compiler.IErrorHandlingPolicy;
import org.eclipse.jdt.internal.compiler.IProblemFactory;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.env.ICompilationUnit;
import org.eclipse.jdt.internal.compiler.env.INameEnvironment;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.jdt.internal.compiler.problem.DefaultProblemFactory;

import cn.bran.japid.util.JapidFlags;
//import play.Play;



/**
 * Java compiler (uses eclipse JDT)
 * 
 * based on 
 */
public class RendererCompiler {

    Map<String, Boolean> packagesCache = new HashMap<String, Boolean>();
    Map<String, RendererClass> japidClasses ;//= new HashMap<String, RendererClass>();
    
    public TemplateClassLoader crlr;
    
    Map<String, String> settings;
    {
    	/**
    	 * Try to guess the magic configuration options
    	 */
    	this.settings = new HashMap<String, String>();
        this.settings.put(CompilerOptions.OPTION_ReportMissingSerialVersion, CompilerOptions.IGNORE);
        this.settings.put(CompilerOptions.OPTION_LineNumberAttribute, CompilerOptions.GENERATE);
        this.settings.put(CompilerOptions.OPTION_SourceFileAttribute, CompilerOptions.GENERATE);
        this.settings.put(CompilerOptions.OPTION_ReportDeprecation, CompilerOptions.IGNORE);
        this.settings.put(CompilerOptions.OPTION_ReportUnusedImport, CompilerOptions.IGNORE);
        this.settings.put(CompilerOptions.OPTION_Encoding, "UTF-8");
        this.settings.put(CompilerOptions.OPTION_LocalVariableAttribute, CompilerOptions.GENERATE);
        
        String javaVersion = CompilerOptions.VERSION_1_6;
        String javaVersionProperty = System.getProperty("java.version");
		if(javaVersionProperty.startsWith("1.6")) {
            javaVersion = CompilerOptions.VERSION_1_6;
        } else if (javaVersionProperty.startsWith("1.7")) {
            javaVersion = CompilerOptions.VERSION_1_7;
	    } else if (javaVersionProperty.startsWith("1.8")) {
	    	javaVersion = CompilerOptions.VERSION_1_8;
	    }
     
//		Object javaSourceConfig = Play.configuration.get("java.source");
//		if("1.5".equals(javaSourceConfig)) {
//            javaVersion = CompilerOptions.VERSION_1_5;
//        } else if("1.6".equals(javaSourceConfig)) {
//            javaVersion = CompilerOptions.VERSION_1_6;
//        } else if("1.7".equals(javaSourceConfig)) {
//            javaVersion = CompilerOptions.VERSION_1_7;
//	    } else if("1.8".equals(javaSourceConfig)) {
//	    	javaVersion = CompilerOptions.VERSION_1_8;
//	    }
		
        JapidFlags.info("JDT compiler compiles to java version: " + javaVersion);
        
        this.settings.put(CompilerOptions.OPTION_Source, javaVersion);
        this.settings.put(CompilerOptions.OPTION_TargetPlatform, javaVersion);
        this.settings.put(CompilerOptions.OPTION_PreserveUnusedLocal, CompilerOptions.PRESERVE);
        this.settings.put(CompilerOptions.OPTION_Compliance, javaVersion);
    }

    /**
     * supposed to have a single instance for the entire application
     * 
     * @param classes the Japid template class container
     * @param cl
     */
    public RendererCompiler(Map<String, RendererClass> classes, TemplateClassLoader cl ) {
        this.japidClasses = classes;
        this.crlr = cl;
    }

    /**
     * Please compile this className and set the bytecode to the class holder 
     */
    @SuppressWarnings("deprecation")
    public void compile(String[] classNames) {

        ICompilationUnit[] compilationUnits = new CompilationUnit[classNames.length];
        for (int i = 0; i < classNames.length; i++) {
            compilationUnits[i] = new CompilationUnit(this, classNames[i]);
        }
        IErrorHandlingPolicy policy = DefaultErrorHandlingPolicies.exitOnFirstError();
        IProblemFactory problemFactory = new DefaultProblemFactory(Locale.ENGLISH);

        /**
         * To find types ...
         */
        INameEnvironment nameEnvironment = new NameEnv(this);

        /**
         * Compilation result
         */
        ICompilerRequestor compilerRequestor = new CompilerRequestor(this);

        /**
         * The JDT compiler
         */
        Compiler jdtCompiler = new Compiler(nameEnvironment, policy, settings, compilerRequestor, problemFactory) {

            @Override
            protected void handleInternalException(Throwable e, CompilationUnitDeclaration ud, CompilationResult result) {
            }
        };

        // Go !
        jdtCompiler.compile(compilationUnits);

    }
}
