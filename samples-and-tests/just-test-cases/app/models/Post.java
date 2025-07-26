package models;

import play.*;
import play.db.jpa.*;

import jakarta.persistence.*;
import java.util.*;

@Entity
public class Post extends Model {
    
    public String name;
    
    @ManyToMany(cascade=CascadeType.ALL)
    public List<Tag> tags = new ArrayList();
    
}

