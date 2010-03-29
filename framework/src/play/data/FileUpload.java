package play.data;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.fileupload.FileItem;
import play.data.parsing.TempFilePlugin;
import play.exceptions.UnexpectedException;
import play.libs.Files;
import play.libs.IO;

public class FileUpload extends Upload {

    FileItem fileItem;
    File defaultFile;

    public FileUpload() {
        // Left empty
    }

    public FileUpload(FileItem fileItem) {
        this.fileItem = fileItem;
        defaultFile = new File(TempFilePlugin.createTempFolder(), fileItem.getFieldName() + File.separator + fileItem.getName());
        defaultFile.getParentFile().mkdirs();
        try {
            fileItem.write(defaultFile);
        } catch (Exception e) {
            throw new IllegalStateException("Error when trying to write to file " + defaultFile.getAbsolutePath(), e);
        }
    }

    public File asFile() {
        return defaultFile;
    }
    
    public File asFile(File file) {
        try {
            Files.copy(defaultFile, file);
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
            return IO.readContent(defaultFile);
        } catch (IOException e) {
            throw new UnexpectedException(e);
        }
    }

    public InputStream asStream() {
        try {
            return new FileInputStream(defaultFile);
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
