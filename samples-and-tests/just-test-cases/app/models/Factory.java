package models;

import play.*;
import play.db.jpa.*;
import play.data.validation.*;

import javax.persistence.*;
import java.util.*;

@Entity
public class Factory extends GenericModel {
    
    public static enum Color { 
        RED, GREEN, BLUE; 
        public String toString() { 
            return "Color " + this.name(); 
        } 
    }
    
    @Id
    @GeneratedValue
    public long number;
    
    public String name;

    public Color color;
    
    public String toString() {
        return name;
    }
    
}

