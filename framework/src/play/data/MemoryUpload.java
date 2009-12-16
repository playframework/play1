package play.data;

import java.io.File;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import org.apache.commons.fileupload.FileItem;

public class MemoryUpload implements Upload {

    FileItem fileItem;

    public MemoryUpload(FileItem fileItem) {
        this.fileItem = fileItem;
    }

    public File asFile() {
        throw new UnsupportedOperationException();
    }
    
    public byte[] asBytes() {
        return fileItem.get();
    }

    public InputStream asStream() {
        return new ByteArrayInputStream(fileItem.get());
    }

    public String getContentType() {
        return fileItem.getContentType();
    }

    public String getFileName() {
        return fileItem.getName();
    }

    public String getFieldName() {
        return fileItem.getFieldName();
    }

    public Long getSize() {
        return fileItem.getSize();
    }
    
    public boolean isInMemory() {
        return fileItem.isInMemory();
    }

}
