package models;

import play.*;
import play.db.jpa.*;
import play.data.validation.*;

import javax.persistence.*;
import java.util.*;

@Entity
public class A extends GenericModel {

    @Id
    public Long id;

    @ManyToOne(cascade=CascadeType.ALL)
    @Valid
    public B b;

}