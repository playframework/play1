package models;

import play.*;
import play.db.jpa.*;
import play.data.validation.*;

import javax.persistence.*;
import java.util.*;

@Entity
public class Tag extends JPAModel {

	@Required 
	public String label;

	@Required 
	public String code;
	
	public static List<Tag> findByCategory(String category){
		if (category == null) {
			return Tag.findBy("select distinct t from Tag t, Job j where t member of j.tags");
		}
		return Tag.findBy("select distinct t from Tag t, Job j where j.category.code = ? and t member of j.tags and j.online = true", category);
	}

	public static Tag findByCode(String code){
		return Tag.find("byCode", code).one();
	}

	public String toString() {
		return label;
	}
	
}

