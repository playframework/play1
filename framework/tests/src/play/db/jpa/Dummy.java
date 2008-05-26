package play.db.jpa;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
public class Dummy {
    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    public long id;
    public String ppt;
}
