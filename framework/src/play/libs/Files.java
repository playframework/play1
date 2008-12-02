package play.libs;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

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
            throw new RuntimeException();
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
     * Just delete a file
     * @param file The file to delete
     */
    public static void delete(File file) {
        file.delete();
    }
}
