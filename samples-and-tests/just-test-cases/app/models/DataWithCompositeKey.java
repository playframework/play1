package models;

import play.db.jpa.*;
import javax.persistence.*;

@Entity
public class DataWithCompositeKey extends GenericModel {
    
    @Id
    public String key1;
    @Id
    public String key2;
    
    public String name;
}