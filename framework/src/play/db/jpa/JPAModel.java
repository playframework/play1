package play.db.jpa;

import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.PostLoad;
import javax.persistence.PostPersist;
import javax.persistence.PostUpdate;
import org.hibernate.ejb.EntityManagerImpl;

@MappedSuperclass
public class JPAModel extends JPASupport {

    @Id
    @GeneratedValue
    public Long id;
    
    public Long getId() {
        return id;
    }

    @PostLoad
    public void onLoad() {
        super.setupAttachment();
    }

    @PostPersist
    @PostUpdate
    public void onSave() {        
        super.saveAttachment();
    }
}
