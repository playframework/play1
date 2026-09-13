package play.data;

import java.io.InputStream;
import java.io.File;

public interface Upload {

    byte[] asBytes();
    InputStream asStream();
    String getContentType();
    String getFileName();
    String getFieldName();
    long getSize();
    boolean isInMemory();
    File asFile();
}
