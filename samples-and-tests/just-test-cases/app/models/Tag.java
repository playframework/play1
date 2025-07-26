package models;

import play.*;
import play.db.jpa.*;

import jakarta.persistence.*;
import java.util.*;

@Entity
public class Tag extends Model {
    
    public String name;
    
}

