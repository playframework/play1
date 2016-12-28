package models;

import javax.persistence.Entity;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;

import play.db.jpa.Model;

/**
 * @author Manuel Bernhardt <bernhardt.manuel@gmail.com>
 */
@Entity
public class ModelWithLifecycleListeners extends Model {

    public transient String transientValue;

    public String value;

    @PrePersist
    public void onCreate() {
        value = transientValue;
    }

    @PreUpdate
    public void onSave() {
        value = transientValue;
    }
}
