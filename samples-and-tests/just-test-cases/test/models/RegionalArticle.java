package models;

import javax.persistence.Entity;
import play.*;
import play.db.jpa.*;
import play.data.validation.*;

import javax.persistence.*;
import java.io.Serializable;
import java.util.*;

@Entity
public class RegionalArticle extends GenericModel implements Serializable {
    @Id
    public RegionalArticlePk pk;

    public String name;

    @Override
    public String toString() {
        return "RegionalArticle{" +
                "pk=" + pk +
                ", name='" + name + '\'' +
                '}';
    }
}