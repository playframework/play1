package play.db.jpa;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import play.Play;
import play.db.Model.BinaryField;
import play.exceptions.UnexpectedException;
import play.libs.Codec;
import play.libs.IO;

public class Blob implements BinaryField {

    private String UUID;
    private String type;
    private File file;

    public Blob() {}

    Blob(String UUID, String type) {
        this.UUID = UUID;
        this.type = type;
    }

    Blob(String coded) {
        int pipeIndex;
        if (coded != null && (pipeIndex = coded.indexOf('|')) != -1) {
            this.UUID = coded.substring(0, pipeIndex);
            this.type = coded.substring(pipeIndex + 1);
        }
    }

    @Override
    public InputStream get() {
        if (exists()) {
            try {
                return new FileInputStream(getFile());
            } catch(Exception e) {
                throw new UnexpectedException(e);
            }
        }
        return null;
    }

    @Override
    public void set(InputStream is, String type) {
        this.UUID = Codec.UUID();
        this.type = type;
        IO.write(is, getFile());
    }

    @Override
    public long length() {
        return getFile().length();
    }

    @Override
    public String type() {
        return type;
    }

    @Override
    public boolean exists() {
        return UUID != null && getFile().exists();
    }

    public File getFile() {
        if(file == null) {
            file = new File(getStore(), UUID);
        }
        return file;
    }
    
    public String getUUID()  {
        return UUID;
    }

    /**
     * @deprecated use {@link BlobType#getUUID(String)} instead
     */
    @Deprecated(forRemoval = true)
    public static String getUUID(String dbValue) {
       return BlobType.getUUID(dbValue);
    }

    public static File getStore() {
        String name = Play.configuration.getProperty("attachments.path", "attachments");
        File store = new File(name);
        if (!store.isAbsolute()) {
            store = Play.getFile(name);
        }
        if (!store.exists()) {
            store.mkdirs();
        }
        return store;
    }
    
}
