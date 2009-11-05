package models;
 
import java.util.*;
import javax.persistence.*;
 
import play.db.jpa.*;
 
@Entity
public class Juser extends Model {
 
    public String email;
    public String password;
    public String fullname;
    public boolean isAdmin;
    
    public Juser(String email, String password, String fullname) {
        this.email = email;
        this.password = password;
        this.fullname = fullname;
    }
 
}