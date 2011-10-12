package models;

import play.db.jpa.GenericModel;
import play.db.jpa.Model;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;

@Entity
public class UserCompositeId extends GenericModel {

  @EmbeddedId
  public UserId id;
  public Integer age;

}
