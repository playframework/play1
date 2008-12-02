package play.vfs;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.Channel;
import java.util.Collection;
import java.util.List;
import play.exceptions.UnexpectedException;
import play.libs.IO;

/**
 * The VFS used by Play!
 */
public abstract class VirtualFile {

    public abstract String getName();
    public abstract boolean isDirectory();
    public abstract String relativePath();
    public abstract List<VirtualFile> list();
    public abstract boolean exists();
    public abstract InputStream inputstream();
    public abstract OutputStream outputstream();
    public abstract VirtualFile child(String name);
    public abstract Long lastModified();
    public abstract long length();
    public abstract Channel channel();
    
    public static VirtualFile open(String file) {
        return open(new File(file));
    }

    public static VirtualFile open(File file) {
        if (file.isFile() && (file.toString().endsWith(".zip") || file.toString().endsWith(".jar"))) {
            try {
                return new ZFile(file);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        if (file.isDirectory()) {
            return new FileSystemFile(file);
        } else {
            return null;
        }
    }

    public String contentAsString() {
        try {
            return IO.readContentAsString(inputstream());
        } catch (Exception e) {
            throw new UnexpectedException(e);
        }
    }

    public void write(CharSequence string) {
        try {
            IO.writeContent(string, outputstream());
        } catch (Exception e) {
            throw new UnexpectedException(e);
        }
    }

    public byte[] content() {
        byte[] buffer = new byte[(int) length()];
        try {
            InputStream is = inputstream();
            is.read(buffer);
            is.close();
            return buffer;
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
}