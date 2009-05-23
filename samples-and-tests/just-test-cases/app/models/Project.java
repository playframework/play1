package models;

import play.*;
import play.db.jpa.*;
import play.data.validation.*;

import javax.persistence.*;
import java.util.*;

@Entity
public class Project extends JPAModel {
    
    @Future
    @Past("2020-01-01")
    public Date endDate;
    
    @Past
    @Future("1980-21-12")
    public Date startDate;
    
}

