package play.data.binding;

import java.io.File;

public class FileBinder implements SupportedType<File> {

    public File bind(String value) {
        File fl = new File(value);
        return fl;
    }
}
