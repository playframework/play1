package play.classloading;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.UnmodifiableClassException;
import java.util.ArrayList;
import java.util.List;
import play.Logger;
import play.Play;
import play.vfs.VirtualFile;
import play.classloading.ApplicationClasses.ApplicationClass;
import play.exceptions.UnexpectedException;

/**
 * The application classLoader. Load the classes from
 * the application Java sources files.
 * @author guillaume
 */
public class ApplicationClassloader extends ClassLoader {

    public ApplicationClassloader() {
        super(ApplicationClassloader.class.getClassLoader());
        // Clean the existing classes
        for (ApplicationClass applicationClass : Play.classes.all()) {
            applicationClass.uncompile();
        }
    }

    @Override
    protected synchronized Class loadClass(String name, boolean resolve) throws ClassNotFoundException {

        // First check if it's an application Class
        Class applicationClass = loadApplicationClass(name);
        if (applicationClass != null) {
            if (resolve) {
                resolveClass(applicationClass);
            }
            return applicationClass;
        }

        // Delegate to the classic classloader
        return super.loadClass(name, resolve);
    }    // ~~~~~~~~~~~~~~~~~~~~~~~
    public ThreadLocal<List<ApplicationClass>> loadingTracer = new ThreadLocal<List<ApplicationClass>>();

    protected Class loadApplicationClass(String name) {
        ApplicationClass applicationClass = Play.classes.getApplicationClass(name);
        if (applicationClass != null) {
            if (loadingTracer.get() != null) {
                loadingTracer.get().add(applicationClass);
            }
            if (applicationClass.isCompiled()) {
                return applicationClass.javaClass;
            } else {
                if(applicationClass.compile() != null) {
                    applicationClass.enhance();
                    applicationClass.javaClass = defineClass(applicationClass.name, applicationClass.enhancedByteCode, 0, applicationClass.enhancedByteCode.length);
                    resolveClass(applicationClass.javaClass);              
                    return applicationClass.javaClass;
                } else {
                    Play.classes.classes.remove(name);                    
                }
            }
        }
        return null;
    }

    protected byte[] getClassDefinition(String name) {
        name = name.replace(".", "/") + ".class";
        InputStream is = getResourceAsStream(name);
        if (is == null) {
            return null;
        }
        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int count;
            while ((count = is.read(buffer, 0, buffer.length)) > 0) {
                os.write(buffer, 0, count);
            }
            return os.toByteArray();
        } catch (Exception e) {
            throw new UnexpectedException(e);
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                throw new UnexpectedException(e);
            }
        }
    }

    /**
     * Detect Java changes
     */
    public void detectChanges() {
        List<ApplicationClass> modifieds = new ArrayList<ApplicationClass>();
        for (ApplicationClass applicationClass : Play.classes.all()) {
            if (applicationClass.timestamp < applicationClass.javaFile.lastModified()) {
                applicationClass.refresh();
                modifieds.add(applicationClass);
            }
        }
        List<ClassDefinition> newDefinitions = new ArrayList<ClassDefinition>();
        for (ApplicationClass applicationClass : modifieds) {
            long start = System.currentTimeMillis();
            applicationClass.compile();
            applicationClass.enhance();
            if (applicationClass.javaClass != null) {
                newDefinitions.add(new ClassDefinition(applicationClass.javaClass, applicationClass.enhancedByteCode));
            }
            Logger.trace("%sms to compile & enhance %s", System.currentTimeMillis() - start, applicationClass.name);
        }
        try {
            HotswapAgent.reload(newDefinitions.toArray(new ClassDefinition[newDefinitions.size()]));
        } catch (ClassNotFoundException e) {
            throw new UnexpectedException(e);
        } catch (UnmodifiableClassException e) {
            throw new UnexpectedException(e);
        }
    }

    /**
     * Try to load all .java files found.
     * @return The list of well defined Class
     */
    public List<Class> getAllClasses() {
        List<Class> res = new ArrayList<Class>();
        for (VirtualFile virtualFile : Play.javaPath) {
            res.addAll(getAllClasses(virtualFile));
        }
        return res;
    }
    
    /**
     * Try to load all .java files found in a single path.
     * @return The list of well defined Class
     */
    public List<Class> getAllClasses(VirtualFile path) {
        return getAllClasses(path, "");
    }
    
    /**
     * Try to load all .java files found in a single path within a single package.
     * @return The list of well defined Class
     */
    public List<Class> getAllClasses(VirtualFile path, String basePackage) {
        if(basePackage.length()>0 && !basePackage.endsWith(".")) {
            basePackage += ".";
        }
        List<Class> res = new ArrayList<Class>();
        for(VirtualFile virtualFile : path.list()) {
            scan(res, basePackage, virtualFile);
        }
        return res;
    }

    private void scan(List<Class> classes, String packageName, VirtualFile current) {
        if (!current.isDirectory()) {
            if (current.getName().endsWith(".java")) {
                String classname = packageName + current.getName().substring(0, current.getName().length() - 5);      
                Class clazz = loadApplicationClass(classname);
                if(clazz == null) {
                    Logger.error("%s was found but class %s cannot be loaded", current, classname);
                } else {
                    classes.add(clazz);
                }
            }
        } else {
            for (VirtualFile virtualFile : current.list()) {
                scan(classes, packageName + current.getName() + ".", virtualFile);
            }
        }
    }
}
