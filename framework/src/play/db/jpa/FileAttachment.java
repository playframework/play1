package play.db.jpa;

import java.io.File;
import javax.persistence.Embeddable;
import javax.persistence.Transient;
import play.Play;
import play.libs.Files;

@Embeddable
public class FileAttachment {
    
    @Transient
    JPAModel model;
    
    @Transient
    String name;
    
    @Transient
    File f;
    
    public String filename;

    FileAttachment() {
    }

    FileAttachment(JPAModel model, String name) {
        this.model = model;
        this.name = name;
    }
    
    public File get() {
        File file = new File(getStore(), model.getClass().getName()+"."+name+"_"+model.id);
        if(file.exists()) {
            return file;
        }
        return null;
    }
    
    public void set(File file) {
        f = file;
    }
    
    void save() {
        if(f != null) {
            File to = new File(getStore(), model.getClass().getName()+"."+name+"_"+model.id);
            Files.copy(f, to);
        }
    }
    
    public boolean isSet() {
        return f != null || get() != null;
    }
    
    static File getStore() {
        String name = Play.configuration.getProperty("attachments.path", "attachments");
        File store = Play.getFile(name);
        if(!store.exists()) {
            store.mkdirs();
        }
        return store;
    }
    
}
