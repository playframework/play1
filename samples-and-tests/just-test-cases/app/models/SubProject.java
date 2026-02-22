package models;

import jakarta.persistence.*;

@Entity
public class SubProject extends Project {
    
    public String subProjectName;
    
    private String subProjectObservation;
    
    public Project parent;
    
    public String getSubProjectObservation() {
    	return this.subProjectObservation;
    }
    
    public void setSubProjectObservation(String subProjectObservation) {
    	this.subProjectObservation = subProjectObservation;
    }
}

