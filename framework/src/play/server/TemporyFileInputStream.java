package play.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class TemporyFileInputStream extends FileInputStream {

    public File file;


    public TemporyFileInputStream(File file) throws IOException {
        super(file);
        this.file = file;
    }

    public void close() throws IOException {
        file.delete();
        super.close();
    }
}
