package play.libs;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import org.apache.commons.io.FileUtils;
import play.exceptions.UnexpectedException;

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
            int read;
            byte[] buffer = new byte[10000];
            while ((read = is.read(buffer)) > 0) {
                os.write(buffer, 0, read);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            try {
                is.close();
            } catch (Exception ignored) {
            }
            try {
                os.close();
            } catch (Exception ignored) {
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
            for (File file: files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
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
            String outDir = to.getCanonicalPath();
            ZipFile zipFile = new ZipFile(from);
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.isDirectory()) {
                    new File(to, entry.getName()).mkdir();
                    continue;
                }
                File f = new File(to, entry.getName());
                if(!f.getCanonicalPath().startsWith(outDir)) {
                    throw new IOException("Corrupted zip file");
                }
                f.getParentFile().mkdirs();
                FileOutputStream os = new FileOutputStream(f);
                IO.copy(zipFile.getInputStream(entry), os);
                os.close();
            }
            zipFile.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void zip(File directory, File zipFile) {
        try {
            FileOutputStream os = new FileOutputStream(zipFile);
            ZipOutputStream zos = new ZipOutputStream(os);
            zipDirectory(directory, directory, zos);
            zos.close();
            os.close();
        } catch (Exception e) {
            throw new UnexpectedException(e);
        }
    }

    static void zipDirectory(File root, File directory, ZipOutputStream zos) throws Exception {
        for (File item : directory.listFiles()) {
            if (item.isDirectory()) {
                zipDirectory(root, item, zos);
            } else {
                byte[] readBuffer = new byte[2156];
                int bytesIn;
                FileInputStream fis = new FileInputStream(item);
                String path = item.getAbsolutePath().substring(root.getAbsolutePath().length() + 1);
                ZipEntry anEntry = new ZipEntry(path);
                zos.putNextEntry(anEntry);
                while ((bytesIn = fis.read(readBuffer)) != -1) {
                    zos.write(readBuffer, 0, bytesIn);
                }
                fis.close();
            }
        }
    }
}
