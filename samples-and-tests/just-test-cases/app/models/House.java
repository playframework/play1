package models;

import play.*;
import play.db.jpa.*;
import javax.persistence.*;
import java.util.*;

@Entity
public class House extends GenericModel {
    
    @Id
    @Column(length=3)
    public String id;
    
    @OneToOne
    public Address address;
    
    @OneToMany
    @MapKey(name = "lang")
    public Map<String, SomeText> texts;
    
    @Transient    
    public Map<String, Integer> justMap;
    
    public int[] numbers;
    
    public String[] notSet;

    @Transient    
    public List<Integer> notSetList;    
    
    @Transient
    public List<Long> moreNumbers;
    
    @Transient
    public List<Integer> moreMoreNumbers;
    
    @Transient
    public List<String> moreTags;
    
    
    public String[] tags;
    
    @OneToMany
    public List<Dog> dogs;
    
    
}

