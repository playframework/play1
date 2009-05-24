package models;

import play.*;
import play.db.jpa.*;
import play.data.validation.*;

import javax.persistence.*;
import java.util.*;

@Entity
public class Project extends JPAModel {
    
    @Required
    public String name;
    
    @Future
    @Past("2020-01-01")
    public Date endDate;
    
    @Past
    @Future("1980-21-12")
    public Date startDate;
    
    @Required
    @ManyToOne
    public Company company;
    
    public String toString() {
        return name + " belongs to " + company;
    }
    
}

