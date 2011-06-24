package models.threeLevels;

import javax.persistence.*;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

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
