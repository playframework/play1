package models.otherdb;

import play.*;
import play.db.jpa.*;
import play.data.validation.*;

import javax.persistence.*;
import java.util.*;


@PersistenceUnit(name="other")
@Entity
public class EntityInOtherDb extends Model {

    public String name;

}
