package models;

import jakarta.persistence.Entity;

import play.db.jpa.Model;

@Entity
public class AnotherEntity extends Model {
	public String prop; 
}
