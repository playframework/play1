package models;

import play.data.binding.As;
import play.data.binding.NoBinding;
import play.db.jpa.Model;

import java.util.Date;

import javax.persistence.Entity;

@Entity
public class Person extends Model{

    public String userName;
    
    public String password;
    
    @As("dd/MM/yyyy")
    public Date creationDate;
    
    @NoBinding("secure")
    public String role;

}
