package play.libs;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.apache.commons.io.FileUtils;

/**
 * Files utils
 */
public class Files {

    /**
     * Just copy a file
     * @param from
     * @param to
     */
    public static void copy(File from, File to) {
        if (from.getAbsolutePath().equals(to.getAbsolutePath())) {
            return;
        }
        FileInputStream is = null;
        FileOutputStream os = null;
        try {
            is = new FileInputStream(from);
            os = new FileOutputStream(to);
            int read = -1;
            byte[] buffer = new byte[10000];
            while ((read = is.read(buffer)) > 0) {
                os.write(buffer, 0, read);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            try {
                is.close();
            } catch (Exception e) {
            }
            try {
                os.close();
            } catch (Exception e) {
            }
        }
    }

    /**
     * Just delete a file. If the file is a directory, it's work.
     * @param file The file to delete
     */
    public static boolean delete(File file) {
        if (file.isDirectory()) {
            return deleteDirectory(file);
        } else {
            return file.delete();
        }
    }

    /**
     * Recursively delete a directory.
     */
    public static boolean deleteDirectory(File path) {
        if (path.exists()) {
            File[] files = path.listFiles();
            for (int i = 0; i < files.length; i++) {
                if (files[i].isDirectory()) {
                    deleteDirectory(files[i]);
                } else {
                    files[i].delete();
                }
            }
        }
        return (path.delete());
    }

    public static boolean copyDir(File from, File to) {
        try {
            FileUtils.copyDirectory(from, to);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public static void unzip(File from, File to) {
        try {
            ZipFile zipFile = new ZipFile(from);
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.isDirectory()) {
                    new File(to, entry.getName()).mkdir();
                    continue;
                }
                File f = new File(to, entry.getName());
                f.getParentFile().mkdirs();
                IO.copy(zipFile.getInputStream(entry), new FileOutputStream(f));
            }
            zipFile.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
