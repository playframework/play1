package models;

import play.*;
import play.db.jpa.*;
import play.data.validation.*;

import javax.persistence.*;
import java.util.*;

@Entity
public class Bottle extends Model {
    
    @Required
    @CheckWith(BottleCheck.class)
    public String name1;
    
    @MinSize(3)
    @Required
    public String name2;
    
    public Bottle() {
    }
    
    public Bottle(String name1) {
        this.name1 = name1;
    }
    
    public Bottle(String name1, String name2) {
        this.name1 = name1;
        this.name2 = name2;
    }
    
    public String toString() {
        return name1+"-"+name2;
    }
    
    static class BottleCheck extends Check {
        
        public boolean isSatisfied(Object validatedObject, Object value) {
            System.out.println(checkWithCheck.getMessage());
            Bottle bottle = (Bottle)validatedObject;
            if(bottle == null || bottle.name1 == null || bottle.name2 == null) {
                return true;
            }
            setMessage("wrong.bottle", bottle.name1, bottle.name2);
            return bottle.name2.equals(bottle.name1);
        }
        
    }
    
}

