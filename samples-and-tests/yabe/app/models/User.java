package models;
 
import java.util.*;
import javax.persistence.*;
 
import play.db.jpa.*;
 
@Entity
public class User extends Model {
 
    public String email;
    public String password;
    public String fullname;
    
    public User(String email, String password, String fullname) {
        this.email = email;
        this.password = password;
        this.fullname = fullname;
    }
    
    public static User connect(String email, String password) {
        return find("byEmailAndPassword", email, password).first();
    }
 
}