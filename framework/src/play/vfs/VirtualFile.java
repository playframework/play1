package play.vfs;

import org.apache.commons.io.IOUtils;
import play.Play;
import play.exceptions.UnexpectedException;
import play.libs.IO;

import java.io.*;
import java.nio.channels.Channel;
import java.security.AccessControlException;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.commons.io.IOUtils.closeQuietly;

/**
 * The VFS used by Play!
 */
public class VirtualFile {

    private static class CacheEntry {
        long cachedAt;
        List<VirtualFile> list;
    }

    private static Map<String,CacheEntry> childrenCache = new HashMap<String,CacheEntry>();

    File realFile;

    VirtualFile(File file) {
        this.realFile = file;
    }

    public String getName() {
        return realFile.getName();
    }

    public boolean isDirectory() {
        return realFile.isDirectory();
    }

    public String relativePath() {
        List<String> path = new ArrayList<String>();
        File f = realFile;
        String prefix = "{?}";
        while (true) {
            path.add(f.getName());
            f = f.getParentFile();
            if (f == null) {
                break; // ??
            }
            if (f.equals(Play.frameworkPath)) {
                prefix = "{play}";
                break;
            }
            if (f.equals(Play.applicationPath)) {
                prefix = "";
                break;
            }
            String module = isRoot(f);
            if (module != null) {
                prefix = module;
                break;
            }

        }
        Collections.reverse(path);
        StringBuilder builder = new StringBuilder();
        for (String p : path) {
            builder.append("/" + p);
        }
        return prefix + builder.toString();
    }

    String isRoot(File f) {
        for (VirtualFile vf : Play.roots) {
            if (vf.realFile.getAbsolutePath().equals(f.getAbsolutePath())) {
                String modulePathName = vf.getName();
                String moduleName = modulePathName.contains("-") ? modulePathName.substring(0, modulePathName.lastIndexOf("-")) : modulePathName;
                return "{module:" + moduleName + "}";
            }
        }
        return null;
    }

    public List<VirtualFile> list() {
        if (!exists()) {
            return Collections.emptyList();
        }
        String cacheKey = realFile.getAbsolutePath();
        if (childrenCache.containsKey(cacheKey)) {
            CacheEntry cacheEntry = childrenCache.get(cacheKey);
            if (cacheEntry.cachedAt < realFile.lastModified()) {
                childrenCache.remove(cacheKey);

            } else {
                // cache found
                return cacheEntry.list;
            }
        }
        List<VirtualFile> res = new ArrayList<VirtualFile>();
        File[] children = realFile.listFiles();
        for (int i = 0; i < children.length; i++) {
            res.add(new VirtualFile(children[i]));
        }
        // store to cache
        CacheEntry newCache = new CacheEntry();
        newCache.cachedAt = new Date().getTime();
        newCache.list = res;
        childrenCache.put(cacheKey, newCache);
        return res;
    }

    public boolean exists() {
        try {
            if (realFile != null) {
                return realFile.exists();
            }
            return false;
        } catch (AccessControlException e) {
            return false;
        }
    }

    public InputStream inputstream() {
        try {
            return new FileInputStream(realFile);
        } catch (Exception e) {
            throw new UnexpectedException(e);
        }
    }

    public OutputStream outputstream() {
        try {
            return new FileOutputStream(realFile);
        } catch (Exception e) {
            throw new UnexpectedException(e);
        }
    }

    public Long lastModified() {
        if (realFile != null) {
            return realFile.lastModified();
        }
        return 0L;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof VirtualFile) {
            VirtualFile vf = (VirtualFile) other;
            if (realFile != null && vf.realFile != null) {
                return realFile.equals(vf.realFile);
            }
        }
        return super.equals(other);
    }

    @Override
    public int hashCode() {
        if (realFile != null) {
            return realFile.hashCode();
        }
        return super.hashCode();
    }

    public long length() {
        return realFile.length();
    }

    public VirtualFile child(String name) {
        return new VirtualFile(new File(realFile, name));
    }

    public Channel channel() {
        try {
            FileInputStream fis = new FileInputStream(realFile);
            try {
                return fis.getChannel();
            }
            finally {
                closeQuietly(fis);
            }
        } catch (FileNotFoundException e) {
            return null;
        }
    }

    public static VirtualFile open(String file) {
        return open(new File(file));
    }

    public static VirtualFile open(File file) {
        return new VirtualFile(file);
    }

    public String contentAsString() {
        try {
            InputStream is = inputstream();
            try {
                return IO.readContentAsString(is);
            }
            finally {
                closeQuietly(is);
            }
        } catch (Exception e) {
            throw new UnexpectedException(e);
        }
    }

    public File getRealFile() {
        return realFile;
    }

    public void write(CharSequence string) {
        try {
            IO.writeContent(string, outputstream());
        } catch (Exception e) {
            throw new UnexpectedException(e);
        }
    }

    public byte[] content() {
        try {
            InputStream is = inputstream();
            try {
                return IOUtils.toByteArray(is);
            }
            finally {
                closeQuietly(is);
            }
        } catch (Exception e) {
            throw new UnexpectedException(e);
        }
    }

    @Override
    public String toString() {
        return getName();
    }

    public static VirtualFile search(Collection<VirtualFile> roots, String path) {
        for (VirtualFile file : roots) {
            if (file.child(path).exists()) {
                return file.child(path);
            }
        }
        return null;
    }

    public static VirtualFile fromRelativePath(String relativePath) {
        Pattern pattern = Pattern.compile("^(\\{(.+?)\\})?(.*)$");
        Matcher matcher = pattern.matcher(relativePath);

        if(matcher.matches()) {
            String path = matcher.group(3);
            String module = matcher.group(2);
            if(module == null || module.equals("?") || module.equals("")) {
                return new VirtualFile(Play.applicationPath).child(path);
            } else {
                if(module.equals("play")) {
                    return new VirtualFile(Play.frameworkPath).child(path);
                }
                if(module.startsWith("module:")){
                    module = module.substring("module:".length());
                    for(Entry<String, VirtualFile> entry : Play.modules.entrySet()) {
                        if(entry.getKey().equals(module))
                            return entry.getValue().child(path);
                    }
                }
            }
        }

        return null;
    }

    /**
     * Method to check if the name really match (very useful on system without case sensibility (like windows))
     * @param fileName
     * @return true if match
     */
    public boolean matchName(String fileName) {
        // we need to check the name case to be sure we is not conflict with a file with the same name
        String canonicalName = null; 
        try {
            canonicalName = this.realFile.getCanonicalFile().getName();
        } catch (IOException e) {
        }
        // Name case match
        if (fileName != null && canonicalName != null && fileName.endsWith(canonicalName)) {
            return true;
        }
        return false;
    }
}