package models;

import play.*;
import play.db.jpa.*;

import jakarta.persistence.*;
import java.util.*;

@Entity
public class Face extends Model {
    
    public String name;
    
    @OneToOne(cascade = CascadeType.ALL)
    public Nose nose;
    
}

