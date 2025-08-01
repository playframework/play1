package models;

import jakarta.persistence.Entity;

import play.db.jpa.Model;

@Entity
public class MyBook extends OptimisticLockingModel {
    public String text;
    public String excludedProperty;
    
}
