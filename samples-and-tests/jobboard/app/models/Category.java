package models;

import play.*;
import play.db.jpa.*;
import play.data.validation.*;

import javax.persistence.*;
import java.util.*;

@Entity
public class Category extends Model {
	
	@Required 
	public String label;
	
	@Required 
	public String code;
	
	public String toString(){
		return label;
	}
	
}

