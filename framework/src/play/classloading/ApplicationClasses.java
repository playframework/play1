package play.classloading;

import java.util.HashMap;
import java.util.Map;

import play.Play;
import play.Play.VirtualFile;

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

    public class ApplicationClass {

        public String name;
        public VirtualFile javaFile;
        public String javaSource;
        public byte[] javaByteCode;
        public Class javaClass;
        public Long timestamp;

        public ApplicationClass(String name) {
            this.name = name;
            this.javaFile = getJava(name);
            this.refresh();
        }

        public void refresh() {
            this.javaSource = this.javaFile.contentAsString();
            this.javaByteCode = null;
            this.timestamp = this.javaFile.lastModified();
        }

        public void setByteCode(byte[] compiledByteCode) {
            this.javaByteCode = compiledByteCode;
        }

        public boolean isCompiled() {
            return javaClass != null;
        }

        public byte[] compile() {
            compiler.compile(this.name);
            return this.javaByteCode;
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
