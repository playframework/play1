package models;

import play.*;
import play.db.jpa.*;
import javax.persistence.*;
import java.util.*;

@Entity
public class House extends JPASupport {
    
    @Id
    @Column(length=3)
    public String id;
    
}

