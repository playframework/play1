package models;

import play.*;
import play.db.jpa.*;
import javax.persistence.*;
import java.util.*;

@Entity
public class Company extends GenericModel {

    @Id
    @GeneratedValue(generator="system-uuid")
    @org.hibernate.annotations.GenericGenerator(name="system-uuid", strategy = "uuid")
    public String id;

    public String name;

    public String toString() {
        return name;
    }

}

