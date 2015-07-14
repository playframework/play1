package play.data;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.io.FilenameUtils;
import play.data.parsing.TempFilePlugin;
import play.exceptions.UnexpectedException;
import play.libs.Files;
import play.libs.IO;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class FileUpload implements Upload {

    FileItem fileItem;
    File defaultFile;

    public FileUpload() {
        // Left empty
    }

    public FileUpload(FileItem fileItem) {
        this.fileItem = fileItem;
        File tmp = TempFilePlugin.createTempFolder();
        // Check that the file has a name to avoid to override the field folder
        if (fileItem.getName().trim().length() > 0) {
            defaultFile = new File(tmp, FilenameUtils.getName(fileItem.getFieldName()) + File.separator
                    + FilenameUtils.getName(fileItem.getName()));
            try {
                if (!defaultFile.getCanonicalPath().startsWith(tmp.getCanonicalPath())) {
                    throw new IOException("Temp file try to override existing file?");
                }
                defaultFile.getParentFile().mkdirs();
                fileItem.write(defaultFile);
            } catch (Exception e) {
                throw new IllegalStateException("Error when trying to write to file " + defaultFile.getAbsolutePath(), e);
            }
        }
    }

    @Override
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

    @Override
    public byte[] asBytes() {
        return IO.readContent(defaultFile);
    }

    @Override
    public InputStream asStream() {
        try {
            return new FileInputStream(defaultFile);
        } catch (IOException ex) {
            throw new UnexpectedException(ex);
        }
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
        return defaultFile == null ? null : defaultFile.length();
    }

    @Override
    public boolean isInMemory() {
        return fileItem.isInMemory();
    }
}
