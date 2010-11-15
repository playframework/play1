package models;

import play.*;
import play.db.jpa.*;
import play.data.validation.*;

import javax.persistence.*;
import java.util.*;

@Entity
public class B extends GenericModel {

    @Id
    public Long id;

    @MaxSize(10)
    public String name;

}
