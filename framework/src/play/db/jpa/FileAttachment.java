package play.db.jpa;

import java.io.File;
import javax.persistence.Embeddable;
import javax.persistence.Transient;
import play.Play;

@Embeddable
public class FileAttachment {
    
    @Transient
    Object model;
    
    @Transient
    String name;
    
    @Transient
    File f;
    
    public String filename;

    public FileAttachment() {
    }

    FileAttachment(Object model, String name) {
        this.model = model;
        this.name = name;
    }
    
    public File get() {
        return null;
    }
    
    public void set(File file) {
        f = file;
    }
    
    void save() {
        
    }

    void delete() {
        
    }
    
    public boolean isSet() {
        return f != null || get() != null;
    }
    
    public static File getStore() {
        String name = Play.configuration.getProperty("attachments.path", "attachments");
        File store = null;
        if(new File(name).isAbsolute()) {
            store = new File(name);
        } else {
            store = Play.getFile(name);
        }
        if(!store.exists()) {
            store.mkdirs();
        }
        return store;
    }
    
}
