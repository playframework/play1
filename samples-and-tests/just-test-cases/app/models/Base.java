package models;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;

import play.db.jpa.Model;

@Entity
public class Base extends Model{
	public String name;
	@ManyToOne
	public Referenced ref;
}
