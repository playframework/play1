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
    
    static class BottleCheck extends Check {
        
        public boolean isSatisfied(Object validatedObject, Object value) {
            Bottle bottle = (Bottle)validatedObject;
            if(bottle == null || bottle.name1 == null || bottle.name2 == null) {
                return true;
            }
            setMessage("wrong.bottle", bottle.name1, bottle.name2);
            return bottle.name2.equals(bottle.name1);
        }
        
    }
    
}

