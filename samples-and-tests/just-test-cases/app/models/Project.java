package models;

import play.*;
import play.db.jpa.*;
import play.data.validation.*;

import javax.persistence.*;
import java.util.*;

@Entity
public class Project extends Model {
    
    @Required
    public String name;
    
    @InFuture
    @InPast("2020-01-01")
    public Date endDate;
    
    @InPast
    @InFuture("1980-12-21")
    public Date startDate;
    
    @Required
    @ManyToOne(cascade=CascadeType.PERSIST) 
    public Company company;
    
    @OneToMany(cascade=CascadeType.PERSIST)
    @MapKey(name="name")
    public Map<String,Company> companies;
    
    private String observation;
    
    public String toString() {
        return name + " belongs to " + company;
    }
    
    public Project() {}
    
    public Project(String name) {
        this.name = name;
    }
    
    public String getObservation() {
    	return this.observation;
    }
    
    public void setObservation(String observation) {
    	this.observation = observation;
    }
}

