package play.data.binding;

import java.io.File;

/**
 * Bind file form multipart/form-data request.
 */
public class FileBinder implements SupportedType<File> {

    public File bind(String value) {
        if(value == null) return null;
        File fl = new File(value);
        return fl.length() == 0 ? null : fl;
    }
}
