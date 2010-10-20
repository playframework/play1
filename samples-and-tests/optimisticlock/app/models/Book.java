package models;

import javax.persistence.Entity;

import play.db.jpa.Model;

@Entity
public class Book extends OptimisticLockingModel {
    public String text;
    public String excludedProperty;
    
}
