package models;

import javax.persistence.Entity;
import play.db.jpa.JPASupport;

@Entity
public class Client extends JPASupport {
	
	public String name;
	public Integer age;
	
	public String toString() {
		return String.format("%s (=%s)", name, age);  
	}
	
}
