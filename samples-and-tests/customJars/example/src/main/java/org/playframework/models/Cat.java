package org.playframework.models;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity
public class Cat {

  @Id
  @GeneratedValue
  public Long id;

  public String name;

}
