package play.db.jpa;

import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

/**
 * Base class for JPA model objects
 * Automatically provide a @Id Long id field
 */
@MappedSuperclass
public class Model extends GenericModel {

    @Id
    @GeneratedValue
    public Long id;

    public Long getId() {
        return id;
    }

    @Override
    public Object _key() {
        return getId();
    }

}
