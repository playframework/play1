package play.classloading;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import play.Logger;
import play.Play;
import play.classloading.enhancers.ControllersEnhancer;
import play.classloading.enhancers.Enhancer;
import play.classloading.enhancers.JPAEnhancer;
import play.classloading.enhancers.LocalvariablesNamesEnhancer;
import play.classloading.enhancers.PropertiesEnhancer;
import play.classloading.enhancers.ZDBEnhancer;
import play.exceptions.UnexpectedException;
import play.vfs.VirtualFile;

/**
 * Application classes container.
 */
public class ApplicationClasses {

    ApplicationCompiler compiler = new ApplicationCompiler(this);
    Map<String, ApplicationClass> classes = new HashMap<String, ApplicationClass>();

    public ApplicationClasses() {
    }

    public void clear() {
        classes = new HashMap<String, ApplicationClass>();
    }

    /**
     * Get a class by name
     * @param name The fully qualified class name
     * @return The ApplicationClass or null
     */
    public ApplicationClass getApplicationClass(String name) {
        if (!classes.containsKey(name) && getJava(name) != null) {
            classes.put(name, new ApplicationClass(name));
        }
        return classes.get(name);
    }

    public List<ApplicationClass> getAssignableClasses(Class clazz) {
        List<ApplicationClass> results = new ArrayList<ApplicationClass>();
        for (ApplicationClass applicationClass : classes.values()) {
            if (clazz.isAssignableFrom(applicationClass.javaClass) && !applicationClass.javaClass.getName().equals(clazz.getName())) {
                results.add(applicationClass);
            }
        }
        return results;
    }

    public List<ApplicationClass> getAnnotatedClasses(Class clazz) {
        List<ApplicationClass> results = new ArrayList<ApplicationClass>();
        for (ApplicationClass applicationClass : classes.values()) {
            if (applicationClass.javaClass.isAnnotationPresent(clazz)) {
                results.add(applicationClass);
            }
        }
        return results;
    }

    /**
     * All loaded classes.
     * @return All loaded classes
     */
    public List<ApplicationClass> all() {
        return new ArrayList<ApplicationClass>(classes.values());
    }

    /**
     * Does this class is already loaded ?
     * @param name The fully qualified class name
     * @return
     */
    public boolean hasClass(String name) {
        return classes.containsKey(name);
    }    // Enhancers
    Class[] enhancers = new Class[]{
        ControllersEnhancer.class,
        LocalvariablesNamesEnhancer.class,
        PropertiesEnhancer.class,
        JPAEnhancer.class,
        ZDBEnhancer.class
    };

    /**
     * Represent a application class
     */
    public class ApplicationClass {

        /**
         * The fully qualified class name
         */
        public String name;
        /**
         * A reference to the java source file
         */
        public VirtualFile javaFile;
        /**
         * The Java source
         */
        public String javaSource;
        /**
         * The compiled byteCode
         */
        public byte[] javaByteCode;
        /**
         * The enhanced byteCode
         */
        public byte[] enhancedByteCode;
        /**
         * The in JVM loaded class
         */
        public Class javaClass;
        /**
         * Last time than this class was compiled
         */
        public Long timestamp = 0L;
        /**
         * Is this class compiled
         */
        boolean compiled;

        public ApplicationClass(String name) {
            this.name = name;
            this.javaFile = getJava(name);
            this.refresh();
        }

        /**
         * Need to refresh this class !
         */
        public void refresh() {
            this.javaSource = this.javaFile.contentAsString();
            this.javaByteCode = null;
            this.enhancedByteCode = null;
            this.compiled = false;
            this.timestamp = 0L;
        }

        /**
         * Enhance this class
         * @return the enhanced byteCode
         */
        public byte[] enhance() {
            for (Class enhancer : enhancers) {
                try {
                    ((Enhancer) enhancer.newInstance()).enhanceThisClass(this);
                } catch (Exception e) {
                    throw new UnexpectedException("While applying " + enhancer + " on " + name, e);
                }
            }
            return this.enhancedByteCode;

        }

        /**
         * Is this class already compiled ?
         * @return
         */
        public boolean isDefinable() {
            return compiled && javaClass != null;
        }

        /**
         * Compile the class from Java source
         * @return
         */
        public byte[] compile() {
            long start = System.currentTimeMillis();
            compiler.compile(new String[] {this.name});             
            Logger.trace("%sms to compile class %s", System.currentTimeMillis()-start, name);
            return this.javaByteCode;
        }

        /**
         * Unload the class
         */
        public void uncompile() {
            this.javaClass = null;
        }
        
        public void compiled(byte[] code) {
            javaByteCode = code;
            enhancedByteCode = code;
            compiled = true;
            this.timestamp = this.javaFile.lastModified();  
        }
    }

    // ~~ Utils
    /**
     * Retrieve the corresponding source file for a given class name.
     * It handle innerClass too !
     * @param name The fully qualified class name 
     * @return The virtualFile if found
     */
    public VirtualFile getJava(String name) {
        String fileName = name;
        if (fileName.contains("$")) {
            fileName = fileName.substring(0, fileName.indexOf("$"));
        }
        fileName = fileName.replace(".", "/") + ".java";
        for (VirtualFile path : Play.javaPath) {
            VirtualFile javaFile = path.child(fileName);
            if (javaFile.exists()) {
                return javaFile;
            }
        }
        return null;
    }
}
