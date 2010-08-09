package models;

import play.*;
import play.db.jpa.*;

import javax.persistence.*;
import java.util.*;

@Entity
public class Item extends GenericModel {
    
    public static final String PI = "3.14";
    
    @Id
    public String sku;
    
    public String name;
    
    @ManyToOne
    public models.Factory factory;
    
}

