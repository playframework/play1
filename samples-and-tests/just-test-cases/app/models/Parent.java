package models;

import play.*;
import play.db.jpa.*;
import javax.persistence.*;
import java.util.*;

@Entity
public class Parent extends GenericModel {

       @Id
       public String code;

       public String name;

       @ManyToMany
       public Set<Child> children;

}