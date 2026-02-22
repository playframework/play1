package play.db.jpa;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

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
