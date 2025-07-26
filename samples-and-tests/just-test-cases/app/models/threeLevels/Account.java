package models.threeLevels;

import jakarta.persistence.*;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import play.db.jpa.GenericModel;
import play.db.jpa.Model;

@Entity
public class Account extends GenericModel {
    
  @Id
  public Long id;

  @Column
  public String name;
  
  @ManyToOne(cascade=CascadeType.ALL)
  public ContactData contactData;
  
  @Override
  public String toString() {
    return name + " : " + (contactData == null ? "" : contactData.toString());  
  }
  
}
