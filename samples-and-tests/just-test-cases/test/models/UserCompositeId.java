package models;

import play.db.jpa.GenericModel;
import play.db.jpa.Model;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;

@Entity
public class UserCompositeId extends GenericModel {

  @EmbeddedId
  public UserId id;
  public Integer age;

}
