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

    public String email;
    
    @Required
    public Date birthdate;
     
    public Student(String name, String email, Date birthdate) {
       this.name = name;
       this.birthdate = birthdate;
       this.email = email;
    }
    
}