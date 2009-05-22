package models;

import play.*;
import play.db.jpa.*;
import javax.persistence.*;
import java.util.*;

@Entity
public class User extends JPAModel {
    
    public String name;
    public Boolean b;
    public boolean c;
    public Integer i;
    public int j;
    public long l;
    public Long k;
    public Date birth;
    
}

