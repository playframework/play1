package models;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import play.db.jpa.GenericModel;

@Entity
public class ArrayIdEntity extends GenericModel {
	@Id
	public String[] id = new String[2];
	
	public ArrayIdEntity(String id1, String id2) {
	    id[0] = id1;
	    id[1] = id2;
	}
}
