package play.db.jpa;

import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.PostLoad;
import javax.persistence.PostPersist;
import javax.persistence.PostUpdate;

@MappedSuperclass
public class JPAModel extends JPASupport {

    @Id
    @GeneratedValue
    public Long id;

    @PostLoad
    @Override
    public void setupAttachment() {
        super.setupAttachment();
    }

    @PostPersist
    @PostUpdate
    @Override
    public void saveAttachment() {
        super.saveAttachment();
    }
}
