package models;

import play.*;
import play.data.binding.NoBinding;
import play.db.jpa.*;

import javax.persistence.*;

import java.util.*;

@Entity
public class Child extends Model {

       public String name;
       
       @NoBinding("secure")
       @ManyToOne
       public Parent father;
       
       @NoBinding("secure")
       @ManyToOne
       public Parent mother;
       
       @NoBinding("secure")
       @ManyToOne
       public Person tutor;
       
       @NoBinding("Fixtures")
       public String test;
       
       public String toSimpleJSON(){
	   return "{\"name\":\"" + this.name  + "\"" + 
		   ", \"father\":\"" + (this.father != null? this.father.name: "null") + "\"" + 
		   ", \"mother\":\"" + (this.mother != null? this.mother.name: "null") + "\"" + 
		   ", \"tutor\":\"" + (this.tutor != null? this.tutor.userName: "null") + "\"" + 
		   "}";
       }

}