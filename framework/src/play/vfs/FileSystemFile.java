package play.vfs;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.Channel;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import play.Play;
import play.exceptions.UnexpectedException;

public class FileSystemFile extends VirtualFile {

    public File realFile;

    public FileSystemFile(File file) {
        realFile = file;
    }

    private FileSystemFile(FileSystemFile parent, String path) {
        if (parent.realFile != null) {
            realFile = new File(parent.realFile, path);
        }
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
        while (f != null && !f.equals(Play.applicationPath) && !f.equals(Play.frameworkPath)) {
            path.add(f.getName());
            f = f.getParentFile();
        }
        Collections.reverse(path);
        StringBuilder builder = new StringBuilder();
        for (String p : path) {
            builder.append("/" + p);
        }
        return builder.toString();
    }

    public List<VirtualFile> list() {
        List<VirtualFile> res = new ArrayList<VirtualFile>();
        if (exists()) {
            File[] children = realFile.listFiles();
            for (int i = 0; i < children.length; i++) {
                res.add(new FileSystemFile(children[i]));
            }
        }
        return res;
    }

    public boolean exists() {
        if (realFile != null) {
            return realFile.exists();
        }
        return false;
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
        if (other instanceof FileSystemFile) {
            FileSystemFile vf = (FileSystemFile) other;
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
        return new FileSystemFile(this, name);
    }

    public Channel channel() {
        try {
            FileInputStream fis = new FileInputStream(realFile);
            FileChannel ch = fis.getChannel();
            return ch;
        } catch (FileNotFoundException e) {
            return null;
        }

    }
}
