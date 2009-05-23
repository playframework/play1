package models;

import play.*;
import play.db.jpa.*;
import javax.persistence.*;
import java.util.*;

@Entity
public class SomeText extends JPAModel {
    
    public String text;
    public String lang;
    
    public String toString() {
        return text;
    }
    
}

