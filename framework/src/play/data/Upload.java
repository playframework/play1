package play.data;

import java.io.InputStream;
import java.io.File;

public abstract class Upload {

    public abstract byte[] asBytes();
    public abstract InputStream asStream();
    public abstract String getContentType();
    public abstract String getFileName();
    public abstract String getFieldName();
    public abstract Long getSize();
    public abstract boolean isInMemory();
    public abstract File asFile();
    public abstract File asFile(File file);
    public abstract File asFile(String name);
    
}
