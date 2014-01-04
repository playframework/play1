package models;
 
import java.util.*;
import javax.persistence.*;
 
import play.db.jpa.*;
import play.data.validation.*;
 
@Entity
@PersistenceUnit(name = "teacher")
public class Teacher extends Model {
 
    @Required
    public String name;
    
    public String email;

    @Required
    public String topic;
     
    public Teacher(String name, String email, String topic) {
       this.name = name;
       this.topic = topic;
       this.email = email;
    }
    
}