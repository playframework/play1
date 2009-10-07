package play.classloading;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.UnmodifiableClassException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.AllPermission;
import java.security.CodeSource;
import java.security.Permissions;
import java.security.ProtectionDomain;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import play.Logger;
import play.Play;
import play.PlayPlugin;
import play.vfs.VirtualFile;
import play.cache.Cache;
import play.classloading.ApplicationClasses.ApplicationClass;
import play.exceptions.UnexpectedException;

/**
 * The application classLoader. 
 * Load the classes from the application Java sources files.
 */
public class ApplicationClassloader extends ClassLoader {

    /**
     * This protection domain will be affecter to all loaded classes.
     */
    public ProtectionDomain protectionDomain;

    public ApplicationClassloader() {
        super(ApplicationClassloader.class.getClassLoader());
        // Clean the existing classes
        for (ApplicationClass applicationClass : Play.classes.all()) {
            applicationClass.uncompile();
        }
        pathHash = computePathHash();
        try {
            CodeSource codeSource = new CodeSource(new URL("file:" + Play.applicationPath.getAbsolutePath()), (Certificate[]) null);
            Permissions permissions = new Permissions();
            permissions.add(new AllPermission());
            protectionDomain = new ProtectionDomain(codeSource, permissions);
        } catch (MalformedURLException e) {
            throw new UnexpectedException(e);
        }
    }

    /**
     * You know ...
     */
    @Override
    protected synchronized Class loadClass(String name, boolean resolve) throws ClassNotFoundException {

        Class c = findLoadedClass(name);
        if (c != null) {
            return c;
        }
        
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
    }
    
    // ~~~~~~~~~~~~~~~~~~~~~~~
    
    protected Class loadApplicationClass(String name) {
        long start = System.currentTimeMillis();
        ApplicationClass applicationClass = Play.classes.getApplicationClass(name);
        if (applicationClass != null) {
            if (applicationClass.isDefinable()) {
                return applicationClass.javaClass;
            } else {
                byte[] bc = BytecodeCache.getBytecode(name, applicationClass.javaSource);
                if (bc != null) {
                    applicationClass.enhancedByteCode = bc;
                    applicationClass.javaClass = defineClass(applicationClass.name, applicationClass.enhancedByteCode, 0, applicationClass.enhancedByteCode.length, protectionDomain);
                    resolveClass(applicationClass.javaClass);
                    Logger.trace("%sms to load class %s from cache", System.currentTimeMillis() - start, name);
                    return applicationClass.javaClass;
                }
                if (applicationClass.javaByteCode != null || applicationClass.compile() != null) {
                    applicationClass.enhance();
                    applicationClass.javaClass = defineClass(applicationClass.name, applicationClass.enhancedByteCode, 0, applicationClass.enhancedByteCode.length, protectionDomain);
                    BytecodeCache.cacheBytecode(applicationClass.enhancedByteCode, name, applicationClass.javaSource);
                    resolveClass(applicationClass.javaClass);
                    Logger.trace("%sms to load class %s", System.currentTimeMillis() - start, name);
                    return applicationClass.javaClass;
                } else {
                    Play.classes.classes.remove(name);
                }
            }
        }
        return null;
    }

    /**
     * Search for the byte code of the given class.
     */
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
     * You know ...
     */
    @Override
    public InputStream getResourceAsStream(String name) {
        for(VirtualFile vf : Play.javaPath) {
            VirtualFile res = vf.child(name);
            if(res != null && res.exists()) {
                return res.inputstream();
            }
        }
        return super.getResourceAsStream(name);
    }

    /**
     * You know ...
     */
    @Override
    public URL getResource(String name) {
        for(VirtualFile vf : Play.javaPath) {
            VirtualFile res = vf.child(name);
            if(res != null && res.exists()) {
                try {
                    return res.getRealFile().toURI().toURL();
                } catch (MalformedURLException ex) {
                    throw new UnexpectedException(ex);
                }
            }
        }
        return super.getResource(name);
    }

    /**
     * You know ...
     */
    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        List<URL> urls = new ArrayList<URL>();
        for(VirtualFile vf : Play.javaPath) {
            VirtualFile res = vf.child(name);
            if(res != null && res.exists()) {
                try {
                    urls.add(res.getRealFile().toURI().toURL());
                } catch (MalformedURLException ex) {
                    throw new UnexpectedException(ex);
                }
            }
        }
        Enumeration<URL> parent = super.getResources(name);
        while(parent.hasMoreElements()) {
            urls.add(parent.nextElement());
        }
        final Iterator<URL> it = urls.iterator();
        return new Enumeration<URL>() {

            public boolean hasMoreElements() {
                return it.hasNext();
            }

            public URL nextElement() {
                return it.next();
            }
        };
    }

    /**
     * Detect Java changes
     */
    public void detectChanges() {
        // Now check for file modification
        List<ApplicationClass> modifieds = new ArrayList<ApplicationClass>();
        for (ApplicationClass applicationClass : Play.classes.all()) {
            if (applicationClass.timestamp < applicationClass.javaFile.lastModified()) {
                applicationClass.refresh();
                modifieds.add(applicationClass);
            }
        }
        List<ClassDefinition> newDefinitions = new ArrayList<ClassDefinition>();
        boolean dirtySig = false;
        for (ApplicationClass applicationClass : modifieds) {
            if (applicationClass.compile() == null) {
                Play.classes.classes.remove(applicationClass.name);
            } else {
                int sigChecksum = applicationClass.sigChecksum;
                applicationClass.enhance();
                if (sigChecksum != applicationClass.sigChecksum) {
                    dirtySig = true;
                }
                BytecodeCache.cacheBytecode(applicationClass.enhancedByteCode, applicationClass.name, applicationClass.javaSource);
                newDefinitions.add(new ClassDefinition(applicationClass.javaClass, applicationClass.enhancedByteCode));
            }
        }
        if(newDefinitions.size() > 0) {
            Cache.clear();
            if(HotswapAgent.enabled) {
                try {
                    HotswapAgent.reload(newDefinitions.toArray(new ClassDefinition[newDefinitions.size()]));
                } catch (ClassNotFoundException e) {
                    throw new UnexpectedException(e);
                } catch (UnmodifiableClassException e) {
                    throw new UnexpectedException(e);
                }
            } else {
                throw new RuntimeException("Need reload");
            }
        }
        // Check signature (variable name & annotations aware !)
        if (dirtySig) {
            throw new RuntimeException("Signature change !");
        }
        // Now check if there is new classes or removed classes
        int hash = computePathHash();
        if (hash != this.pathHash) {
            // Remove class for deleted files !!
            for (ApplicationClass applicationClass : Play.classes.all()) {
                if (!applicationClass.javaFile.exists()) {
                    Play.classes.classes.remove(applicationClass.name);
                }
                if (applicationClass.name.contains("$")) {
                    Play.classes.classes.remove(applicationClass.name);
                    // Ok we have to remove all classes from the same file ...
                    VirtualFile vf = applicationClass.javaFile;
                    for(ApplicationClass ac : Play.classes.all()) {
                        if(ac.javaFile.equals(vf)) {
                            Play.classes.classes.remove(ac.name);
                        }
                    }
                }
            }
            throw new RuntimeException("Path has changed");
        }
    }

    /**
     * Used to track change of the application sources path
     */
    int pathHash = 0;

    int computePathHash() {
        StringBuffer buf = new StringBuffer();
        for (VirtualFile virtualFile : Play.javaPath) {
            scan(buf, virtualFile);
        }
        return buf.toString().hashCode();
    }

    void scan(StringBuffer buf, VirtualFile current) {
        if (!current.isDirectory()) {
            if (current.getName().endsWith(".java")) {
                Matcher matcher = Pattern.compile("\\s+class\\s([a-zA-Z0-9_]+)\\s+").matcher(current.contentAsString());
                buf.append(current.getName());
                buf.append("(");
                while (matcher.find()) {
                    buf.append(matcher.group(1));
                    buf.append(",");
                }
                buf.append(")");
            }
        } else if (!current.getName().startsWith(".")) {
            for (VirtualFile virtualFile : current.list()) {
                scan(buf, virtualFile);
            }
        }
    }

    /**
     * Try to load all .java files found.
     * @return The list of well defined Class
     */
    public List<Class> getAllClasses() {
        if (allClasses == null) {
            allClasses = new ArrayList<Class>();
            List<ApplicationClass> all = new ArrayList<ApplicationClass>();
            
            // Let's plugins play
            for(PlayPlugin plugin : Play.plugins) {
                plugin.compileAll(all);
            }
            
            for (VirtualFile virtualFile : Play.javaPath) {
                all.addAll(getAllClasses(virtualFile));
            }
            List<String> classNames = new ArrayList();
            for (int i = 0; i < all.size(); i++) {
                if(all.get(i) != null && !all.get(i).compiled) {
                    classNames.add(all.get(i).name);
                }
            }
            Play.classes.compiler.compile(classNames.toArray(new String[classNames.size()]));
            for (ApplicationClass applicationClass : Play.classes.all()) {
                Class clazz = loadApplicationClass(applicationClass.name);
                if (clazz != null) {
                    allClasses.add(clazz);
                }
            }
            Collections.sort(allClasses, new Comparator<Class>() {
                public int compare(Class o1, Class o2) {
                    return o1.getName().compareTo(o2.getName());
                }
            });
        }
        return allClasses;
    }
    List<Class> allClasses = null;

    /**
     * Retrieve all application classes assignable to this class.
     * @param clazz The superclass, or the interface.
     * @return A list of class
     */
    public List<Class> getAssignableClasses(Class clazz) {
        getAllClasses();
        List<Class> results = new ArrayList<Class>();
        for (ApplicationClass c : Play.classes.getAssignableClasses(clazz)) {
            results.add(c.javaClass);
        }
        return results;
    }

    /**
     * Find a class in a case insensitive way
     * @param nale The class name.
     * @return A class
     */
    public Class getClassIgnoreCase(String name) {
        getAllClasses();
        for (ApplicationClass c : Play.classes.all()) {
            if (c.name.equalsIgnoreCase(name)) {
                return loadApplicationClass(c.name);
            }
        }
        return null;
    }

    /**
     * Retrieve all application classes with a specific annotation.
     * @param clazz The annotation class.
     * @return A list of class
     */
    public List<Class> getAnnotatedClasses(Class clazz) {
        getAllClasses();
        List<Class> results = new ArrayList<Class>();
        for (ApplicationClass c : Play.classes.getAnnotatedClasses(clazz)) {
            results.add(c.javaClass);
        }
        return results;
    }
    
    public List<Class> getAnnotatedClasses(Class[] clazz) {
        List<Class> results = new ArrayList<Class>();
        for(Class cl : clazz) {
            results.addAll(getAnnotatedClasses(cl));
        }
        return results;
    }
    
    // ~~~ Intern
    
    List<ApplicationClass> getAllClasses(String basePackage) {
        List<ApplicationClass> res = new ArrayList<ApplicationClass>();
        for (VirtualFile virtualFile : Play.javaPath) {
            res.addAll(getAllClasses(virtualFile, basePackage));
        }
        return res;
    }

    List<ApplicationClass> getAllClasses(VirtualFile path) {
        return getAllClasses(path, "");
    }

    List<ApplicationClass> getAllClasses(VirtualFile path, String basePackage) {
        if (basePackage.length() > 0 && !basePackage.endsWith(".")) {
            basePackage += ".";
        }
        List<ApplicationClass> res = new ArrayList<ApplicationClass>();
        for (VirtualFile virtualFile : path.list()) {
            scan(res, basePackage, virtualFile);
        }
        return res;
    }

    void scan(List<ApplicationClass> classes, String packageName, VirtualFile current) {
        if (!current.isDirectory()) {
            if (current.getName().endsWith(".java") && !current.getName().startsWith(".")) {
                String classname = packageName + current.getName().substring(0, current.getName().length() - 5);
                classes.add(Play.classes.getApplicationClass(classname));
            }
        } else {
            for (VirtualFile virtualFile : current.list()) {
                scan(classes, packageName + current.getName() + ".", virtualFile);
            }
        }
    }

    @Override
    public String toString() {
        return "(play) " + (allClasses == null ? "" : allClasses.toString());
    }
    
    
}
