package play.libs;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import play.exceptions.UnexpectedException;

import java.io.*;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import static org.apache.commons.io.FileUtils.copyInputStreamToFile;

/**
 * Files utils
 */
public class Files {

    /**
     * Characters that are invalid in Windows OS file names (Unix only forbids '/' character)
     */
    public static final char[] ILLEGAL_FILENAME_CHARS = {34, 60, 62, 124, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13,
            14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 58, 42, 63, 92, 47};

    public static final char ILLEGAL_FILENAME_CHARS_REPLACE = '_';

    /**
     * Just copy a file
     * @param from
     * @param to
     */
    public static void copy(File from, File to) {
        if (from.getAbsolutePath().equals(to.getAbsolutePath())) {
            return;
        }

        try {
            FileUtils.copyFile(from, to);
        } catch (Exception e) {
            throw new RuntimeException(e);
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
            try (ZipFile zipFile = new ZipFile(from)) {
                Enumeration<? extends ZipEntry> entries = zipFile.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    if (entry.isDirectory()) {
                        new File(to, entry.getName()).mkdir();
                        continue;
                    }
                    File f = new File(to, entry.getName());
                    if (!f.getCanonicalPath().startsWith(outDir)) {
                        throw new IOException("Corrupted zip file");
                    }
                    f.getParentFile().mkdirs();
                    copyInputStreamToFile(zipFile.getInputStream(entry), f);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void zip(File directory, File zipFile) {
        try {
            try (FileOutputStream os = new FileOutputStream(zipFile)) {
                try (ZipOutputStream zos = new ZipOutputStream(os)) {
                    zipDirectory(directory, directory, zos);
                }
            }
        } catch (Exception e) {
            throw new UnexpectedException(e);
        }
    }

    /**
     * Replace all characters that are invalid in file names on Windows or Unix operating systems
     * with {@link Files#ILLEGAL_FILENAME_CHARS_REPLACE} character.
     * <p>
     * This method makes sure your file name can successfully be used to write new file to disk.
     * Invalid characters are listed in {@link Files#ILLEGAL_FILENAME_CHARS} array.
     *
     * @param fileName File name to sanitize
     * @return Sanitized file name (new String object) if found invalid characters or same string if not
     */
    public static String sanitizeFileName(String fileName) {
        return sanitizeFileName(fileName, ILLEGAL_FILENAME_CHARS_REPLACE);
    }

    /**
     * Replace all characters that are invalid in file names on Windows or Unix operating systems
     * with passed in character.
     * <p>
     * This method makes sure your file name can successfully be used to write new file to disk.
     * Invalid characters are listed in {@link Files#ILLEGAL_FILENAME_CHARS} array.
     *
     * @param fileName File name to sanitize
     * @param replacement character to use as replacement for invalid chars
     * @return Sanitized file name (new String object) if found invalid characters or same string if not
     */
    public static String sanitizeFileName(String fileName, char replacement) {
        if (fileName == null || fileName.isEmpty()) {
            return fileName;
        }

        char[] chars = fileName.toCharArray();
        boolean changed = false;
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            if (c < 128) {
                for (char illegal : ILLEGAL_FILENAME_CHARS) {
                    if (c == illegal) {
                        chars[i] = replacement;
                        changed = true;
                        break;
                    }
                }
            }
        }

        return changed ? new String(chars) : fileName;
    }

    static void zipDirectory(File root, File directory, ZipOutputStream zos) throws Exception {
        for (File item : directory.listFiles()) {
            if (item.isDirectory()) {
                zipDirectory(root, item, zos);
            } else {
                try (FileInputStream fis = new FileInputStream(item)) {
                    String path = item.getAbsolutePath().substring(root.getAbsolutePath().length() + 1);
                    ZipEntry anEntry = new ZipEntry(path);
                    zos.putNextEntry(anEntry);
                    IOUtils.copyLarge(fis, zos);
                }
            }
        }
    }
}
