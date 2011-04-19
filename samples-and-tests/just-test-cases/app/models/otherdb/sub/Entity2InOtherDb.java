package models.otherdb.sub;

import play.*;
import play.db.jpa.*;
import play.data.validation.*;

import javax.persistence.*;
import java.util.*;


// This class should get its @PersistenceUnit-annotations from package-info.java in same folder
@Entity
public class Entity2InOtherDb extends Model {

    public String name;

}
