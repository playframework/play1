package play.classloading;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import play.Play;
import play.Play.VirtualFile;
import play.classloading.enhancers.ControllersEnhancer;
import play.classloading.enhancers.Enhancer;
import play.classloading.enhancers.LocalvariablesNamesEnhancer;
import play.classloading.enhancers.PropertiesEnhancer;
import play.exceptions.UnexpectedException;

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
    }
    
    // Enhancers
    Class[] enhancers = new Class[] {
        ControllersEnhancer.class,
        LocalvariablesNamesEnhancer.class,
        PropertiesEnhancer.class
    };

    public class ApplicationClass {

        public String name;
        public VirtualFile javaFile;
        public String javaSource;
        public byte[] javaByteCode;
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
            this.timestamp = this.javaFile.lastModified();
            this.compiled = false;            
        }

        public void setByteCode(byte[] compiledByteCode) {
            this.javaByteCode = compiledByteCode;                       
        }
        
        public void enhance() {
            try {       
                for(Class enhancer : enhancers) {
                    ((Enhancer)enhancer.newInstance()).enhanceThisClass(this);
                } 
            } catch(Exception e) {
                throw new UnexpectedException(e);
            }
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
        name = name.replace(".", "/") + ".java";
        for (VirtualFile path : Play.javaPath) {
            VirtualFile javaFile = new VirtualFile(path, name);
            if (javaFile.exists()) {
                return javaFile;
            }
        }
        return null;
    }
    
}
