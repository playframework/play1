package models;

import java.util.Date;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import play.data.validation.Email;
import play.data.validation.Required;
import play.db.jpa.Model;

@Entity
//@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Contact extends Model {
    
    @Required
    public String firstname;
    
    @Required
    public String name;
    
    @Required
    public Date birthdate;
    
    @Required
    @Email
    public String email;
    
    @OneToMany
    public List<Dept> depts;

    @ManyToOne
    public Dept de2;

//    @Required
//    public String gender2;
//    public String gender33;

    public String toString() {
    	String r = "Contact name: " + name;
    	r += "; Email: " + email;
    	r += "; Birth date: " + birthdate;
    	String s = r + "1";
    	String s2 = s;
		return s2;
    	
    }
}

