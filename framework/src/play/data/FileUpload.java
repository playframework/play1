package play.data;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import play.Logger;
import play.data.parsing.TempFilePlugin;
import play.exceptions.UnexpectedException;
import play.libs.Files;
import play.libs.IO;

public class FileUpload implements Upload {

    FileItem fileItem;
    File defaultFile;

    public FileUpload() {
        // Left empty
    }

    public FileUpload(FileItem fileItem) {
        this.fileItem = fileItem;
        File tmp = TempFilePlugin.createTempFolder();
        defaultFile = new File(tmp, FilenameUtils.getName(fileItem.getFieldName()) + File.separator + FilenameUtils.getName(fileItem.getName()));
        try {
            if(!defaultFile.getCanonicalPath().startsWith(tmp.getCanonicalPath())) {
                throw new IOException("Temp file try to override existing file?");
            }
            defaultFile.getParentFile().mkdirs();
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
        return IO.readContent(defaultFile);
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
        return defaultFile.length();
    }
    
    public boolean isInMemory() {
        return fileItem.isInMemory();
    }
}
