package play.classloading;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import play.Play;

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
        public File javaFile;
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
            StringWriter source = new StringWriter();
            PrintWriter out = new PrintWriter(source);
            InputStream is = null;
            try {
                is = new FileInputStream(this.javaFile);
                BufferedReader reader = new BufferedReader(new InputStreamReader(is, "utf-8"));
                String line = null;
                while ((line = reader.readLine()) != null) {
                    out.println(line);
                }
                this.javaSource = source.toString();
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                try {
                    is.close();
                } catch (Exception e) {
                }
            }
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
    public File getJava(String name) {
        name = name.replace(".", "/") + ".java";
        for (File path : Play.javaPath) {
            File javaFile = new File(path, name);
            if (javaFile.exists()) {
                return javaFile;
            }
        }
        return null;
    }
}
