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

    public  DataWithCompositeKey() {
        super();
    }

    public  DataWithCompositeKey(String key1, String key2) {
        this.key1 = key1;
        this.key2 = key2;
    }
}