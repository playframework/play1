package models;

import play.*;
import play.db.jpa.*;
import javax.persistence.*;
import java.util.*;

public class Company {

    @Id
    @GeneratedValue(generator="system-uuid")
    @org.hibernate.annotations.GenericGenerator(name="system-uuid", strategy = "uuid")
    public String id;
    
    @Version
    public int version;
    
    public String b = "YOP";

    public String name;

    public String toString() {
        return name;
    }

}

