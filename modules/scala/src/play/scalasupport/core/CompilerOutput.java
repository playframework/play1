package play.scalasupport.core;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import scala.Iterator;
import scala.tools.nsc.io.AbstractFile;

public class CompilerOutput extends AbstractFile {
    
    static Map<String, ByteArrayOutputStream> generatedBytecode = new HashMap<String, ByteArrayOutputStream>();
    
    String path = "";
    String name = "";
    CompilerOutput parent = null;
    
    public CompilerOutput() {
    }
    
    public CompilerOutput(CompilerOutput parent, String name) {
        this.path = parent.path + ( parent.path.equals("") ? "" : "/") + name;
        this.parent = parent;
    }

    @Override 
    public AbstractFile lookupName(String name, boolean arg1) {
        return null;
    }

    @Override
    public Iterator<AbstractFile> elements() {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public OutputStream output() {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public InputStream input() {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public long lastModified() {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public boolean isDirectory() {
        return true;
    }

    @Override
    public File file() {
        return null;
    }

    @Override
    public AbstractFile container() {
        return parent;
    }

    @Override
    public String path() {
        return path;
    }

    @Override
    public String name() {
        return name;
    }

    public <B> void copyToArray(B[] arg0, int arg1) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public AbstractFile fileNamed(String name) {
        return new GeneratedClass(this, name);
    }

    @Override
    public AbstractFile subdirectoryNamed(String name) {
        return new CompilerOutput(this, name);
    }  

    public static class GeneratedClass extends AbstractFile {
        
        CompilerOutput parent;
        String path;
        String name;
        
        public GeneratedClass(CompilerOutput parent, String name) {
            this.parent = parent;
            this.name = name;
            this.path = parent.path + ( parent.path.equals("/") ? "" : "/") + name;
        }

        @Override
        public AbstractFile lookupName(String name, boolean arg1) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public Iterator<AbstractFile> elements() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public OutputStream output() {
            String className = path.replaceAll("/", ".");
            if(className.endsWith(".class")) {
                className = className.substring(0, className.length() - 6);
            }
            CompilerOutput.generatedBytecode.put(className,  new ByteArrayOutputStream());
            return CompilerOutput.generatedBytecode.get(className);
        }

        @Override
        public InputStream input() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public long lastModified() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean isDirectory() {
            return false;
        }

        @Override
        public File file() {
            return null;
        }

        @Override
        public AbstractFile container() {
            return parent;
        }

        @Override
        public String path() {
            return path;
        }

        @Override
        public String name() {
            return name;
        }

        public <B> void copyToArray(B[] arg0, int arg1) {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }
}

