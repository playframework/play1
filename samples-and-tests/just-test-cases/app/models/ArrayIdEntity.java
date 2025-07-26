package models;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

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
