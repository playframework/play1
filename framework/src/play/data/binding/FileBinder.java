package play.data.binding;

import java.io.File;

/**
 * Bind file form multipart/form-data request.
 */
public class FileBinder implements SupportedType<File> {

    public File bind(String value) {
        File fl = new File(value);
        return fl;
    }
}
