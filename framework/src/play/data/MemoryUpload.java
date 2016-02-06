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

    @Override
    public File asFile() {
        throw new UnsupportedOperationException();
    }

    @Override
    public byte[] asBytes() {
        return fileItem.get();
    }

    @Override
    public InputStream asStream() {
        return new ByteArrayInputStream(fileItem.get());
    }

    @Override
    public String getContentType() {
        return fileItem.getContentType();
    }

    @Override
    public String getFileName() {
        return fileItem.getName();
    }

    @Override
    public String getFieldName() {
        return fileItem.getFieldName();
    }

    @Override
    public Long getSize() {
        return fileItem.getSize();
    }

    @Override
    public boolean isInMemory() {
        return fileItem.isInMemory();
    }

}
