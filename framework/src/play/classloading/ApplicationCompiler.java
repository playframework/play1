package play.classloading;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;

import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.ClassFile;
import org.eclipse.jdt.internal.compiler.CompilationResult;
import org.eclipse.jdt.internal.compiler.DefaultErrorHandlingPolicies;
import org.eclipse.jdt.internal.compiler.ICompilerRequestor;
import org.eclipse.jdt.internal.compiler.IErrorHandlingPolicy;
import org.eclipse.jdt.internal.compiler.IProblemFactory;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileReader;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFormatException;
import org.eclipse.jdt.internal.compiler.env.ICompilationUnit;
import org.eclipse.jdt.internal.compiler.env.INameEnvironment;
import org.eclipse.jdt.internal.compiler.env.NameEnvironmentAnswer;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.jdt.internal.compiler.problem.DefaultProblemFactory;
import org.eclipse.jdt.internal.compiler.Compiler;

import play.Play;
import play.classloading.ApplicationClasses.ApplicationClass;
import play.exceptions.JavaCompilationException;
import play.exceptions.UnexpectedException;

public class ApplicationCompiler {

    ApplicationClasses applicationClasses;
    Map<String, String> settings;

    /**
     * Try to guess the magic configuration options
     */
    public ApplicationCompiler(ApplicationClasses applicationClasses) {
        this.applicationClasses = applicationClasses;
        this.settings = new HashMap<String, String>();
        this.settings.put(CompilerOptions.OPTION_ReportMissingSerialVersion, CompilerOptions.IGNORE);
        this.settings.put(CompilerOptions.OPTION_LineNumberAttribute, CompilerOptions.GENERATE);
        this.settings.put(CompilerOptions.OPTION_SourceFileAttribute, CompilerOptions.GENERATE);
        this.settings.put(CompilerOptions.OPTION_ReportDeprecation, CompilerOptions.IGNORE);
        this.settings.put(CompilerOptions.OPTION_ReportUnusedImport, CompilerOptions.IGNORE);
        this.settings.put(CompilerOptions.OPTION_Encoding, "UTF-8");
        this.settings.put(CompilerOptions.OPTION_LocalVariableAttribute, CompilerOptions.GENERATE);
        this.settings.put(CompilerOptions.OPTION_Source, CompilerOptions.VERSION_1_5);
        this.settings.put(CompilerOptions.OPTION_TargetPlatform, CompilerOptions.VERSION_1_5);
        this.settings.put(CompilerOptions.OPTION_PreserveUnusedLocal, CompilerOptions.PRESERVE);
        this.settings.put(CompilerOptions.OPTION_Compliance, CompilerOptions.VERSION_1_5);
    }

    /**
     * Something to compile
     */
    final class CompilationUnit implements ICompilationUnit {

        final private String clazzName;
        final private String fileName;
        final private char[] typeName;
        final private char[][] packageName;

        CompilationUnit(final String pClazzName) {
            clazzName = pClazzName;
            fileName = pClazzName.replace('.', '/') + ".java";
            int dot = clazzName.lastIndexOf('.');
            if (dot > 0) {
                typeName = clazzName.substring(dot + 1).toCharArray();
            } else {
                typeName = clazzName.toCharArray();
            }
            StringTokenizer izer = new StringTokenizer(clazzName, ".");
            packageName = new char[izer.countTokens() - 1][];
            for (int i = 0; i < packageName.length; i++) {
                packageName[i] = izer.nextToken().toCharArray();
            }
        }

        public char[] getFileName() {
            return fileName.toCharArray();
        }

        public char[] getContents() {
            return applicationClasses.getApplicationClass(clazzName).javaSource.toCharArray();
        }

        public char[] getMainTypeName() {
            return typeName;
        }

        public char[][] getPackageName() {
            return packageName;
        }
    }

    /**
     * Please compile this className
     */
    public void compile(String className) {

        ICompilationUnit compilationUnit = new CompilationUnit(className);
        IErrorHandlingPolicy policy = DefaultErrorHandlingPolicies.exitOnFirstError();
        IProblemFactory problemFactory = new DefaultProblemFactory(Locale.ENGLISH);

        /**
         * To find types ...
         */
        INameEnvironment nameEnvironment = new INameEnvironment() {

            public NameEnvironmentAnswer findType(final char[][] compoundTypeName) {
                final StringBuffer result = new StringBuffer();
                for (int i = 0; i < compoundTypeName.length; i++) {
                    if (i != 0) {
                        result.append('.');
                    }
                    result.append(compoundTypeName[i]);
                }
                return findType(result.toString());
            }

            public NameEnvironmentAnswer findType(final char[] typeName, final char[][] packageName) {
                final StringBuffer result = new StringBuffer();
                for (int i = 0; i < packageName.length; i++) {
                    result.append(packageName[i]);
                    result.append('.');
                }
                result.append(typeName);
                return findType(result.toString());
            }

            private NameEnvironmentAnswer findType(final String name) {
                try {

                    char[] fileName = name.toCharArray();
                    ApplicationClass applicationClass = applicationClasses.getApplicationClass(name);

                    // ApplicationClass exists
                    if (applicationClass != null) {

                        if (applicationClass.isCompiled()) {
                            ClassFileReader classFileReader = new ClassFileReader(applicationClass.javaByteCode, fileName, true);
                            return new NameEnvironmentAnswer(classFileReader, null);
                        } else {
                            // Cascade compilation
                            ICompilationUnit compilationUnit = new CompilationUnit(name);
                            return new NameEnvironmentAnswer(compilationUnit, null);
                        }
                    }

                    // So it's a standard class
                    byte[] bytes = Play.classloader.getClassDefinition(name);
                    if (bytes != null) {
                        ClassFileReader classFileReader = new ClassFileReader(bytes, fileName, true);
                        return new NameEnvironmentAnswer(classFileReader, null);
                    }

                    // So it does not exist
                    return null;
                } catch (ClassFormatException e) {
                    // Something very very bad
                    throw new UnexpectedException(e);
                }
            }

            public boolean isPackage(char[][] parentPackageName, char[] packageName) {
                // Rebuild something usable
                StringBuilder sb = new StringBuilder();
                if (parentPackageName != null) {
                    for (char[] p : parentPackageName) {
                        sb.append(new String(p));
                        sb.append(".");
                    }
                }
                sb.append(new String(packageName));
                String name = sb.toString();
                // Check if thera a .java or .class for this ressource
                if (Play.classloader.getClassDefinition(name) != null) {
                    return false;
                }
                if (applicationClasses.getApplicationClass(name) != null) {
                    return false;
                }
                return true;
            }

            public void cleanup() {
            }
        };

        /**
         * Compilation result
         */
        ICompilerRequestor compilerRequestor = new ICompilerRequestor() {

            public void acceptResult(CompilationResult result) {
                // If error
                if (result.hasErrors()) {
                    IProblem[] problems = result.getErrors();
                    for (int i = 0; i < problems.length; i++) {
                        IProblem problem = problems[i];
                        String className = new String(problem.getOriginatingFileName()).replace("/", ".");
                        className = className.substring(0, className.length()-5);
                        throw new JavaCompilationException(Play.classes.getApplicationClass(className), problem);
                    }
                }
                // Something has been compiled
                ClassFile[] clazzFiles = result.getClassFiles();
                for (int i = 0; i < clazzFiles.length; i++) {
                    final ClassFile clazzFile = clazzFiles[i];
                    final char[][] compoundName = clazzFile.getCompoundName();
                    final StringBuffer clazzName = new StringBuffer();
                    for (int j = 0; j < compoundName.length; j++) {
                        if (j != 0) {
                            clazzName.append('.');
                        }
                        clazzName.append(compoundName[j]);
                    }
                    applicationClasses.getApplicationClass(clazzName.toString()).javaByteCode = clazzFile.getBytes();
                }
            }
        };

        /**
         * The JDT compiler
         */
        Compiler jdtCompiler = new Compiler(nameEnvironment, policy, settings, compilerRequestor, problemFactory) {
            @Override
            protected void handleInternalException(Throwable e, CompilationUnitDeclaration ud, CompilationResult result) {
                super.handleInternalException(e, ud, result);
            }
        };

        // Go !
        jdtCompiler.compile(new ICompilationUnit[]{compilationUnit});

    }
}
