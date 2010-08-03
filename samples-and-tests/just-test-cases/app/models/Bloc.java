package models;

import play.*;
import play.db.jpa.*;

import javax.persistence.*;
import java.util.*;

@Entity
public class Bloc extends Model {
	
    public String name;

    @ElementCollection(fetch = FetchType.EAGER)
    @JoinTable(name = "Bloc_Criterias", joinColumns = @JoinColumn(name = "id"))
    @Column(name = "propertyValue", nullable = false)
    @org.hibernate.annotations.Cascade(value = org.hibernate.annotations.CascadeType.ALL)
    public Map<String, String> criterias = new HashMap<String, String>();
	
    public Date created;

}

