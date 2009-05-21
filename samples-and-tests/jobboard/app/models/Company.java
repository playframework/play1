package models;

import play.*;
import play.db.jpa.*;
import play.data.validation.*;

import javax.persistence.*;
import java.util.*;

@Entity
public class Company extends JPAModel {
	
	@Required 
	public String name;	
	
	@Email 
	@Required 
	public String email;	
	
	@Required 
	@Password 
	public String password;	
	
	public String website;
	
	@Embedded 
	public FileAttachment logo;
	
	public String toString() {
		return name;
	}
}

