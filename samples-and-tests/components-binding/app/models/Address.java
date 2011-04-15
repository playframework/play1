package models;

import javax.persistence.Entity;

import play.db.jpa.Model;

@Entity
public class Address extends Model {
	  public String streetName;
	  public String cityName;
}
