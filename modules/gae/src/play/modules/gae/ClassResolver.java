package play.modules.gae;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import org.datanucleus.ClassLoaderResolver;
import play.Play;

public class ClassResolver implements ClassLoaderResolver {
    
    public ClassResolver() {
    }
    
    public ClassResolver(ClassLoader cl) {
    }

    public Class classForName(String name, ClassLoader cl) {
        try {
            return Play.classloader.loadClass(name);
        } catch(ClassNotFoundException e) {
            return null;
        }
    }

    public Class classForName(String name, ClassLoader cl, boolean initialize) {
        try {
            return Play.classloader.loadClass(name);
        } catch(ClassNotFoundException e) {
            return null;
        }
    }

    public Class classForName(String name) {
        try {
            return Play.classloader.loadClass(name);
        } catch(ClassNotFoundException e) {
            return null;
        }
    }

    public Class classForName(String name, boolean initialize) {
        try {
            return Play.classloader.loadClass(name);
        } catch(ClassNotFoundException e) {
            return null;
        }
    }

    public boolean isAssignableFrom(String arg0, Class arg1) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean isAssignableFrom(Class arg0, String arg1) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean isAssignableFrom(String arg0, String arg1) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void registerClassLoader(ClassLoader cl) {
        //
    }

    public void registerUserClassLoader(ClassLoader cl) {
        //
    }

    public Enumeration getResources(String name, ClassLoader cl) throws IOException {
        return cl.getResources(name);
    }

    public URL getResource(String name, ClassLoader cl) {
        return cl.getResource(name);
    }

    public void setPrimary(ClassLoader cl) {
        //
    }

    public void unsetPrimary() {
        //
    }

}
