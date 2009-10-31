package play.scalasupport.core;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import play.classloading.ApplicationClasses.ApplicationClass;
import play.libs.IO;
import play.vfs.VirtualFile;

public class ScalaApplicationClass extends ApplicationClass {
    
    public static List<ApplicationClass> allScalaClasses = new ArrayList<ApplicationClass>();

    public ScalaApplicationClass(String name, VirtualFile file) {
        this.name = name;
        this.javaFile = file;
        this.refresh();
    }

    @Override
    public byte[] compile() {
        if(javaByteCode == null) {
            ScalaCompiler.compileAll(null);
        }
        return javaByteCode;
    }

    @Override
    public byte[] enhance() {
        byte[] code = super.enhance();
        try {
            IO.write(code, new File("/tmp/" + name + ".enhanced.class"));
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return code;
    }
    
    
    
}
