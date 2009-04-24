package play.db.jpa;

import java.lang.reflect.Field;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.PostLoad;
import javax.persistence.PostPersist;
import javax.persistence.PostUpdate;
import play.exceptions.UnexpectedException;

@MappedSuperclass
public class JPAModel extends JPASupport {

    @Id
    @GeneratedValue
    public Long id;

    @PostLoad
    public void setupAttachment() {
        for (Field field : getClass().getFields()) {
            if (field.getType().equals(FileAttachment.class)) {
                try {
                    FileAttachment attachment = (FileAttachment)field.get(this);
                    if(attachment != null) {
                        attachment.model = this;
                        attachment.name = field.getName();
                    }
                } catch (Exception ex) {
                    throw new UnexpectedException(ex);
                }
            }
        }
    }

    @PostPersist
    @PostUpdate
    public void saveAttachment() {
        for (Field field : getClass().getFields()) {
            if (field.getType().equals(FileAttachment.class)) {
                try {
                    FileAttachment attachment = (FileAttachment)field.get(this);
                    if(attachment != null) {
                        attachment.save();
                    }
                } catch (Exception ex) {
                    throw new UnexpectedException(ex);
                }
            }
        }
    }
}
