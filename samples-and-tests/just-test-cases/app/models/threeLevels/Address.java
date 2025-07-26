package models.threeLevels;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import play.db.jpa.GenericModel;
import play.db.jpa.Model;

@Entity(name="tlAddress")
public class Address extends GenericModel {
    
  @Id
  public Long id;
  
  @Column
  public String streetName;
  
  @Override
  public String toString() {
    return streetName;
  }
  
}
