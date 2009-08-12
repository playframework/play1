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
    @InFuture("1980-21-12")
    public Date startDate;
    
    @Required
    @ManyToOne
    public Company company;
    
    public String toString() {
        return name + " belongs to " + company;
    }
    
}

