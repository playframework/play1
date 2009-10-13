package play.scalasupport.core;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.bytecode.SourceFileAttribute;
import play.Logger;
import play.Play;
import play.classloading.ApplicationClasses.ApplicationClass;
import play.exceptions.UnexpectedException;
import play.libs.IO;
import play.vfs.VirtualFile;
import scala.tools.nsc.Global;
import scala.tools.nsc.Settings;
import scala.tools.nsc.io.AbstractFile;
import scala.tools.nsc.reporters.Reporter;
import scala.tools.nsc.util.Position;
import scala.tools.nsc.util.BatchSourceFile;
import scala.tools.nsc.util.SourceFile;

public class ScalaCompiler {
    
    static Map<String, VirtualFile> symbolic2real = new HashMap<String, VirtualFile>();

    public static void compileAll(List<ApplicationClass> all) {
        
        List<VirtualFile> sources = new ArrayList<VirtualFile>();
        for (VirtualFile virtualFile : Play.javaPath) {
            sources.addAll(getAllScalaSources(virtualFile));
        }
        
        AbstractFile dir = new CompilerOutput();
 
        Settings settings = new Settings();
        settings.debuginfo().level_$eq(3);
        
        Reporter reporter = new Reporter() {

            @Override
            public void info0(Position position, String msg, Severity severity, boolean force) {
                if(severity.id() == 2) {
                    throw new ScalaCompilationException(symbolic2real.get(position.source().get()+""), msg, position.line().get());
                }
                if(severity.id() == 1) {
                    Logger.warn(msg + ", at line "+position.line().get() + " of "+position.source().get());
                }
                if(severity.id() == 0) {
                    Logger.info(msg + ", at line "+position.line().get() + " of "+position.source().get());
                }
            }
            
        };
        
        Global compiler = new Global(settings, reporter);
        compiler.genJVM().outputDir_$eq(dir);

        // Clean
        symbolic2real.clear();
        CompilerOutput.generatedBytecode.clear();
        ScalaApplicationClass.allScalaClasses.clear();
        
        // Add scala sources
        scala.collection.jcl.ArrayList<SourceFile> batchSources = new scala.collection.jcl.ArrayList<SourceFile>();
        for(VirtualFile source : sources) {         
            symbolic2real.put(source.relativePath(), source);
            batchSources.add(new BatchSourceFile(source.relativePath(), source.contentAsString().toCharArray()));
        }
        
        // Compile now
        compiler.new Run().compileSources(batchSources.toList());  
        
        // For each compiled class
        for(String classname : CompilerOutput.generatedBytecode.keySet()) {
            byte[] bytecode = CompilerOutput.generatedBytecode.get(classname).toByteArray();
            try {
                CtClass ctClass = ClassPool.getDefault().makeClass(new ByteArrayInputStream(bytecode));
                String sourceName = ((SourceFileAttribute)ctClass.getClassFile().getAttribute("SourceFile")).getFileName();
                VirtualFile sourceFile = symbolic2real.get(sourceName);
                ApplicationClass applicationClass = Play.classes.getApplicationClass(classname);
                if(applicationClass == null) {
                    applicationClass = new ScalaApplicationClass(classname, sourceFile);
                    Play.classes.add(applicationClass);
                }
                applicationClass.compiled(bytecode);
                ScalaApplicationClass.allScalaClasses.add(applicationClass);
            } catch (IOException ex) {
                throw new UnexpectedException("Cannot read a scala generated class using javassist", ex);
            } 
        }
        
        // Done
        if(all != null) {
            all.addAll(ScalaApplicationClass.allScalaClasses);
        }
        
        // DEBUG
        try {
            for(String s : CompilerOutput.generatedBytecode.keySet()) {
                IO.write(CompilerOutput.generatedBytecode.get(s).toByteArray(), new File("/tmp/"+s+".class"));
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
        
    }
    
    
    // Scanner
    static List<VirtualFile> getAllScalaSources(VirtualFile path) {
        List<VirtualFile> files = new ArrayList<VirtualFile>();
        scan(files, path);
        return files;
    }

    static void scan(List<VirtualFile> sources, VirtualFile current) {
        if (!current.isDirectory()) {
            if (current.getName().endsWith(".scala") && !current.getName().startsWith(".")) {
                sources.add(current);
            }
        } else {
            for (VirtualFile virtualFile : current.list()) {
                scan(sources, virtualFile);
            }
        }
    }
}
