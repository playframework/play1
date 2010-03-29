package play.data;

import java.io.InputStream;
import java.io.File;

public interface Upload {

    public byte[] asBytes();
    public InputStream asStream();
    public String getContentType();
    public String getFileName();
    public String getFieldName();
    public Long getSize();
    public boolean isInMemory();
    public File asFile();
    public File asFile(File file);
    public File asFile(String name);
}
