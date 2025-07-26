package org.playframework.models;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@Entity
public class Cat {

  @Id
  @GeneratedValue
  public Long id;

  public String name;

}
