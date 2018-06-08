package play.classloading;

import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableMap;
import static org.apache.commons.io.IOUtils.closeQuietly;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.instrument.ClassDefinition;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import play.Logger;
import play.Play;
import play.cache.Cache;
import play.classloading.ApplicationClasses.ApplicationClass;
import play.classloading.hash.ClassStateHashCreator;
import play.exceptions.RestartNeededException;
import play.exceptions.UnexpectedException;
import play.libs.IO;
import play.vfs.VirtualFile;

/**
 * The application classLoader. Load the classes from the application Java sources files.
 */
public class ApplicationClassloader extends ClassLoader {

    private final ClassStateHashCreator classStateHashCreator = new ClassStateHashCreator();

    /**
     * A representation of the current state of the ApplicationClassloader. It gets a new value each time the state of
     * the classloader changes.
     */
    public ApplicationClassloaderState currentState = new ApplicationClassloaderState();

    /**
     * This protection domain applies to all loaded classes.
     */
    public ProtectionDomain protectionDomain;

    private final Object lock = new Object();

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

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        // Look up our cache
        Class<?> c = findLoadedClass(name);
        if (c != null) {
            return c;
        }

        synchronized (lock) {
            // First check if it's an application Class
            Class<?> applicationClass = loadApplicationClass(name);
            if (applicationClass != null) {
                if (resolve) {
                    resolveClass(applicationClass);
                }
                return applicationClass;
            }
        }
        // Delegate to the classic classloader
        return super.loadClass(name, resolve);
    }

    public Class<?> loadApplicationClass(String name) {

        if (ApplicationClass.isClass(name)) {
            Class maybeAlreadyLoaded = findLoadedClass(name);
            if (maybeAlreadyLoaded != null) {
                return maybeAlreadyLoaded;
            }
        }

        if (Play.usePrecompiled) {
            try {
                File file = Play.getFile("precompiled/java/" + name.replace(".", "/") + ".class");
                if (!file.exists()) {
                    return null;
                }
                byte[] code = IO.readContent(file);
                Class<?> clazz = findLoadedClass(name);
                if (clazz == null) {
                    if (name.endsWith("package-info")) {
                        definePackage(getPackageName(name), null, null, null, null, null, null, null);
                    } else {
                        loadPackage(name);
                    }
                    clazz = defineClass(name, code, 0, code.length, protectionDomain);
                }
                ApplicationClass applicationClass = Play.classes.getApplicationClass(name);
                if (applicationClass != null) {
                    applicationClass.javaClass = clazz;
                    if (!applicationClass.isClass()) {
                        applicationClass.javaPackage = applicationClass.javaClass.getPackage();
                    }
                }
                return clazz;
            } catch (Exception e) {
                throw new RuntimeException("Cannot find precompiled class file for " + name, e);
            }
        }

        long start = System.currentTimeMillis();
        ApplicationClass applicationClass = Play.classes.getApplicationClass(name);
        if (applicationClass != null) {
            if (applicationClass.isDefinable()) {
                return applicationClass.javaClass;
            }
            byte[] bc = BytecodeCache.getBytecode(name, applicationClass.javaSource);

            if (Logger.isTraceEnabled()) {
                Logger.trace("Compiling code for %s", name);
            }

            if (!applicationClass.isClass()) {
                definePackage(applicationClass.getPackage(), null, null, null, null, null, null, null);
            } else {
                loadPackage(name);
            }
            if (bc != null) {
                applicationClass.enhancedByteCode = bc;
                applicationClass.javaClass = defineClass(applicationClass.name, applicationClass.enhancedByteCode, 0,
                        applicationClass.enhancedByteCode.length, protectionDomain);
                resolveClass(applicationClass.javaClass);
                if (!applicationClass.isClass()) {
                    applicationClass.javaPackage = applicationClass.javaClass.getPackage();
                }

                if (Logger.isTraceEnabled()) {
                    Logger.trace("%sms to load class %s from cache", System.currentTimeMillis() - start, name);
                }

                return applicationClass.javaClass;
            }
            if (applicationClass.javaByteCode != null || applicationClass.compile() != null) {
                applicationClass.enhance();
                applicationClass.javaClass = defineClass(applicationClass.name, applicationClass.enhancedByteCode, 0,
                        applicationClass.enhancedByteCode.length, protectionDomain);
                BytecodeCache.cacheBytecode(applicationClass.enhancedByteCode, name, applicationClass.javaSource);
                resolveClass(applicationClass.javaClass);
                if (!applicationClass.isClass()) {
                    applicationClass.javaPackage = applicationClass.javaClass.getPackage();
                }

                if (Logger.isTraceEnabled()) {
                    Logger.trace("%sms to load class %s", System.currentTimeMillis() - start, name);
                }

                return applicationClass.javaClass;
            }
            Play.classes.classes.remove(name);
        }
        return null;
    }

    private String getPackageName(String name) {
        int dot = name.lastIndexOf('.');
        return dot > -1 ? name.substring(0, dot) : "";
    }

    private void loadPackage(String className) {
        // find the package class name
        int symbol = className.indexOf("$");
        if (symbol > -1) {
            className = className.substring(0, symbol);
        }
        symbol = className.lastIndexOf(".");
        if (symbol > -1) {
            className = className.substring(0, symbol) + ".package-info";
        } else {
            className = "package-info";
        }
        if (this.findLoadedClass(className) == null) {
            this.loadApplicationClass(className);
        }
    }

    /**
     * Search for the byte code of the given class.
     */
    byte[] getClassDefinition(String name) {
        name = name.replace(".", "/") + ".class";
        InputStream is = this.getResourceAsStream(name);
        if (is == null) {
            return null;
        }
        try {
            return IOUtils.toByteArray(is);
        } catch (Exception e) {
            throw new UnexpectedException(e);
        } finally {
            closeQuietly(is);
        }
    }

    @Override
    public InputStream getResourceAsStream(String name) {
        for (VirtualFile vf : Play.javaPath) {
            VirtualFile res = vf.child(name);
            if (res != null && res.exists()) {
                return res.inputstream();
            }
        }
        URL url = getResource(name);

        return super.getResourceAsStream(name);
    }

    @Override
    public URL getResource(String name) {
        URL url = null;
        for (VirtualFile vf : Play.javaPath) {
            VirtualFile res = vf.child(name);
            if (res != null && res.exists()) {
                try {
                    url = res.getRealFile().toURI().toURL();
                    break;
                } catch (MalformedURLException ex) {
                    throw new UnexpectedException(ex);
                }
            }
        }
        if (url == null) {
            url = super.getResource(name);
            if (url != null) {
                try {
                    File file = new File(url.toURI());
                    String fileName = file.getCanonicalFile().getName();
                    if (!name.endsWith(fileName)) {
                        url = null;
                    }
                } catch (Exception ignore) {
                }
            }
        }
        return url;
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        List<URL> urls = new ArrayList<>();
        for (VirtualFile vf : Play.javaPath) {
            VirtualFile res = vf.child(name);
            if (res != null && res.exists()) {
                try {
                    urls.add(res.getRealFile().toURI().toURL());
                } catch (MalformedURLException ex) {
                    throw new UnexpectedException(ex);
                }
            }
        }
        Enumeration<URL> parent = super.getResources(name);
        while (parent.hasMoreElements()) {
            URL next = parent.nextElement();
            if (!urls.contains(next)) {
                urls.add(next);
            }
        }
        final Iterator<URL> it = urls.iterator();
        return new Enumeration<URL>() {

            @Override
            public boolean hasMoreElements() {
                return it.hasNext();
            }

            @Override
            public URL nextElement() {
                return it.next();
            }
        };
    }

    /**
     * Detect Java changes
     * 
     * @throws play.exceptions.RestartNeededException
     *             Thrown if the application need to be restarted
     */
    public void detectChanges() throws RestartNeededException {
        // Now check for file modification
        List<ApplicationClass> modifieds = new ArrayList<>();
        for (ApplicationClass applicationClass : Play.classes.all()) {
            if (applicationClass.timestamp < applicationClass.javaFile.lastModified()) {
                applicationClass.refresh();
                modifieds.add(applicationClass);
            }
        }
        Set<ApplicationClass> modifiedWithDependencies = new HashSet<>();
        modifiedWithDependencies.addAll(modifieds);
        if (!modifieds.isEmpty()) {
            modifiedWithDependencies.addAll(Play.pluginCollection.onClassesChange(modifieds));
        }
        List<ClassDefinition> newDefinitions = new ArrayList<>();
        boolean dirtySig = false;
        for (ApplicationClass applicationClass : modifiedWithDependencies) {
            if (applicationClass.compile() == null) {
                Play.classes.classes.remove(applicationClass.name);
                currentState = new ApplicationClassloaderState();// show others that we have changed..
            } else {
                int sigChecksum = applicationClass.sigChecksum;
                applicationClass.enhance();
                if (sigChecksum != applicationClass.sigChecksum) {
                    dirtySig = true;
                }
                BytecodeCache.cacheBytecode(applicationClass.enhancedByteCode, applicationClass.name, applicationClass.javaSource);
                newDefinitions.add(new ClassDefinition(applicationClass.javaClass, applicationClass.enhancedByteCode));
                currentState = new ApplicationClassloaderState();// show others that we have changed..
            }
        }

        if (!newDefinitions.isEmpty()) {
            Cache.clear();
            if (HotswapAgent.enabled) {
                try {
                    HotswapAgent.reload(newDefinitions.toArray(new ClassDefinition[newDefinitions.size()]));
                } catch (Throwable e) {
                    throw new RestartNeededException(newDefinitions.size() + " classes changed", e);
                }
            } else {
                throw new RestartNeededException(newDefinitions.size() + " classes changed (and HotSwap is not enabled)");
            }
        }
        // Check signature (variable name & annotations aware !)
        if (dirtySig) {
            throw new RestartNeededException("Signature change !");
        }

        // Now check if there is new classes or removed classes
        int hash = computePathHash();
        if (hash != this.pathHash) {
            // Remove class for deleted files !!
            for (ApplicationClass applicationClass : Play.classes.all()) {
                if (!applicationClass.javaFile.exists()) {
                    Play.classes.classes.remove(applicationClass.name);
                    currentState = new ApplicationClassloaderState();// show others that we have changed..
                }
                if (applicationClass.name.contains("$")) {
                    Play.classes.classes.remove(applicationClass.name);
                    currentState = new ApplicationClassloaderState();// show others that we have changed..
                    // Ok we have to remove all classes from the same file ...
                    VirtualFile vf = applicationClass.javaFile;
                    for (ApplicationClass ac : Play.classes.all()) {
                        if (ac.javaFile.equals(vf)) {
                            Play.classes.classes.remove(ac.name);
                        }
                    }
                }
            }
            throw new RestartNeededException("Path has changed");
        }
    }

    /**
     * Used to track change of the application sources path
     */
    private int pathHash = 0;

    private int computePathHash() {
        return classStateHashCreator.computePathHash(Play.javaPath);
    }

    /**
     * Try to load all .java files found.
     * 
     * @return The list of well defined Class
     */
    public List<Class> getAllClasses() {
        if (allClasses == null) {
            List<Class> result = new ArrayList<>();

            if (Play.usePrecompiled) {

                List<ApplicationClass> applicationClasses = new ArrayList<>();
                scanPrecompiled(applicationClasses, "", Play.getVirtualFile("precompiled/java"));
                Play.classes.clear();
                for (ApplicationClass applicationClass : applicationClasses) {
                    Play.classes.add(applicationClass);
                    Class clazz = loadApplicationClass(applicationClass.name);
                    applicationClass.javaClass = clazz;
                    applicationClass.compiled = true;
                    result.add(clazz);
                }

            } else {

                if (!Play.pluginCollection.compileSources()) {

                    List<ApplicationClass> all = new ArrayList<>();

                    for (VirtualFile virtualFile : Play.javaPath) {
                        all.addAll(getAllClasses(virtualFile));
                    }
                    List<String> classNames = new ArrayList<>();
                    for (ApplicationClass applicationClass : all) {
                        if (applicationClass != null && !applicationClass.compiled && applicationClass.isClass()) {
                            classNames.add(applicationClass.name);
                        }
                    }

                    Play.classes.compiler.compile(classNames.toArray(new String[classNames.size()]));

                }

                for (ApplicationClass applicationClass : Play.classes.all()) {
                    Class clazz = loadApplicationClass(applicationClass.name);
                    if (clazz != null) {
                        result.add(clazz);
                    }
                }

                Collections.sort(result, new Comparator<Class>() {

                    @Override
                    public int compare(Class o1, Class o2) {
                        return o1.getName().compareTo(o2.getName());
                    }
                });
            }

            Map<String, ApplicationClass> byNormalizedName = new HashMap<>(result.size());
            for (ApplicationClass clazz : Play.classes.all()) {
                byNormalizedName.put(clazz.name.toLowerCase(), clazz);
                if (clazz.name.contains("$")) {
                    byNormalizedName.put(StringUtils.replace(clazz.name.toLowerCase(), "$", "."), clazz);
                }
            }

            allClassesByNormalizedName = unmodifiableMap(byNormalizedName);
            allClasses = unmodifiableList(result);
        }
        return allClasses;
    }

    private List<Class> allClasses;
    private Map<String, ApplicationClass> allClassesByNormalizedName;

    /**
     * Retrieve all application classes assignable to this class.
     * 
     * @param clazz
     *            The superclass, or the interface.
     * @return A list of class
     */
    public List<Class> getAssignableClasses(Class clazz) {
        if (clazz == null) {
            return Collections.emptyList();
        }
        getAllClasses();
        List<Class> results = assignableClassesByName.get(clazz.getName());
        if (results != null) {
            return results;
        } else {
            results = new ArrayList<>();
            for (ApplicationClass c : Play.classes.getAssignableClasses(clazz)) {
                results.add(c.javaClass);
            }
            // cache assignable classes
            assignableClassesByName.put(clazz.getName(), unmodifiableList(results));
        }
        return results;
    }

    // assignable classes cache
    private final Map<String, List<Class>> assignableClassesByName = new HashMap<>(100);

    /**
     * Find a class in a case insensitive way
     * 
     * @param name
     *            The class name.
     * @return a class
     */
    public Class getClassIgnoreCase(String name) {
        getAllClasses();
        String nameLowerCased = name.toLowerCase();
        ApplicationClass c = allClassesByNormalizedName.get(nameLowerCased);
        if (c != null) {
            if (Play.usePrecompiled) {
                return c.javaClass;
            }
            return loadApplicationClass(c.name);
        }
        return null;
    }

    /**
     * Retrieve all application classes with a specific annotation.
     * 
     * @param clazz
     *            The annotation class.
     * @return A list of class
     */
    public List<Class> getAnnotatedClasses(Class<? extends Annotation> clazz) {
        getAllClasses();
        List<Class> results = new ArrayList<>();
        for (ApplicationClass c : Play.classes.getAnnotatedClasses(clazz)) {
            results.add(c.javaClass);
        }
        return results;
    }

    public List<Class> getAnnotatedClasses(Class[] clazz) {
        List<Class> results = new ArrayList<>();
        for (Class<? extends Annotation> cl : clazz) {
            results.addAll(getAnnotatedClasses(cl));
        }
        return results;
    }

    private List<ApplicationClass> getAllClasses(VirtualFile path) {
        return getAllClasses(path, "");
    }

    private List<ApplicationClass> getAllClasses(VirtualFile path, String basePackage) {
        if (basePackage.length() > 0 && !basePackage.endsWith(".")) {
            basePackage += ".";
        }
        List<ApplicationClass> res = new ArrayList<>();
        for (VirtualFile virtualFile : path.list()) {
            scan(res, basePackage, virtualFile);
        }
        return res;
    }

    private void scan(List<ApplicationClass> classes, String packageName, VirtualFile current) {
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

    private void scanPrecompiled(List<ApplicationClass> classes, String packageName, VirtualFile current) {
        if (!current.isDirectory()) {
            if (current.getName().endsWith(".class") && !current.getName().startsWith(".")) {
                String classname = packageName.substring(5) + current.getName().substring(0, current.getName().length() - 6);
                classes.add(new ApplicationClass(classname));
            }
        } else {
            for (VirtualFile virtualFile : current.list()) {
                scanPrecompiled(classes, packageName + current.getName() + ".", virtualFile);
            }
        }
    }
}
