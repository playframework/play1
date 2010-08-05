package models;

import play.*;
import play.db.jpa.*;

import javax.persistence.*;
import java.util.*;

@Entity
public class Factory extends GenericModel {
    
    @Id
    @GeneratedValue
    public long number;
    
    public String name;
    
    public String toString() {
        return name;
    }
    
}

