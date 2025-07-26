package models;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;

import play.db.jpa.Model;

@Entity
public class Base extends Model{
	public String name;
	@ManyToOne
	public Referenced ref;
}
