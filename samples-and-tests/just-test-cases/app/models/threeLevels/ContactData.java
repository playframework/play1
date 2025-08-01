package models.threeLevels;

import jakarta.persistence.*;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import play.db.jpa.GenericModel;
import play.db.jpa.Model;

@Entity
public class ContactData extends GenericModel {

  @Id
  public Long id;
  
  @Column
  public String phone;
  
  @ManyToOne(cascade=CascadeType.ALL)
  public Address address;
  
  @Override
  public String toString() {
    return phone + " : " + (address == null ? "" : address.toString());
  }
  
}
