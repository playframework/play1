package play.classloading;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import play.Play;
import play.classloading.enhancers.ControllersEnhancer;
import play.classloading.enhancers.Enhancer;
import play.classloading.enhancers.JPAEnhancer;
import play.classloading.enhancers.LocalvariablesNamesEnhancer;
import play.classloading.enhancers.PropertiesEnhancer;
import play.classloading.enhancers.ZDBEnhancer;
import play.exceptions.UnexpectedException;
import play.vfs.VirtualFile;

public class ApplicationClasses {

    ApplicationCompiler compiler = new ApplicationCompiler(this);
    Map<String, ApplicationClass> classes = new HashMap<String, ApplicationClass>();

    public ApplicationClasses() {
    }

    public ApplicationClass getApplicationClass(String name) {
        if (!classes.containsKey(name) && getJava(name) != null) {
            classes.put(name, new ApplicationClass(name));
        }
        return classes.get(name);
    }

    public List<ApplicationClass> all() {
        return new ArrayList<ApplicationClass>(classes.values());
    }

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

    public class ApplicationClass {

        public String name;
        public VirtualFile javaFile;
        public String javaSource;
        public byte[] javaByteCode;
        public byte[] enhancedByteCode;
        public Class javaClass;
        public Long timestamp = 0L;
        boolean compiled;

        public ApplicationClass(String name) {
            this.name = name;
            this.javaFile = getJava(name);
            this.refresh();
        }

        public void refresh() {
            this.javaSource = this.javaFile.contentAsString();
            this.javaByteCode = null;
            this.enhancedByteCode = null;
            this.timestamp = this.javaFile.lastModified();
            this.compiled = false;
        }

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

        public boolean isCompiled() {
            return compiled && javaClass != null;
        }

        public byte[] compile() {
            compiler.compile(this.name);
            compiled = true;
            return this.javaByteCode;
        }

        public void uncompile() {
            this.javaClass = null;
        }
    }

    // ~~ Utils
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
