package models;

import play.db.jpa.*;
import play.data.validation.*;

import javax.persistence.*;

@Entity
public class Company extends Model {

    @Required
    public String name;
    
    @Email
    @Required
    public String email;
    
    @Required
    @Password
    public String password;
    
    public String website;
    
    public Blob logo;

    public String toString() {
        return name;
    }
    
}

