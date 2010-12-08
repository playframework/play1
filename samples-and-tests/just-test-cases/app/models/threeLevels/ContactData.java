package models.threeLevels;

import javax.persistence.*;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

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
