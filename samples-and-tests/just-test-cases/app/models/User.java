package models;

import play.*;
import play.db.jpa.*;
import play.data.validation.*;
import javax.persistence.*;
import java.util.*;
import play.data.binding.annotations.Bind;

@Entity
public class User extends Model {
	
	public User(String name) {
		this.name = name;
	}
	
	public User() {
	}
    
    public String name;
    public Boolean b;
    public boolean c;
    public Integer i;
    public int j;
    public long l;
    public Long k;

	@Required
        @Bind(format = "dd/MM/yyyy")
    public Date birth;

	public String toString() {
		return name;
	}
	
	public static String yip() {
		return "YIP";
	}
    
}

