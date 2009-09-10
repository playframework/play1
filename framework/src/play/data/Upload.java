package play.data;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.util.Streams;
import play.data.parsing.TempFilePlugin;
import play.exceptions.UnexpectedException;

public class Upload {

    FileItem fileItem;

    public Upload(FileItem fileItem) {
        this.fileItem = fileItem;
    }

    public File asFile() {
        File file = new File(TempFilePlugin.createTempFolder(), fileItem.getFieldName() + File.separator + fileItem.getName());
        file.getParentFile().mkdirs();
        try {
            fileItem.write(file);
        } catch (Exception e) {
            throw new IllegalStateException("Error when trying to write to file " + file.getAbsolutePath(), e);
        }
        return file;
    }
    
    public File asFile(File file) {
        try {
            fileItem.write(file);
            return file;
        } catch (Exception ex) {
            throw new UnexpectedException(ex);
        }
    }
    
    public File asFile(String name) {
        return asFile(new File(name));
    }

    public byte[] asBytes() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Streams.copy(asStream(), baos, true);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new UnexpectedException(e);
        }
    }

    public InputStream asStream() {
        try {
            return this.fileItem.getInputStream();
        } catch (IOException ex) {
            throw new UnexpectedException(ex);
        }
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
