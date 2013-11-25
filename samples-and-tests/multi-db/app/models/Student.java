package models;
 
import java.util.*;
import javax.persistence.*;
 
import play.db.jpa.*;
import play.data.validation.*;
 
@Entity
@PersistenceUnit(name = "default")
public class Student extends Model {
 
    @Required
    public String name;
    
    @Required
    public Date birthdate;
     
    public Student(String name, Date birthdate) {
       this.name = name;
       this.birthdate = birthdate;
    }
    
}