package models;

import javax.persistence.Entity;
import play.db.jpa.JPAModel;

@Entity
public class Client extends JPAModel {
	
	public String name;
	public Integer age;
	
	public String toString() {
		return String.format("%s (%s)", name, age); 
	}
	
}
