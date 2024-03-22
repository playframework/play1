package play.classloading;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.stream.Stream;

import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.internal.compiler.ClassFile;
import org.eclipse.jdt.internal.compiler.CompilationResult;
import org.eclipse.jdt.internal.compiler.Compiler;
import org.eclipse.jdt.internal.compiler.DefaultErrorHandlingPolicies;
import org.eclipse.jdt.internal.compiler.ICompilerRequestor;
import org.eclipse.jdt.internal.compiler.IErrorHandlingPolicy;
import org.eclipse.jdt.internal.compiler.IProblemFactory;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileReader;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFormatException;
import org.eclipse.jdt.internal.compiler.env.ICompilationUnit;
import org.eclipse.jdt.internal.compiler.env.INameEnvironment;
import org.eclipse.jdt.internal.compiler.env.NameEnvironmentAnswer;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.jdt.internal.compiler.problem.DefaultProblemFactory;

import play.Logger;
import play.Play;
import play.classloading.ApplicationClasses.ApplicationClass;
import play.exceptions.CompilationException;
import play.exceptions.UnexpectedException;

/**
 * Java compiler (uses eclipse JDT)
 */
public class ApplicationCompiler {

    private static final String JAVA_SOURCE_DEFAULT_VERSION = "11";
    static final Map<String, String> compatibleJavaVersions = Map.of(
        "11", CompilerOptions.VERSION_11,
        "12", CompilerOptions.VERSION_12,
        "13", CompilerOptions.VERSION_13,
        "14", CompilerOptions.VERSION_14,
        "15", CompilerOptions.VERSION_15,
        "16", CompilerOptions.VERSION_16,
        "17", CompilerOptions.VERSION_17,
        "18", CompilerOptions.VERSION_18,
        "19", CompilerOptions.VERSION_19,
        "21", CompilerOptions.VERSION_21
    );

    final Map<String, Boolean> packagesCache = new HashMap<>();
    final ApplicationClasses applicationClasses;
    final Map<String, String> settings;

    /**
     * Try to guess the magic configuration options
     *
     * @param applicationClasses
     *            The application classes container
     */
    public ApplicationCompiler(ApplicationClasses applicationClasses) {
        final String runningJavaVersion = System.getProperty("java.version");
        if (Stream.of("1.5", "1.6", "1.7", "1.8", "9", "10").anyMatch(runningJavaVersion::startsWith)) {
            throw new CompilationException("JDK version prior to 11 are not supported to run the application");
        }

        final String configSourceVersion = Play.configuration.getProperty("java.source", JAVA_SOURCE_DEFAULT_VERSION);
        final String jdtVersion = compatibleJavaVersions.get(configSourceVersion);
        if (jdtVersion == null) {
            throw new CompilationException(String.format("Incompatible Java version specified (%s). Compatible versions are: %s",
                configSourceVersion, compatibleJavaVersions.keySet()));
        }

        this.applicationClasses = applicationClasses;
        this.settings = Map.ofEntries(
            Map.entry(CompilerOptions.OPTION_ReportMissingSerialVersion, CompilerOptions.IGNORE),
            Map.entry(CompilerOptions.OPTION_LineNumberAttribute, CompilerOptions.GENERATE),
            Map.entry(CompilerOptions.OPTION_SourceFileAttribute, CompilerOptions.GENERATE),
            Map.entry(CompilerOptions.OPTION_ReportDeprecation, CompilerOptions.IGNORE),
            Map.entry(CompilerOptions.OPTION_ReportUnusedImport, CompilerOptions.IGNORE),
            Map.entry(CompilerOptions.OPTION_LocalVariableAttribute, CompilerOptions.GENERATE),
            Map.entry(CompilerOptions.OPTION_PreserveUnusedLocal, CompilerOptions.PRESERVE),
            Map.entry(CompilerOptions.OPTION_MethodParametersAttribute, CompilerOptions.GENERATE),
            Map.entry(CompilerOptions.OPTION_Encoding, "UTF-8"),
            Map.entry(CompilerOptions.OPTION_Source, jdtVersion),
            Map.entry(CompilerOptions.OPTION_TargetPlatform, jdtVersion),
            Map.entry(CompilerOptions.OPTION_Compliance, jdtVersion)
        );
    }

    /**
     * Something to compile
     */
    final class CompilationUnit implements ICompilationUnit {

        private final String clazzName;
        private final String fileName;
        private final char[] typeName;
        private final char[][] packageName;

        CompilationUnit(String pClazzName) {
            clazzName = pClazzName;
            if (pClazzName.contains("$")) {
                pClazzName = pClazzName.substring(0, pClazzName.indexOf('$'));
            }
            fileName = pClazzName.replace('.', '/') + ".java";
            int dot = pClazzName.lastIndexOf('.');
            if (dot > 0) {
                typeName = pClazzName.substring(dot + 1).toCharArray();
            } else {
                typeName = pClazzName.toCharArray();
            }
            StringTokenizer izer = new StringTokenizer(pClazzName, ".");
            packageName = new char[izer.countTokens() - 1][];
            for (int i = 0; i < packageName.length; i++) {
                packageName[i] = izer.nextToken().toCharArray();
            }
        }

        @Override
        public char[] getFileName() {
            return fileName.toCharArray();
        }

        @Override
        public char[] getContents() {
            return applicationClasses.getApplicationClass(clazzName).javaSource.toCharArray();
        }

        @Override
        public char[] getMainTypeName() {
            return typeName;
        }

        @Override
        public char[][] getPackageName() {
            return packageName;
        }

        @Override
        public boolean ignoreOptionalProblems() {
            return false;
        }
    }

    /**
     * Please compile this className
     *
     * @param classNames
     *            Arrays of the class name to compile
     */
    @SuppressWarnings("deprecation")
    public void compile(String[] classNames) {
        ICompilationUnit[] compilationUnits = new CompilationUnit[classNames.length];
        for (int i = 0; i < classNames.length; i++) {
            compilationUnits[i] = new CompilationUnit(classNames[i]);
        }
        IErrorHandlingPolicy policy = DefaultErrorHandlingPolicies.exitOnFirstError();
        IProblemFactory problemFactory = new DefaultProblemFactory(Locale.ENGLISH);

        /**
         * To find types ...
         */
        INameEnvironment nameEnvironment = new INameEnvironment() {

            @Override
            public NameEnvironmentAnswer findType(char[][] compoundTypeName) {
                StringBuilder result = new StringBuilder(compoundTypeName.length * 7);
                for (int i = 0; i < compoundTypeName.length; i++) {
                    if (i != 0) {
                        result.append('.');
                    }
                    result.append(compoundTypeName[i]);
                }
                return findType(result.toString());
            }

            @Override
            public NameEnvironmentAnswer findType(char[] typeName, char[][] packageName) {
                StringBuilder result = new StringBuilder(packageName.length * 7 + 1 + typeName.length);
                for (final char[] element : packageName) {
                    result.append(element);
                    result.append('.');
                }
                result.append(typeName);
                return findType(result.toString());
            }

            private NameEnvironmentAnswer findType(String name) {
                try {

                    if (name.startsWith("play.") || name.startsWith("java.") || name.startsWith("javax.")) {
                        byte[] bytes = Play.classloader.getClassDefinition(name);
                        if (bytes != null) {
                            ClassFileReader classFileReader = new ClassFileReader(bytes, name.toCharArray(), true);
                            return new NameEnvironmentAnswer(classFileReader, null);
                        }
                        return null;
                    }

                    char[] fileName = name.toCharArray();
                    ApplicationClass applicationClass = applicationClasses.getApplicationClass(name);

                    // ApplicationClass exists
                    if (applicationClass != null) {

                        if (applicationClass.javaByteCode != null) {
                            ClassFileReader classFileReader = new ClassFileReader(applicationClass.javaByteCode, fileName, true);
                            return new NameEnvironmentAnswer(classFileReader, null);
                        }
                        // Cascade compilation
                        ICompilationUnit compilationUnit = new CompilationUnit(name);
                        return new NameEnvironmentAnswer(compilationUnit, null);
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

            @Override
            public boolean isPackage(char[][] parentPackageName, char[] packageName) {
                // Rebuild something usable
                String name;
                if (parentPackageName == null) {
                    name = new String(packageName);
                } else {
                    StringBuilder sb = new StringBuilder(parentPackageName.length * 7 + packageName.length);
                    for (char[] p : parentPackageName) {
                        sb.append(p);
                        sb.append(".");
                    }
                    sb.append(new String(packageName));
                    name = sb.toString();
                }

                if (packagesCache.containsKey(name)) {
                    return packagesCache.get(name);
                }
                // Check if there are .java or .class for this resource
                if (Play.classloader.getResource(name.replace('.', '/') + ".class") != null) {
                    packagesCache.put(name, false);
                    return false;
                }
                if (applicationClasses.getApplicationClass(name) != null) {
                    packagesCache.put(name, false);
                    return false;
                }
                packagesCache.put(name, true);
                return true;
            }

            @Override
            public void cleanup() {
            }
        };

        /**
         * Compilation result
         */
        ICompilerRequestor compilerRequestor = result -> {
            // If error
            if (result.hasErrors()) {
                for (IProblem problem : result.getErrors()) {
                    String className = new String(problem.getOriginatingFileName()).replace('/', '.');
                    className = className.substring(0, className.length() - 5);
                    String message = problem.getMessage();
                    if (problem.getID() == IProblem.CannotImportPackage) {
                        // Non sense !
                        message = problem.getArguments()[0] + " cannot be resolved";
                    }
                    throw new CompilationException(Play.classes.getApplicationClass(className).javaFile, message,
                            problem.getSourceLineNumber(), problem.getSourceStart(), problem.getSourceEnd());
                }
            }
            // Something has been compiled
            ClassFile[] clazzFiles = result.getClassFiles();
            for (final ClassFile clazzFile : clazzFiles) {
                char[][] compoundName = clazzFile.getCompoundName();
                StringBuilder clazzName = new StringBuilder();
                for (int j = 0; j < compoundName.length; j++) {
                    if (j != 0) {
                        clazzName.append('.');
                    }
                    clazzName.append(compoundName[j]);
                }

                if (Logger.isTraceEnabled()) {
                    Logger.trace("Compiled %s", clazzName);
                }

                applicationClasses.getApplicationClass(clazzName.toString()).compiled(clazzFile.getBytes());
            }
        };

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
